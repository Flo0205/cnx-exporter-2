package de.uniko.bas.cnx.objects;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WikiPage {

    // Identifiers
    private String uuid;
    private String label;

    // Content
    private String title;
    private String summary;
    private String html;

    // Timestamps
    private String published;
    private String updated;
    private String created;
    private String modified;

    // Visibility / versioning
    private String visibility;
    private String versionUuid;
    private String versionLabel;

    // Users
    private String authorUserId;
    private String modifierUserId;

    private String parentUuid;
    private List<WikiPage> children = new ArrayList<>();
    private List<WikiPageComment> comments = new ArrayList<>();
    private List<File> attachments = new ArrayList<>();
}
