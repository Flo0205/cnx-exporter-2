package de.uniko.bas.cnx.services;

import de.uniko.bas.cnx.Config;
import de.uniko.bas.cnx.LogUtil;
import de.uniko.bas.cnx.RestBroker;
import de.uniko.bas.cnx.XmlUtil;
import de.uniko.bas.cnx.objects.Community;
import de.uniko.bas.cnx.objects.File;
import de.uniko.bas.cnx.objects.Folder;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;

public class FileService {

    private final RestBroker broker = new RestBroker();

    public void getCommunityFiles(Community c) throws IOException, InterruptedException {
        c.setFiles(getCommunityTree(c));
    }

    public Folder getCommunityTree(Community c) throws IOException, InterruptedException {
        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Getting files for community: " + c.getUuid());

        try {
            Folder root = new Folder();
            root.setUuid(c.getUuid());
            root.setLabel("ROOT");
            root.setPath(Path.of(Config.EXPORTDIR, c.getUuid(), "files"));

            // --- 1) fetch top-level files (documents) ---
            String baseUrl = Config.URLS.get("files")
                    + Config.URLS.get("files_getCommunityFilesAndFolders");
            baseUrl = baseUrl.replace("${commUuid}", c.getUuid());

            String docsXml = broker.doGetAuth(baseUrl);
            if (docsXml != null && !docsXml.isBlank()) {
                XmlUtil xu = XmlUtil.parse(docsXml);
                parseFilesIntoFolder(xu, root);
            }

            // --- 2) fetch top-level folders (collections) ---
            String foldersUrl = baseUrl
                    + (baseUrl.contains("?") ? "&" : "?")
                    + "category=collection";

            String foldersXml = broker.doGetAuth(foldersUrl);
            if (foldersXml != null && !foldersXml.isBlank()) {
                XmlUtil xu = XmlUtil.parse(foldersXml);
                NodeList entries = xu.nodes("/atom:feed/atom:entry");

                for (int i = 0; i < entries.getLength(); i++) {
                    Node e = entries.item(i);
                    Folder folder = parseFolderEntry(xu, e, root.getPath());
                    root.getFolders().add(folder);
                    populateFolderRecursive(folder);
                }
            }

            return root;
        } catch (InterruptedException ex) {
            throw ex;
        }  catch (Exception e) {
            throw new IOException("Failed to build community file tree", e);
        }
    }

    public void downloadAllFiles(Community c) throws IOException, InterruptedException {
        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Downloading files");

        if (c.getFiles() == null) {
            // build tree if not present
            c.setFiles(getCommunityTree(c));
        }

        Folder root = c.getFiles();
        if (root == null) return;

        Path baseDir = Path.of(Config.EXPORTDIR, c.getUuid(), "files");
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IOException("Failed to create base download dir: " + baseDir, e);
        }

        // BFS/DFS through folders
        Deque<Folder> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Folder cur = stack.pop();

            // download files in this folder
            if (cur.getFiles() != null) {
                for (File f : cur.getFiles()) {
                    try {
                        downloadFile(f);
                    } catch (IOException ex) {
                        LogUtil.log(this.getClass(),
                                "Skip file after download error: " + f.getLabel() + " (" + f.getUuid() + ") - "
                                        + ex.getMessage());
                    }
                }
            }

            // enqueue children
            if (cur.getFolders() != null) {
                for (Folder child : cur.getFolders()) {
                    stack.push(child);
                }
            }
        }
    }

    private void downloadFile(File file) throws IOException {
        if (file == null) return;
        if (file.getLocalPath() == null) {
            throw new IOException("File has no localPath: uuid=" + file.getUuid());
        }

        // localPath starts with "/" -> resolve safely
        Path outPath = file.getLocalPath();
        Path outDir = outPath.getParent();
        if (outDir != null) Files.createDirectories(outDir);

        String url = file.getDownloadUrl();

        // If url is blank try fetching it
        if ((url == null || url.isBlank()) && file.getLibraryId() != null && file.getUuid() != null) {
                url = Config.URLS.get("files")
                        + "/basic/api/library/" + file.getLibraryId()
                        + "/document/" + file.getUuid()
                        + "/media";
            }


        if (url == null || url.isBlank()) {
            throw new IOException("No download URL for file: " + file.getLabel() + " (" + file.getUuid() + ")");
        }

        try {
            LogUtil.log(this.getClass(), "Downloading file: " + file.getLabel() + " (" + file.getUuid() + ")");
            byte[] data = broker.doGetAuthBytes(url);
            Files.write(outPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new IOException("Failed downloading: " + url + " -> " + outPath, e);
        }
    }

    // ---------------------------------------------------------------------
    // Folder recursion
    // ---------------------------------------------------------------------

    private void populateFolderRecursive(Folder folder) throws IOException, InterruptedException {
        if (folder.getFilesFeedUrl() == null || folder.getFilesFeedUrl().isBlank()) return;

        try {
            // 1) documents in this folder
            String xmlDocs = broker.doGetAuth(folder.getFilesFeedUrl());
            if (xmlDocs != null && !xmlDocs.isBlank()) {
                XmlUtil xuDocs = XmlUtil.parse(xmlDocs);
                parseFilesIntoFolder(xuDocs, folder);
            }

            // 2) subfolders in this folder (collections)
            String collectionsUrl = folder.getFilesFeedUrl()
                    + (folder.getFilesFeedUrl().contains("?") ? "&" : "?")
                    + "category=collection";

            String xmlFolders = broker.doGetAuth(collectionsUrl);
            if (xmlFolders == null || xmlFolders.isBlank()) return;

            XmlUtil xuFolders = XmlUtil.parse(xmlFolders);
            NodeList entries = xuFolders.nodes("/atom:feed/atom:entry");

            for (int i = 0; i < entries.getLength(); i++) {
                Node e = entries.item(i);
                String term = xuFolders.text("atom:category/@term", e);
                if (term != null && !"collection".equalsIgnoreCase(term)) continue;

                Folder child = parseFolderEntry(xuFolders, e, folder.getPath());
                folder.getFolders().add(child);

                populateFolderRecursive(child);
            }
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception e) {
            throw new IOException("Failed to populate folder " + folder.getUuid(), e);
        }
    }


    // ---------------------------------------------------------------------
    // Parsing helpers
    // ---------------------------------------------------------------------

    private void parseFilesIntoFolder(XmlUtil xu, Folder target) throws Exception {
        NodeList entries = xu.nodes("/atom:feed/atom:entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Node e = entries.item(i);

            String term = xu.text("atom:category/@term", e);
            if (!"document".equalsIgnoreCase(term)) continue;
            if (target.getPath().toString().equals("/") && xu.bool("td:isFiledInFolder", e)) continue;

            File f = new File();
            f.setUuid(xu.text("td:uuid", e));
            f.setAdded(xu.text("td:added", e));
            f.setCreated(xu.text("td:created", e));
            f.setPublished(xu.text("atom:published", e));
            f.setUpdated(xu.text("atom:updated", e));
            f.setAddedBy(xu.text("td:addedBy/snx:userid", e));
            f.setModifiedBy(xu.text("td:modifier/snx:userid", e));
            f.setLabel(firstNonBlank(
                    xu.text("td:label", e),
                    xu.text("atom:title", e)
            ));
            f.setLibraryId(xu.text("td:libraryId", e));
            f.setLibraryType(xu.text("td:libraryType", e));
            f.setVersionUuid(xu.text("td:versionUuid", e));
            f.setVersionLabel(xu.text("td:versionLabel", e));
            f.setPropagation(xu.bool("td:propagation", e));
            f.setRestrictedVisibility(xu.bool("td:restrictedVisibility", e));
            f.setExternal(xu.bool("snx:isExternal", e));
            f.setFolderUuid(target.getPath().toString().equals("/") ? null : target.getUuid());

            String dl = xu.text("atom:link[@rel='enclosure']/@href", e);
            f.setDownloadUrl(dl);

            f.setLocalPath(target.getPath().resolve(f.getLabel()));

            target.getFiles().add(f);
        }
    }

    private Folder parseFolderEntry(XmlUtil xu, Node e, Path parentPath)
            throws Exception {

        Folder f = new Folder();
        f.setUuid(xu.text("td:uuid", e));
        f.setLabel(firstNonBlank(
                xu.text("td:label", e),
                xu.text("atom:title", e)
        ));


        f.setPath(parentPath.resolve(f.getLabel()));

        String feedUrl = xu.text("atom:link[@rel='files']/@href", e);
        if (feedUrl == null) {
            feedUrl = xu.text("atom:content/@src", e);
        }
        f.setFilesFeedUrl(feedUrl);

        return f;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b == null ? "" : b;
    }
}
