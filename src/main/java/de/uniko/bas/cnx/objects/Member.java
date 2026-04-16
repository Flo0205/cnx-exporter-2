package de.uniko.bas.cnx.objects;

import lombok.Data;

@Data
public class Member {
    private String userid;
    private String role;
    private String name;
    private String email;
    private boolean active;
    private boolean isExternal;
}
