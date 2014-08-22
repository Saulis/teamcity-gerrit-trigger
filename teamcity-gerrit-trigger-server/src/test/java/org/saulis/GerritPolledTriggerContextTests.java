package org.saulis;

import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GerritPolledTriggerContextTests {

    private GerritPolledTriggerContext sut;
    private PolledTriggerContext context;
    private HashMap<String, String> parameters;
    private CustomDataStorage customDataStorage;
    private HashMap<String, String> storedValues;

    @Before
    public void setup() {
        context = mock(PolledTriggerContext.class);
        sut = new GerritPolledTriggerContext(context);

        BuildTriggerDescriptor triggerDescriptor = mock(BuildTriggerDescriptor.class);
        when(context.getTriggerDescriptor()).thenReturn(triggerDescriptor);

        parameters = new HashMap<String, String>();
        when(triggerDescriptor.getParameters()).thenReturn(parameters);

        customDataStorage = mock(CustomDataStorage.class);
        when(context.getCustomDataStorage()).thenReturn(customDataStorage);

        storedValues = new HashMap<String, String>();
        when(customDataStorage.getValues()).thenReturn(storedValues);
    }

    @Test
    public void trimmedUserNameIsFetched() {
        parameters.put(Parameters.USERNAME, "username    ");

        assertThat(sut.getUsername(), is("username"));
    }

    @Test
    public void trimmedHostIsFetched() {
        parameters.put(Parameters.HOST, "host    ");

        assertThat(sut.getHost(), is("host"));
    }

    @Test
    public void trimmedPassphraseIsFetched() {
        parameters.put(Parameters.PASSPHRASE, "foo  ");

        assertThat(sut.getPassphrase(), is("foo"));
    }

    @Test
    public void hasPassphrase() {
        parameters.put(Parameters.PASSPHRASE, "foo");

        assertTrue(sut.hasPassphrase());
    }

    @Test
    public void missingPassphraseIsHandled() {
        parameters.put(Parameters.PASSPHRASE, null);

        assertFalse(sut.hasPassphrase());
    }

    @Test
    public void emptyPassphraseIsHandled() {
        parameters.put(Parameters.PASSPHRASE, "  ");

        assertFalse(sut.hasPassphrase());
    }

    @Test
    public void trimmedPrivateKeyIsFetched() {
        parameters.put(Parameters.KEYPATH, "foo  ");

        assertThat(sut.getCustomPrivateKey(), is("foo"));
    }

    @Test
    public void hasPrivateKey() {
        parameters.put(Parameters.KEYPATH, "foo");

        assertTrue(sut.hasCustomPrivateKey());
    }

    @Test
    public void missingPrivateKeyIsHandled() {
        parameters.put(Parameters.KEYPATH, null);

        assertFalse(sut.hasCustomPrivateKey());
    }

    @Test
    public void emptyPrivateKeyIsHandled() {
        parameters.put(Parameters.KEYPATH, "  ");

        assertFalse(sut.hasCustomPrivateKey());
    }

    @Test
    public void trimmedProjectIsFetched() {
        parameters.put(Parameters.PROJECT, "foo  ");

        assertThat(sut.getProjectParameter(), is("foo"));
    }

    @Test
    public void hasProject() {
        parameters.put(Parameters.PROJECT, "foo");

        assertTrue(sut.hasProjectParameter());
    }

    @Test
    public void missingProjectIsHandled() {
        parameters.put(Parameters.PROJECT, null);

        assertFalse(sut.hasProjectParameter());
    }

    @Test
    public void emptyProjectIsHandled() {
        parameters.put(Parameters.PROJECT, "  ");

        assertFalse(sut.hasProjectParameter());
    }

    @Test
    public void trimmedBranchIsFetched() {
        parameters.put(Parameters.BRANCH, "foo  ");

        assertThat(sut.getBranchParameter(), is("foo"));
    }

    @Test
    public void hasBranchParameter() {
        parameters.put(Parameters.BRANCH, "foo");

        assertTrue(sut.hasBranchParameter());
    }

    @Test
    public void missingBranchIsHandled() {
        parameters.put(Parameters.BRANCH, null);

        assertFalse(sut.hasBranchParameter());
    }

    @Test
    public void emptyBranchIsHandled() {
        parameters.put(Parameters.BRANCH, "  ");

        assertFalse(sut.hasBranchParameter());
    }

    @Test
    public void currentTimeIsSetToTimestamp() {
        when(customDataStorage.getValues()).thenReturn(null);

        sut.updateTimestampIfNewer(new Date());

        assertThat(storedValues.get("timestamp"), IsNot.not("1390482249000"));
    }
}