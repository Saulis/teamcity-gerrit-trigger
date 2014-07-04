package org.saulis;

import com.jcraft.jsch.JSch;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class GerritTriggerService extends BuildTriggerService {

    @NotNull
    private final BuildCustomizerFactory buildCustomizerFactory;
    @NotNull
    private final PluginDescriptor pluginDescriptor;
    private final GerritClient gerritClient;
    private GerritPolledBuildTrigger triggerPolicy;

    public GerritTriggerService(@NotNull final BuildCustomizerFactory buildCustomizerFactory,
                                @NotNull final PluginDescriptor pluginDescriptor) {

        this.buildCustomizerFactory = buildCustomizerFactory;
        this.pluginDescriptor = pluginDescriptor;
        gerritClient = new GerritClient(new JSch());
    }

    @NotNull
    @Override
    public String getName() {
        return "gerritBuildTrigger";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Gerrit Build Trigger";
    }

    @NotNull
    @Override
    public String describeTrigger(@NotNull BuildTriggerDescriptor buildTriggerDescriptor) {
        Map<String,String> parameters = buildTriggerDescriptor.getParameters();
        StringBuilder description = new StringBuilder();

        description.append("Listening");

        String project = getTrimmedParameter(parameters, Parameters.PROJECT);
        if(project.length() > 0) {
            description.append(" to ");
            description.append(project);

            String branch = getTrimmedParameter(parameters, Parameters.BRANCH);
            if(branch.length() > 0) {
                description.append("/" + branch);
            }
        }

        description.append(" on " + parameters.get(Parameters.HOST));

        return description.toString();
    }

    private String getTrimmedParameter(Map<String, String> parameters, String key) {

        //No sure how TeamCity inputs empty string parameters in UI, so playing it safe for nulls.
        if(parameters.containsKey(key)) {
            String value = parameters.get(key);

            if(value != null) {
                return value.trim();
            }
        }

        return "";
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return pluginDescriptor.getPluginResourcesPath("editGerritTrigger.jsp");
    }

    @NotNull
    @Override
    public BuildTriggeringPolicy getBuildTriggeringPolicy() {
        if(triggerPolicy == null) {
            triggerPolicy = new GerritPolledBuildTrigger(gerritClient, buildCustomizerFactory);
        }

        return triggerPolicy;
    }

    @Override
    public boolean isMultipleTriggersPerBuildTypeAllowed() {
        return true;
    }
}
