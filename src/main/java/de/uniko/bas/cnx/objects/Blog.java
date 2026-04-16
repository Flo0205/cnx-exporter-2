package de.uniko.bas.cnx.objects;

import lombok.Data;

import java.util.List;

@Data
public class Blog {
    private String id;
    private String uuid;
    private String link;
    private String title;
    private String updated;
    private String edited;
    private String published;
    private int recommendationCount;
    private int commentCount;
    private int hitCount;

    private String authorName;
    private String authorEmail;
    private String authorUuid;
    private boolean authorState;
    private boolean authorIsExternal;

    private String summary;
    private String content;
    private List<BlogComment> comments;
}
