package org.saulis;

import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildCustomizer;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.serverSide.SBuildType;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class GerritPolledBuildTrigger extends PolledBuildTrigger {
    private static final Logger LOG = Logger.getLogger(Loggers.VCS_CATEGORY + GerritPolledBuildTrigger.class);
    private final GerritClient gerritClient;
    private final BuildCustomizerFactory buildCustomizerFactory;

    public GerritPolledBuildTrigger(GerritClient gerritClient, BuildCustomizerFactory buildCustomizerFactory) {
        this.gerritClient = gerritClient;
        this.buildCustomizerFactory = buildCustomizerFactory;
    }

    @Override
    public void triggerBuild(@NotNull PolledTriggerContext polledTriggerContext) throws BuildTriggerException {
        try {
            List<GerritPatchSet> newPatchSets = gerritClient.getNewPatchSets(new GerritPolledTriggerContext(polledTriggerContext));

            LOG.debug(String.format("GERRIT: Going to trigger %s new build(s).", newPatchSets.size()));

            for(GerritPatchSet p : newPatchSets) {
                SBuildType buildType = polledTriggerContext.getBuildType();
                BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, null);
                buildCustomizer.setDesiredBranchName(p.getRef().substring(5));

                buildCustomizer.createPromotion().addToQueue("Gerrit");
            }
        } catch (Exception e) {
            LOG.error("GERRIT:", e);
        }
    }
}
