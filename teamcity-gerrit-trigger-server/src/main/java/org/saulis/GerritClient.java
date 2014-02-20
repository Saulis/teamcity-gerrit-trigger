package org.saulis;

import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by Saulis on 19/02/14.
 */
public class GerritClient {

    private final JSch jsch;
    private List<GerritPatchSet> events;

    public GerritClient(JSch jsch) {
        this.jsch = jsch;

        events = new ArrayList<GerritPatchSet>();
    }

    public List<GerritPatchSet> getNewPatchSets(PolledTriggerContext triggerContext) {
        StringBuilder command = createCommand(triggerContext);

        try {
            runCommand(command.toString());
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    private StringBuilder createCommand(PolledTriggerContext triggerContext) {
        StringBuilder command = new StringBuilder();
        command.append("gerrit query --format=JSON status:open");

        if(hasProjectParameter(triggerContext)) {
            command.append(" project:" + getProjectParameter(triggerContext));
        }

        if(hasBranchParameter(triggerContext)) {
            command.append(" branch:" + getBranchParameter(triggerContext));
        }

        // Optimizing the query.
        // Assuming that no more than <limit> new patch sets are created during a single poll interval.
        // Adjust if needed.
        command.append(" limit:10");
        command.append(" --current-patch-set ");

        return command;
    }

    private boolean hasProjectParameter(PolledTriggerContext triggerContext) {
        return getProjectParameter(triggerContext).length() > 0;
    }

    private boolean hasBranchParameter(PolledTriggerContext triggerContext) {
        return getBranchParameter(triggerContext).length() > 0;
    }

    private String getProjectParameter(PolledTriggerContext triggerContext) {
        return getTrimmedParameter(triggerContext, Parameters.PROJECT);
    }

    private String getBranchParameter(PolledTriggerContext triggerContext) {
        return getTrimmedParameter(triggerContext, Parameters.BRANCH);
    }

    private String getTrimmedParameter(PolledTriggerContext triggerContext, String key) {
        Map<String, String> parameters = getParameters(triggerContext);

        if(parameters.containsKey(key)) {
            String projectParameter = parameters.get(key);

            if(projectParameter != null) {
                return projectParameter.trim().toLowerCase();
            }
        }

        return "";
    }

    private Map<String, String> getParameters(PolledTriggerContext triggerContext) {
        BuildTriggerDescriptor triggerDescriptor = triggerContext.getTriggerDescriptor();

        return triggerDescriptor.getParameters();
    }

    private void runCommand(@NotNull String command) throws JSchException, IOException {
        ChannelExec channel = null;
        Session session = null;
        try {
            String home = System.getProperty("user.home");
            home = home == null ? new File(".").getAbsolutePath() : new File(home).getAbsolutePath();
            jsch.addIdentity(new File(new File(home, ".ssh"), "id_rsa").getAbsolutePath());
            session = jsch.getSession("Saulis", "foo.bar", 29418);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));

            JsonStreamParser parser = new JsonStreamParser(new InputStreamReader(channel.getInputStream()));

            channel.connect();

            events.clear();

            while(parser.hasNext()) {
                JsonObject row = parser.next().getAsJsonObject();

                if(isStatsRow(row)) {
                    break;
                }

                String project = row.get("project").getAsString();
                String branch = row.get("branch").getAsString();
                JsonObject currentPatchSet = row.get("currentPatchSet").getAsJsonObject();
                String ref = currentPatchSet.get("ref").getAsString();

                events.add(new GerritPatchSet(project, branch, ref));
            }

            String line;
            StringBuilder details = new StringBuilder();
            while ((line = stderr.readLine()) != null) {
                details.append(line).append("\n");
            }
            if (details.length() > 0)
                throw new IOException(details.toString());
        } finally {
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }

    private boolean isStatsRow(JsonObject ticket) {
        return ticket.has("rowCount");
    }

}
