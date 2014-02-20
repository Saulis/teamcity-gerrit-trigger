package org.saulis;

/**
 * Created by Saulis on 19/02/14.
 */
class GerritPatchSet {
    private final String project;
    private final String branch;
    private final String ref;

    public GerritPatchSet(String project, String branch, String ref) {

        this.project = project;
        this.branch = branch;
        this.ref = ref;
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
}
