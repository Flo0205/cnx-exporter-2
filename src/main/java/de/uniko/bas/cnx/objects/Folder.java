package de.uniko.bas.cnx.objects;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class Folder {
    private String uuid;
    private String label;
    private String filesFeedUrl;
    private Path path;
    private List<Folder> folders = new ArrayList<>();
    private List<File> files = new ArrayList<>();
}
