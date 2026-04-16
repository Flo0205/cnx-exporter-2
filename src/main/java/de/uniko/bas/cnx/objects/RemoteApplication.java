package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class RemoteApplication {
    private String id;
    private String title;
    private String content;
    private String published;   // Time
    private String updated;     // Time
    private String feedUrl;
    private String publishUrl;
}
