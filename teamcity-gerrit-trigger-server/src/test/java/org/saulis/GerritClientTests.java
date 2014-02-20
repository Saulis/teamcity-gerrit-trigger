package org.saulis;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Created by Saulis on 19/02/14.
 */
public class GerritClientTests {

    private GerritClient client;
    private JSch jsch;
    private PolledTriggerContext triggerContext;
    private Session session;
    private ChannelExec channel;
    private HashMap<String,String> parameters;
    private BuildTriggerDescriptor triggerDescriptor;

    void mockDepedencies() {
        jsch = mock(JSch.class);
    }

    @Before
    public void setup() throws JSchException, IOException {
        mockDepedencies();

        client = new GerritClient(jsch);


        parameters = new HashMap<String, String>();
        session = mock(Session.class);
        channel = mock(ChannelExec.class);
        triggerContext = mock(PolledTriggerContext.class);
        triggerDescriptor = mock(BuildTriggerDescriptor.class);


        when(jsch.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
        when(session.openChannel("exec")).thenReturn(channel);

        when(channel.getErrStream()).thenReturn(new ByteArrayInputStream( "".getBytes() ));
        when(channel.getInputStream()).thenReturn(new ByteArrayInputStream("{\"project\":\"abraham\",\"branch\":\"bush\",\"id\":\"I56f19c5af7dc4ccfd2fa4c9098f06e77dbfa12fb\",\"number\":\"2448\",\"subject\":\"Add support for monkey facets (#43245)\",\"owner\":{\"name\":\"Don Johnson\",\"email\":\"vice@miami.gov.us\",\"username\":\"don\"},\"url\":\"https://dev.miami.com/review/2448\",\"commitMessage\":\"Add support for blah blah (#12645)\\n\\nSince this is quite the change, I\\u0027ve taken the opportunity to rewrite smaller\\nadjoining pieces to make more sense. Move methods from classes, and so on.\\nThese changes are, however, only on the code level, no other functionality will\\nbe introduced by this patch.\\n\\nChange-Id: I56f19c5af7dc4ccfd2fa4c9098f06e77dbfa12fb\\n\",\"createdOn\":1389255476,\"lastUpdated\":1392802081,\"sortKey\":\"002b3a9800000990\",\"open\":true,\"status\":\"NEW\",\"currentPatchSet\":{\"number\":\"7\",\"revision\":\"15b1316507acd69bc7398643ddfad68efd6ded67\",\"parents\":[\"5733fbda77f1dfdfdde57e596a79260d1e9eb549\"],\"ref\":\"refs/changes/48/2448/7\",\"uploader\":{\"name\":\"Don Johnson\",\"email\":\"vice@miami.gov.us\",\"username\":\"donson\"},\"createdOn\":1390482249,\"author\":{\"name\":\"Don Johnson\",\"email\":\"vice@miami.gov.us\",\"username\":\"donnnnss\"},\"isDraft\":false,\"approvals\":[{\"type\":\"Code-Review\",\"description\":\"Code-Review\",\"value\":\"-1\",\"grantedOn\":1392802081,\"by\":{\"name\":\"John Foobars\",\"email\":\"john@miami.gov.us\",\"username\":\"johnfoos\"}}],\"sizeInsertions\":490,\"sizeDeletions\":-109}}\n{\"type\":\"stats\",\"rowCount\":1,\"runTimeMilliseconds\":10}".getBytes()));


        when(triggerContext.getTriggerDescriptor()).thenReturn(triggerDescriptor);
        when(triggerDescriptor.getParameters()).thenReturn(parameters);
    }

    private void assertThatCommandContains(String expected) {
        verify(channel).setCommand(argThat(StringContains.containsString(expected)));
    }

    private void assertThatCommandDoesNotContain(String expected) {
        verify(channel).setCommand(argThat(IsNot.not(StringContains.containsString(expected))));
    }

    private void setProjectParameter(String project) {
        parameters.put(Parameters.PROJECT, project);
    }

    private void setBranchParameter(String branch) {
        parameters.put(Parameters.BRANCH, branch);
    }

    private void getNewPatchSets() {
        client.getNewPatchSets(triggerContext);
    }

    @Test
    public void commandHasRequiredParameters() {

        getNewPatchSets();

        assertThatCommandContains("--format=JSON");
        assertThatCommandContains("status:open");
        assertThatCommandContains("--current-patch-set");
        assertThatCommandContains("limit:10");
    }

    @Test
    public void commandHasProjectParameter() {
        setProjectParameter("foo");

        getNewPatchSets();

        assertThatCommandContains("project:foo");
    }

    @Test
    public void emptyParameterIsIgnored() {
        setProjectParameter("    ");

        getNewPatchSets();

        assertThatCommandDoesNotContain("project:");
    }

    @Test
    public void commandHasBranchParameter() {
        setBranchParameter("BaR");

        getNewPatchSets();

        assertThatCommandContains("branch:bar");
    }

    @Test
    public void nullParameterIsIgnored() {
        setBranchParameter(null);

        getNewPatchSets();

        assertThatCommandDoesNotContain("branch:");
    }


}
