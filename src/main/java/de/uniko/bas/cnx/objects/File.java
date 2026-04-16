package de.uniko.bas.cnx.objects;

import lombok.Data;

import java.nio.file.Path;

@Data
public class File {
    private String uuid;
    private String added;       // Time
    private String created;     // Time
    private String published;   // Time
    private String updated;     // Time
    private String addedBy;     // userid
    private String modifiedBy;  // userid
    private String label;       // userid
    private String libraryId;
    private String libraryType;
    private String versionUuid;
    private String versionLabel;
    private boolean propagation;
    private boolean restrictedVisibility;
    private boolean isExternal;
    private Path localPath;   // The path where it is saved locally
    private String folderUuid;
    private String downloadUrl;
}
