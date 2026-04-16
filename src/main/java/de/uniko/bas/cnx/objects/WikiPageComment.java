package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class WikiPageComment {
    private String uuid;

    private String title;
    private String content;      // <content type="text">...</content>
    private String language;

    private String published;
    private String updated;
    private String created;
    private String modified;

    private String versionLabel;
    private boolean deleteWithRecord;

    private String authorName;
    private String authorUserId;
    private String modifierName;
    private String modifierUserId;

    // helpful context
    private String wikiUuid;
    private String pageUuid;
}
