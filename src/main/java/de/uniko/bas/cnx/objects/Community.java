package de.uniko.bas.cnx.objects;

import lombok.Data;

import java.util.List;

@Data
public class Community {
    private String uuid;
    private String name;
    private String type;
    private String content;
    private String summary;
    private List<String> tag;
    private String authorUuid;
    private String logo;
    private String parentId;
    private String wikiInstanceId;
    private boolean listWhenRestricted;
    private int memberCount;
    private boolean preModeration;
    private boolean postModeration;
    private String published;   // Time 2024-12-05T09:36:21.621Z
    private String updated;     // Time 2025-04-10T12:59:26.054Z
    private String theme;
    private List<Bookmark> bookmarks;
    private List<Event> events;
    private List<Member> members;
    private List<RemoteApplication>  remoteApplications;
    private List<Community> subCommunities;
    private List<Widget> widgets;
    private Folder files;
    private List<Forum> forums;
    private List<Wiki> wikis;
    private List<Blog> blogs;
}
