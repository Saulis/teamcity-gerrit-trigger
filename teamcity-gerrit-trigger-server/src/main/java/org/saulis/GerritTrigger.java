package org.saulis;

import jetbrains.buildServer.buildTriggers.*;
import jetbrains.buildServer.log.Loggers;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class GerritTrigger extends BuildTriggerService {
    private static final Logger LOG = Logger.getLogger(Loggers.VCS_CATEGORY + GerritTrigger.class);

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
        return "Listening to events!";
    }


    @NotNull
    @Override
    public BuildTriggeringPolicy getBuildTriggeringPolicy() {
        LOG.info("Creating GERRIT trigger policy...");
        return new PolledBuildTrigger() {
            @Override
            public void triggerBuild(@NotNull PolledTriggerContext polledTriggerContext) throws BuildTriggerException {

                LOG.info("HELLO GERRIT!");


            }
        };
    }
}
