package org.saulis;

import com.jcraft.jsch.JSch;
import jetbrains.buildServer.buildTriggers.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildCustomizer;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.serverSide.SBuildType;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class GerritTrigger extends BuildTriggerService {
    private static final Logger LOG = Logger.getLogger(Loggers.VCS_CATEGORY + GerritTrigger.class);
    @NotNull
    private final BuildCustomizerFactory buildCustomizerFactory;

    public GerritTrigger(@NotNull final BuildCustomizerFactory buildCustomizerFactory) {

        this.buildCustomizerFactory = buildCustomizerFactory;
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
        return "Listening to events!";
    }


    @NotNull
    @Override
    public BuildTriggeringPolicy getBuildTriggeringPolicy() {
        LOG.info("Creating GERRIT trigger policy...");
        return new PolledBuildTrigger() {
            @Override
            public void triggerBuild(@NotNull PolledTriggerContext polledTriggerContext) throws BuildTriggerException {
                GerritClient client = new GerritClient(new JSch());
                client.getNewPatchSets(polledTriggerContext);

        try {

            SBuildType buildType = polledTriggerContext.getBuildType();
            BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, null);
            buildCustomizer.setDesiredBranchName("changes/10/510/1");

//            buildCustomizer.createPromotion().addToQueue("Gerrit");


//            for(SVcsModification change : buildType.getPendingChanges()) {
//                LOG.info("GERRIT: " + change.toString());
//            }

            //LOG.info("nice GERRIT!");

        } catch (Exception e) {
            LOG.error("GERRIT:", e);
        }
            }
    };
    }
}
