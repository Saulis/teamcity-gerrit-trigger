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
    private final PolledTriggerContext triggerContext;

    public GerritClient(JSch jsch, PolledTriggerContext triggerContext) {
        this.jsch = jsch;
        this.triggerContext = triggerContext;
    }

    public List<GerritPatchSet> getNewPatchSets() {
        ChannelExec channel = null;
        Session session = null;

        try {
            session = openSession();
            channel = openChannel(session);

            //TODO: check if works after connecting
            //BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));

            return readGerritPatchSets(channel);
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

    private Date getPreviousQueryTimestamp() {
        String timestamp = "timestamp";
        Map<String, String> values = triggerContext.getCustomDataStorage().getValues();

        long currentTimestamp = new Date().getTime();
        Date previousTimestamp;

        if(!values.containsKey(timestamp)) {
            previousTimestamp = new Date(currentTimestamp);
        } else {
            previousTimestamp = new Date(Long.parseLong(values.get(timestamp)));
        }

        values.put(timestamp, String.valueOf(currentTimestamp));

        return previousTimestamp;
    }

    private String createCommand() {
        StringBuilder command = new StringBuilder();
        command.append("gerrit query --format=JSON status:open");

        if(hasProjectParameter()) {
            command.append(" project:" + getProjectParameter());
        }

        if(hasBranchParameter()) {
            command.append(" branch:" + getBranchParameter());
        }

        // Optimizing the query.
        // Assuming that no more than <limit> new patch sets are created during a single poll interval.
        // Adjust if needed.
        command.append(" limit:10");
        command.append(" --current-patch-set ");

        return command.toString();
    }

    private boolean hasProjectParameter() {
        return getProjectParameter().length() > 0;
    }

    private boolean hasBranchParameter() {
        return getBranchParameter().length() > 0;
    }

    private String getProjectParameter() {
        return getTrimmedParameter(Parameters.PROJECT);
    }

    private String getBranchParameter() {
        return getTrimmedParameter(Parameters.BRANCH);
    }

    private String getTrimmedParameter(String key) {
        Map<String, String> parameters = getParameters();

        if(parameters.containsKey(key)) {
            String projectParameter = parameters.get(key);

            if(projectParameter != null) {
                return projectParameter.trim().toLowerCase();
            }
        }

        return "";
    }

    private Map<String, String> getParameters() {
        BuildTriggerDescriptor triggerDescriptor = triggerContext.getTriggerDescriptor();

        return triggerDescriptor.getParameters();
    }

    private Session openSession() throws JSchException {
        String username = getTrimmedParameter(Parameters.USERNAME);
        String host = getTrimmedParameter(Parameters.HOST);

        return openSession(username, host);
    }

    private List<GerritPatchSet> readGerritPatchSets(ChannelExec channel) throws IOException {
        JsonStreamParser parser = new JsonStreamParser(new InputStreamReader(channel.getInputStream()));
        Date timestamp = getPreviousQueryTimestamp();

        List<GerritPatchSet> patchSets = new ArrayList<GerritPatchSet>();

        while(parser.hasNext()) {
            JsonObject row = parser.next().getAsJsonObject();

            if(isStatsRow(row)) {
                break;
            }

            GerritPatchSet patchSet = parsePatchSet(row);

            if(patchSet.getCreatedOn().compareTo(timestamp) >= 0) {
                patchSets.add(patchSet);
            }
        }

        return patchSets;
    }

    private GerritPatchSet parsePatchSet(JsonObject row) {
        String project = row.get("project").getAsString();
        String branch = row.get("branch").getAsString();
        JsonObject currentPatchSet = row.get("currentPatchSet").getAsJsonObject();
        String ref = currentPatchSet.get("ref").getAsString();
        long createdOn = currentPatchSet.get("createdOn").getAsLong() * 1000L;

        return new GerritPatchSet(project, branch, ref, createdOn);
    }

    private ChannelExec openChannel(Session session) throws JSchException {
        ChannelExec channel;
        channel = (ChannelExec)session.openChannel("exec");

        String command = createCommand();
        channel.setCommand(command);

        channel.connect();

        return channel;
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

    private boolean isStatsRow(JsonObject ticket) {
        return ticket.has("rowCount");
    }

}
