package org.saulis;


import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GerritTriggerServiceTests {

    private GerritTriggerService service;
    private BuildTriggerDescriptor buildTriggerDescriptor;
    private HashMap<String,String>         parameters = new HashMap<String, String>();


    @Before
    public void setup() {
        BuildCustomizerFactory buildCustomizerFactory = mock(BuildCustomizerFactory.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        buildTriggerDescriptor = mockBuildTriggerDescriptor();


        service = new GerritTriggerService(buildCustomizerFactory, pluginDescriptor);
    }

    private BuildTriggerDescriptor mockBuildTriggerDescriptor() {
        BuildTriggerDescriptor descriptor = mock(BuildTriggerDescriptor.class);

        when(descriptor.getParameters()).thenReturn(parameters);

        return descriptor;
    }

    private String describeTrigger() {
        return service.describeTrigger(buildTriggerDescriptor);
    }

    @Test
    public void sameTriggerPolicyInstanceIsReturned() {
        BuildTriggeringPolicy actual = service.getBuildTriggeringPolicy();
        BuildTriggeringPolicy expected = service.getBuildTriggeringPolicy();

        assertThat(actual, is(expected));
    }

    @Test
    public void descriptionWithoutFiltersIsReturned() {
        parameters.put(Parameters.HOST, "gerrit.foo.bar");

        String description = describeTrigger();

        assertThat(description, is("Listening on gerrit.foo.bar"));
    }

    @Test
    public void descriptionWithProjectFilterIsReturned() {
        parameters.put(Parameters.HOST, "gerrit.foo.bar");
        parameters.put(Parameters.PROJECT, "fooject");

        String description = describeTrigger();

        assertThat(description, is("Listening to fooject on gerrit.foo.bar"));
    }

    @Test
    public void descriptionWithProjectAndBranchFiltersIsReturned() {
        parameters.put(Parameters.HOST, "gerrit.foo.bar");
        parameters.put(Parameters.PROJECT, "fooject");
        parameters.put(Parameters.BRANCH, "barnch");

        String description = describeTrigger();

        assertThat(description, is("Listening to fooject/barnch on gerrit.foo.bar"));
    }
}
