package de.uniko.bas.cnx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.uniko.bas.cnx.objects.Community;
import de.uniko.bas.cnx.objects.CommunityBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Exporter {
    private final ObjectMapper mapper;

    public Exporter() {
        this.mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT);

        // avoids escaping slashes etc; optional
        this.mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }

    public void exportCommunity(Community c) throws IOException {
        Path out = Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c));
        writeToFile(c, out);
        if (c.getSubCommunities().isEmpty()) return;
        for (Community sub : c.getSubCommunities()) {
            exportCommunity(sub);
        }
    }

    public void exportCommunitySplit(Community c) throws IOException {
        // Bookmarks
        writeToFile(c.getBookmarks(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "bookmarks")));
        // Blogs
        writeToFile(c.getBlogs(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "blogs")));
        // Community-Data
        writeToFile(new CommunityBase(c),  Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "community")));
        // Events
        writeToFile(c.getEvents(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "events")));
        // Files
        writeToFile(c.getFiles(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "files")));
        // Forums
        writeToFile(c.getForums(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "forums")));
        // Members
        writeToFile(c.getMembers(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "members")));
        // RemoteApplications
        writeToFile(c.getRemoteApplications(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "remoteApplications")));
        // Widgets
        writeToFile(c.getWidgets(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "widgets")));
        // Wikis
        writeToFile(c.getWikis(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "wikis")));
        // SubCommunities
        writeToFile(c.getSubCommunities().stream().map(Community::getUuid).toList(), Path.of(Config.EXPORTDIR, c.getUuid(), fileName(c, "subCommunities")));


        for (Community sub : c.getSubCommunities()) {
            exportCommunitySplit(sub);
        }
    }

    private void writeToFile(Object obj, Path outFile) throws IOException {
        Files.createDirectories(outFile.getParent());
        mapper.writeValue(outFile.toFile(), obj);
    }

    private String fileName(Community c, String addition) {
        return "community-" + c.getUuid() + "-" + addition + ".json";
    }

    private String fileName(Community c) {
        return "community-" + c.getUuid() + ".json";
    }
}
