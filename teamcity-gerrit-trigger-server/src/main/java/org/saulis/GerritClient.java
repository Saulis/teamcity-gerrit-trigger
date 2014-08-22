package org.saulis;

import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class GerritClient {

    private static final Logger LOG = Logger.getLogger(Loggers.VCS_CATEGORY + GerritClient.class);
    private final JSch jsch;

    public GerritClient(JSch jsch) {
        this.jsch = jsch;
    }

    public List<GerritPatchSet> getNewPatchSets(GerritPolledTriggerContext context) {
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

    private Session openSession(GerritPolledTriggerContext context) throws JSchException {

        if(context.hasPassphrase()) {
            jsch.addIdentity(context.getPrivateKey(), context.getPassphrase());
        } else {
            jsch.addIdentity(context.getPrivateKey());
        }

        Session session = jsch.getSession(context.getUsername(), context.getHost(), 29418);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        return session;
    }

    private ChannelExec openChannel(GerritPolledTriggerContext context, Session session) throws JSchException {
        ChannelExec channel;
        channel = (ChannelExec)session.openChannel("exec");

        String command = createCommand(context);
        LOG.debug("GERRIT: " + command);
        channel.setCommand(command);

        channel.connect();

        return channel;
    }

    private String createCommand(GerritPolledTriggerContext context) {
        StringBuilder command = new StringBuilder();
        command.append("gerrit query --format=JSON status:open");

        if(context.hasProjectParameter()) {
            command.append(" project:" + context.getProjectParameter());
        }

        if(context.hasBranchParameter()) {
            command.append(" branch:" + context.getBranchParameter());
        }

        // Optimizing the query.
        // Assuming that no more than <limit> new patch sets are created during a single poll interval.
        // Adjust if needed.
        command.append(" limit:10");
        command.append(" --current-patch-set ");

        return command.toString();
    }


    private List<GerritPatchSet> readGerritPatchSets(GerritPolledTriggerContext context, ChannelExec channel) throws IOException {
        JsonStreamParser parser = new JsonStreamParser(new InputStreamReader(channel.getInputStream()));

        List<GerritPatchSet> patchSets = new ArrayList<GerritPatchSet>();

        if(context.hasTimestamp()) {
            Date timestamp = context.getTimestamp();

            while(parser.hasNext()) {
               JsonObject row = parser.next().getAsJsonObject();

              if(isStatsRow(row)) {
                break;
              }

              GerritPatchSet patchSet = parsePatchSet(row);

              if(patchSet.getCreatedOn().after(timestamp)) {
                patchSets.add(patchSet);
                context.updateTimestampIfNewer(patchSet.getCreatedOn());
              }
            }
        }
        else {
            context.updateTimestampIfNewer(new Date());
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

    private boolean isStatsRow(JsonObject ticket) {
        return ticket.has("rowCount");
    }

}
