package org.saulis;


import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class GerritTriggerServiceTests {

    private GerritTriggerService service;

    @Before
    public void setup() {
        BuildCustomizerFactory buildCustomizerFactory = mock(BuildCustomizerFactory.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);

        service = new GerritTriggerService(buildCustomizerFactory, pluginDescriptor);
    }

    @Test
    public void sameTriggerPolicyInstanceIsReturned() {
        BuildTriggeringPolicy actual = service.getBuildTriggeringPolicy();
        BuildTriggeringPolicy expected = service.getBuildTriggeringPolicy();

        assertThat(actual, is(expected));
    }
}
