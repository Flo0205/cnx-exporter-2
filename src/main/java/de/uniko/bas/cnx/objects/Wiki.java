package de.uniko.bas.cnx.objects;

import lombok.Data;

import java.util.List;

@Data
public class Wiki {
    private String uuid;
    private String label;
    private String externalInstanceId;

    private String title;
    private String summary;

    private String published;
    private String updated;
    private String created;
    private String modified;

    private String communityUuid;
    private String visibilityComputed;
    private String themeName;
    private boolean isExternal;

    private String authorUserId;
    private String modifierUserId;

    private List<WikiPage> wikiPages;
}
