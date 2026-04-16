package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class BlogComment {
    private String id;
    private String uuid;
    private String title;
    private String updated;
    private String edited;
    private String published;

    private String authorName;
    private String authorEmail;
    private String authorUuid;
    private boolean authorState;
    private boolean authorIsExternal;

    private String contributorName;
    private String contributorEmail;
    private String contributorUuid;
    private boolean contributorState;
    private boolean contributorIsExternal;

    private String content;
    private int recommendationCount;
}
