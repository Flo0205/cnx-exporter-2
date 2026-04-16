package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class Bookmark {
    private String title;
    private String summary;
    private String content;
    private String published; // Time
    private String updated; // Time
    private String author; // userid
    private String contributor; // userid
    private String link;
}
