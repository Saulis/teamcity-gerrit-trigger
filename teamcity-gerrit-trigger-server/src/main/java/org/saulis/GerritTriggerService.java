package org.saulis;

import com.jcraft.jsch.JSch;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        return "Listening to events!"; //TODO: fix description
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
}
