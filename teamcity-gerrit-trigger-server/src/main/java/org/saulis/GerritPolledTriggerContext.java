package org.saulis;

import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;

import java.io.File;
import java.util.Date;
import java.util.Map;

public class GerritPolledTriggerContext {

    private final PolledTriggerContext context;
    private final String TIMESTAMP_KEY = "timestamp";

    public GerritPolledTriggerContext(PolledTriggerContext polledTriggerContext) {

        this.context = polledTriggerContext;
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

    public String getUsername() {
        return getTrimmedParameter(context, Parameters.USERNAME);
    }

    public String getHost() {
        return getTrimmedParameter(context, Parameters.HOST);
    }

    public String getPassphrase() {
        return getTrimmedParameter(context, Parameters.PASSPHRASE);
    }

    public boolean hasPassphrase() {
        return !getPassphrase().isEmpty();
    }

    public String getCustomPrivateKey() {
        return getTrimmedParameter(context, Parameters.KEYPATH);
    }

    public boolean hasCustomPrivateKey() {
        return !getCustomPrivateKey().isEmpty();
    }

    public String getPrivateKey() {
        if(hasCustomPrivateKey()) {
            return new File(getCustomPrivateKey()).getAbsolutePath();
        } else {
            String home = System.getProperty("user.home");
            home = home == null ? new File(".").getAbsolutePath() : new File(home).getAbsolutePath();
            return new File(new File(home, ".ssh"), "id_rsa").getAbsolutePath();
        }
    }

    public boolean hasProjectParameter() {
        return getProjectParameter().length() > 0;
    }

    public String getProjectParameter() {
        return getTrimmedParameter(context, Parameters.PROJECT);
    }

    public boolean hasBranchParameter() {
        return getBranchParameter().length() > 0;
    }

    public String getBranchParameter() {
        return getTrimmedParameter(context, Parameters.BRANCH);
    }


    public void updateTimestampIfNewer(Date timestamp) {
        if(hasTimestamp()) {
            Date previousTimestamp = getTimestamp();

            if(timestamp.after(previousTimestamp)) {
                setTimestamp(timestamp);
            }
        } else {
            setTimestamp(timestamp);
        }
    }

    private void setTimestamp(Date timestamp) {
        context.getCustomDataStorage().putValue(TIMESTAMP_KEY, String.valueOf(timestamp.getTime()));
    }

    public boolean hasTimestamp() {
        if(hasStoredValues()) {
            Map<String, String> storedValues = getStoredValues();

            return storedValues.containsKey(TIMESTAMP_KEY);
        }

        return false;
    }

    private boolean hasStoredValues() {
        return getStoredValues() != null;
    }

    private Map<String, String> getStoredValues() {
        return context.getCustomDataStorage().getValues();
    }

    public Date getTimestamp() {
        Map<String, String> values = getStoredValues();

        return new Date(Long.parseLong(values.get(TIMESTAMP_KEY)));
    }
}
