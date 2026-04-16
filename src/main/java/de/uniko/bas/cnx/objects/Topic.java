package de.uniko.bas.cnx.objects;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Topic {
    private String uuid;
    private String title;
    private String updated;
    private String published;
    private String authorUserId;
    private String content;        // plain text or html (depends on API)
    private List<Reply> replies = new ArrayList<>();
    private List<File> attachments;
}
