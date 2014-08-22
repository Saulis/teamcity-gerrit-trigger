package org.saulis;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class GerritClientTests {

    private GerritClient client;
    private JSch jsch;
    private GerritPolledTriggerContext context;
    private Session session;
    private ChannelExec channel;
    private int sshPort = 29418;

    void mockDepedencies() {
        jsch = mock(JSch.class);
    }

    @Before
    public void setup() throws JSchException, IOException {
        mockDepedencies();

        client = new GerritClient(jsch);

        context = mock(GerritPolledTriggerContext.class);
        session = mock(Session.class);
        channel = mock(ChannelExec.class);

        when(jsch.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
        when(session.openChannel("exec")).thenReturn(channel);

        when(channel.getErrStream()).thenReturn(new ByteArrayInputStream( "".getBytes() ));
        when(channel.getInputStream()).thenReturn(new ByteArrayInputStream("{\"project\":\"abraham\",\"branch\":\"bush\",\"id\":\"I56f19c5af7dc4ccfd2fa4c9098f06e77dbfa12fb\",\"number\":\"2448\",\"subject\":\"Add support for monkey facets (#43245)\",\"owner\":{\"name\":\"Don Johnson\",\"email\":\"vice@miami.gov.us\",\"username\":\"don\"},\"url\":\"https://dev.miami.com/review/2448\",\"commitMessage\":\"Add support for blah blah (#12645)\\n\\nSince this is quite the change, I\\u0027ve taken the opportunity to rewrite smaller\\nadjoining pieces to make more sense. Move methods from classes, and so on.\\nThese changes are, however, only on the code level, no other functionality will\\nbe introduced by this patch.\\n\\nChange-Id: I56f19c5af7dc4ccfd2fa4c9098f06e77dbfa12fb\\n\",\"createdOn\":1389255476,\"lastUpdated\":1392802081,\"sortKey\":\"002b3a9800000990\",\"open\":true,\"status\":\"NEW\",\"currentPatchSet\":{\"number\":\"7\",\"revision\":\"15b1316507acd69bc7398643ddfad68efd6ded67\",\"parents\":[\"5733fbda77f1dfdfdde57e596a79260d1e9eb549\"],\"ref\":\"refs/changes/48/2448/7\",\"uploader\":{\"name\":\"Don Johnson\",\"email\":\"vice@miami.gov.us\",\"username\":\"donson\"},\"createdOn\":1390482249,\"author\":{\"name\":\"Don Johnson\",\"email\":\"vice@miami.gov.us\",\"username\":\"donnnnss\"},\"isDraft\":false,\"approvals\":[{\"type\":\"Code-Review\",\"description\":\"Code-Review\",\"value\":\"-1\",\"grantedOn\":1392802081,\"by\":{\"name\":\"John Foobars\",\"email\":\"john@miami.gov.us\",\"username\":\"johnfoos\"}}],\"sizeInsertions\":490,\"sizeDeletions\":-109}}\n{\"type\":\"stats\",\"rowCount\":1,\"runTimeMilliseconds\":10}".getBytes()));

        Calendar calendar = new GregorianCalendar(2013, 0, 1);
        setTimeStamp(String.valueOf(calendar.getTime().getTime()));
    }

    private void assertThatCommandContains(String expected) {
        verify(channel).setCommand(argThat(StringContains.containsString(expected)));
    }

    private void assertThatCommandDoesNotContain(String expected) {
        verify(channel).setCommand(argThat(not(StringContains.containsString(expected))));
    }

    private void setProjectParameter(String project) {
        when(context.hasProjectParameter()).thenReturn(true);
        when(context.getProjectParameter()).thenReturn(project);
    }

    private void setBranchParameter(String branch) {
        when(context.hasBranchParameter()).thenReturn(true);
        when(context.getBranchParameter()).thenReturn(branch);
    }

    private List<GerritPatchSet> getNewPatchSets() {
        return client.getNewPatchSets(context);
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
        setEmptyProjectParameter();

        getNewPatchSets();

        assertThatCommandDoesNotContain("project:");
    }

    private void setEmptyProjectParameter() {
        when(context.hasProjectParameter()).thenReturn(false);
    }

    @Test
    public void commandHasBranchParameter() {
        setBranchParameter("bar");

        getNewPatchSets();

        assertThatCommandContains("branch:bar");
    }

    @Test
    public void emptyBranchIsIgnored() {
        setEmptyBranchParameter();

        getNewPatchSets();

        assertThatCommandDoesNotContain("branch:");
    }

    private void setEmptyBranchParameter() {
        when(context.hasBranchParameter()).thenReturn(false);
    }

    @Test
    public void usernameAndHostParametersAreUsed() throws JSchException {
        when(context.getUsername()).thenReturn("foobar");
        when(context.getHost()).thenReturn("host.com");

        getNewPatchSets();

        verify(jsch).getSession("foobar", "host.com", sshPort);
    }

    @Test
    public void patchSetIsParsed() {
        List<GerritPatchSet> patchSets = getNewPatchSets();

        GerritPatchSet patchset = patchSets.get(0);

        //Values from sample JSON ^
        assertThat(patchset.getProject(), is("abraham"));
        assertThat(patchset.getBranch(), is("bush"));
        assertThat(patchset.getRef(), is("refs/changes/48/2448/7"));
        assertThat(patchset.getCreatedOn(), is(new Date(1390482249000L)));
    }

    @Test
    public void patchSetsWithCurrentTimestampAreNotFetched() {
        setTimeStamp("1390482249000");

        List<GerritPatchSet> patchSets = getNewPatchSets();

        assertThat(patchSets.size(), is(0));
    }

    private void setTimeStamp(String value) {
        when(context.hasTimestamp()).thenReturn(true);
        when(context.getTimestamp()).thenReturn(new Date(Long.parseLong(value)));
    }

    @Test
    public void patchSetsOlderThanTimestampAreSkipped() {
        setTimeStamp("1390482249001");

        List<GerritPatchSet> patchSets = getNewPatchSets();

        assertThat(patchSets.size(), is(0));
    }

    @Test
    public void patchSetsNewerThanTimestampAreFetched() {
        setTimeStamp("1390482248999");

        List<GerritPatchSet> patchSets = getNewPatchSets();

        assertThat(patchSets.size(), is(1));
    }

    @Test
    public void timestampIsUpdated() {
        setTimeStamp("1390482249000");

        getNewPatchSets();

        verify(context).updateTimestampIfNewer(context.getTimestamp());
    }

}
