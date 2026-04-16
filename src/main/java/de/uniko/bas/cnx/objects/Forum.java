package de.uniko.bas.cnx.objects;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Forum {
    private String uuid;
    private String title;
    private String updated;
    private String published;
    private String authorUserId;   // snx:userid if present
    private List<Topic> topics = new ArrayList<>();
}
