package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class Event {
    private String uuid;
    private String title;
    private String published;   //Time
    private String updated;     //Time
    private String author;      //userid
    private String location;
    private boolean allDay;
    private String startDate;   //Time
    private String endDate;     //Time
    private String untilDate;   //Time
    private String byDay;
    private String frequency;
    private String interval;
}
