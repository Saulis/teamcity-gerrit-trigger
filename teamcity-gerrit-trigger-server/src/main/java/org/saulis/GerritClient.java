package org.saulis;

import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class GerritClient {

    private static final Logger LOG = Logger.getLogger(Loggers.VCS_CATEGORY + GerritClient.class);
    private final JSch jsch;
    private final String TIMESTAMP_KEY = "timestamp";

    public GerritClient(JSch jsch) {
        this.jsch = jsch;
    }

    public List<GerritPatchSet> getNewPatchSets(PolledTriggerContext context) {
        ChannelExec channel = null;
        Session session = null;

        try {
            session = openSession(context);
            channel = openChannel(context, session);

            return readGerritPatchSets(context, channel);
        }
        catch (Exception e) {
            LOG.error("Gerrit trigger failed while getting patch sets.", e);
        }
        finally {
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }

        return new ArrayList<GerritPatchSet>();
    }

    private Session openSession(PolledTriggerContext context) throws JSchException {
        String username = getTrimmedParameter(context, Parameters.USERNAME);
        String host = getTrimmedParameter(context, Parameters.HOST);

        return openSession(username, host);
    }

    private String getTrimmedParameter(PolledTriggerContext context, String key) {
        Map<String, String> parameters = getParameters(context);

        if(parameters.containsKey(key)) {
            String projectParameter = parameters.get(key);

            if(projectParameter != null) {
                return projectParameter.trim();
            }
        }

        return "";
    }

    private Map<String, String> getParameters(PolledTriggerContext context) {
        BuildTriggerDescriptor triggerDescriptor = context.getTriggerDescriptor();

        return triggerDescriptor.getParameters();
    }

    private Session openSession(String username, String host) throws JSchException {
        String home = System.getProperty("user.home");
        home = home == null ? new File(".").getAbsolutePath() : new File(home).getAbsolutePath();
        jsch.addIdentity(new File(new File(home, ".ssh"), "id_rsa").getAbsolutePath());
        Session session = jsch.getSession(username, host, 29418);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        return session;
    }

    private ChannelExec openChannel(PolledTriggerContext context, Session session) throws JSchException {
        ChannelExec channel;
        channel = (ChannelExec)session.openChannel("exec");

        String command = createCommand(context);
        LOG.debug("GERRIT: " + command);
        channel.setCommand(command);

        channel.connect();

        return channel;
    }

    private String createCommand(PolledTriggerContext context) {
        StringBuilder command = new StringBuilder();
        command.append("gerrit query --format=JSON status:open");

        if(hasProjectParameter(context)) {
            command.append(" project:" + getProjectParameter(context));
        }

        if(hasBranchParameter(context)) {
            command.append(" branch:" + getBranchParameter(context));
        }

        // Optimizing the query.
        // Assuming that no more than <limit> new patch sets are created during a single poll interval.
        // Adjust if needed.
        command.append(" limit:10");
        command.append(" --current-patch-set ");

        return command.toString();
    }

    private boolean hasProjectParameter(PolledTriggerContext context) {
        return getProjectParameter(context).length() > 0;
    }

    private String getProjectParameter(PolledTriggerContext context) {
        return getTrimmedParameter(context, Parameters.PROJECT);
    }

    private boolean hasBranchParameter(PolledTriggerContext context) {
        return getBranchParameter(context).length() > 0;
    }

    private String getBranchParameter(PolledTriggerContext context) {
        return getTrimmedParameter(context, Parameters.BRANCH);
    }

    private List<GerritPatchSet> readGerritPatchSets(PolledTriggerContext context, ChannelExec channel) throws IOException {
        JsonStreamParser parser = new JsonStreamParser(new InputStreamReader(channel.getInputStream()));

        List<GerritPatchSet> patchSets = new ArrayList<GerritPatchSet>();

        if(hasTimestamp(context)) {
            Date timestamp = getTimestamp(context);

            while(parser.hasNext()) {
               JsonObject row = parser.next().getAsJsonObject();

              if(isStatsRow(row)) {
                break;
              }

              GerritPatchSet patchSet = parsePatchSet(row);

              if(patchSet.getCreatedOn().after(timestamp)) {
                patchSets.add(patchSet);
                updateTimestampIfNewer(context, patchSet.getCreatedOn());
              }
            }
        }
        else {
            updateTimestampIfNewer(context, new Date());
        }

        return patchSets;
    }

    private void updateTimestampIfNewer(PolledTriggerContext context, Date timestamp) {
        if(hasTimestamp(context)) {
            Date previousTimestamp = getTimestamp(context);

            if(timestamp.after(previousTimestamp)) {
                setTimestamp(context, timestamp);
            }
        } else {
            setTimestamp(context, timestamp);
        }
    }

    private void setTimestamp(PolledTriggerContext context, Date timestamp) {
        context.getCustomDataStorage().putValue(TIMESTAMP_KEY, String.valueOf(timestamp.getTime()));
    }

    private boolean hasTimestamp(PolledTriggerContext context) {
        if(hasStoredValues(context)) {
            Map<String, String> storedValues = getStoredValues(context);

            return storedValues.containsKey(TIMESTAMP_KEY);
        }

        return false;
    }

    private boolean hasStoredValues(PolledTriggerContext context) {
        return getStoredValues(context) != null;
    }

    private Map<String, String> getStoredValues(PolledTriggerContext context) {
        return context.getCustomDataStorage().getValues();
    }

    private Date getTimestamp(PolledTriggerContext context) {
        Map<String, String> values = getStoredValues(context);

        return new Date(Long.parseLong(values.get(TIMESTAMP_KEY)));
    }

    private GerritPatchSet parsePatchSet(JsonObject row) {
        String project = row.get("project").getAsString();
        String branch = row.get("branch").getAsString();
        JsonObject currentPatchSet = row.get("currentPatchSet").getAsJsonObject();
        String ref = currentPatchSet.get("ref").getAsString();
        long createdOn = currentPatchSet.get("createdOn").getAsLong() * 1000L;

        return new GerritPatchSet(project, branch, ref, createdOn);
    }

    private boolean isStatsRow(JsonObject ticket) {
        return ticket.has("rowCount");
    }

}
