package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class Widget {
    private String title;
    private String link;
    private String defId;
    private String instanceId;
    private boolean hidden;
    private String location;
    private String prevInstanceId;
}
