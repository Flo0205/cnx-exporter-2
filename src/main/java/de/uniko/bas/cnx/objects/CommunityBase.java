package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class CommunityBase {
    private String uuid;
    private String name;
    private String type;
    private String content;
    private String summary;
    private String authorUuid;
    private String parentId;
    private boolean listWhenRestricted;
    private int memberCount;
    private boolean preModeration;
    private boolean postModeration;
    private String published;
    private String updated;
    private String theme;

    public CommunityBase(Community c) {
        this.uuid = c.getUuid();
        this.name = c.getName();
        this.type = c.getType();
        this.content = c.getContent();
        this.summary = c.getSummary();
        this.authorUuid = c.getAuthorUuid();
        this.parentId = c.getParentId();
        this.listWhenRestricted = c.isListWhenRestricted();
        this.memberCount = c.getMemberCount();
        this.preModeration = c.isPreModeration();
        this.postModeration = c.isPostModeration();
        this.published = c.getPublished();
        this.updated = c.getUpdated();
        this.theme = c.getTheme();
    }
}
