package de.uniko.bas.cnx.objects;

import lombok.Data;

import java.util.List;

@Data
public class Reply {
    private String uuid;
    private String updated;
    private String published;
    private String authorUserId;
    private String replyToId;
    private String content;
    private int likeCount;
    private String recommendationsUrl;
    private List<File> attachments;
}
