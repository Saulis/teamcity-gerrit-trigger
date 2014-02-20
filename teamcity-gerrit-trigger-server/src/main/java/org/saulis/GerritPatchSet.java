package org.saulis;

import java.util.Date;

class GerritPatchSet {
    private final String project;
    private final String branch;
    private final String ref;
    private final Date createdOn;

    public GerritPatchSet(String project, String branch, String ref, long createdOn) {

        this.project = project;
        this.branch = branch;
        this.ref = ref;
        this.createdOn = new Date(createdOn);
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getRef() {
        return ref;
    }

    public Date getCreatedOn() {
        return createdOn;
    }
}
