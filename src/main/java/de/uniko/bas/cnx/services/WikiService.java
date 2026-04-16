package de.uniko.bas.cnx.services;

import de.uniko.bas.cnx.Config;
import de.uniko.bas.cnx.LogUtil;
import de.uniko.bas.cnx.RestBroker;
import de.uniko.bas.cnx.XmlUtil;
import de.uniko.bas.cnx.objects.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class WikiService {
    private final RestBroker broker = new RestBroker();

    public void getCommunityWikis(Community c) throws IOException, InterruptedException {
        List<Wiki> wikis = getWikis(c);
        c.setWikis(wikis);
    }

    public List<Wiki> getWikis(Community c) throws IOException, InterruptedException {
        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Getting wikis for community: " + c.getUuid());

        try {
            String url = Config.URLS.get("wikis") + Config.URLS.get("wikis_getCommunityWikis");
            url = url.replace("${commUuid}", c.getUuid());

            String xml = broker.doGetAuth(url);
            if (xml == null || xml.isBlank()) return List.of();

            XmlUtil xu = XmlUtil.parse(xml);
            NodeList entries = xu.nodes("/atom:feed/atom:entry");

            List<Wiki> out = new ArrayList<>();
            for (int i = 0; i < entries.getLength(); i++) {
                LogUtil.log(this.getClass(), "Parsing " + i + "/" +  entries.getLength());

                Node e = entries.item(i);
                Wiki w = parseWikiEntry(xu, e);

                // NESTED pages:
                LogUtil.log(this.getClass(), "Retrieving pages for wiki: " + w.getTitle());
                w.setWikiPages(getWikiPagesNested(w.getUuid(), c.getUuid()));

                out.add(w);
            }
            return out;

        } catch (InterruptedException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new IOException("Failed to fetch wikis for community " + c.getUuid(), ex);
        }
    }

    private List<WikiPage> getWikiPagesNested(String wikiUuid, String comUuid) throws IOException, InterruptedException {
        // 1) flat fetch
        List<WikiPage> flat = getWikiPagesFlat(wikiUuid);
        if (flat.isEmpty()) return List.of();

        // 2) enrich each page with parentUuid (navigation endpoint)
        for (WikiPage p : flat) {
            String parentUuid = fetchParentUuidFromNavigation(wikiUuid, p);
            p.setParentUuid(parentUuid);

            // Ensure children list exists (in case WikiPage doesn't initialize it)
            if (p.getChildren() == null) {
                p.setChildren(new ArrayList<>());
            }

            LogUtil.log(this.getClass(), "Retrieving comments for page: " + p.getTitle());
            p.setComments(getWikiPageComments(wikiUuid, p.getUuid()));

            LogUtil.log(this.getClass(), "Retrieving HTML for page: " + p.getTitle());
            p.setHtml(getWikiPageHTML(wikiUuid, p.getUuid()));

            LogUtil.log(this.getClass(), "Retrieving attachments for page: " + p.getTitle());
            p.setAttachments(getWikiPageAttachments(wikiUuid, p.getUuid(), comUuid));

        }

        // 3) map by uuid
        Map<String, WikiPage> byUuid = new LinkedHashMap<>();
        for (WikiPage p : flat) {
            if (p.getUuid() != null && !p.getUuid().isBlank()) {
                byUuid.put(p.getUuid(), p);
            }
        }

        // 4) attach children to parents
        List<WikiPage> roots = new ArrayList<>();
        for (WikiPage p : flat) {
            String parent = p.getParentUuid();
            if (parent == null || parent.isBlank()) {
                roots.add(p);
                continue;
            }

            WikiPage parentPage = byUuid.get(parent);
            if (parentPage == null) {
                // parent not in this feed -> treat as root
                roots.add(p);
                continue;
            }

            if (parentPage.getChildren() == null) {
                parentPage.setChildren(new ArrayList<>());
            }
            parentPage.getChildren().add(p);
        }

        return roots;
    }

    private List<WikiPage> getWikiPagesFlat(String wikiUuid) throws IOException, InterruptedException {
        try {
            String url = Config.URLS.get("wikis") + Config.URLS.get("wikis_getWikiPages");
            url = url.replace("${uUid}", wikiUuid);

            List<WikiPage> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            while (url != null && !url.isBlank() && seen.add(url)) {
                String xml = broker.doGetAuth(url);
                if (xml == null || xml.isBlank()) break;

                XmlUtil xu = XmlUtil.parse(xml);

                NodeList entries = xu.nodes("/atom:feed/atom:entry");
                for (int i = 0; i < entries.getLength(); i++) {
                    WikiPage p = parseWikiPageEntry(xu, entries.item(i));
                    out.add(p);
                }

                // Atom pagination: <link rel="next" href="..."/>
                String next = xu.text("/atom:feed/atom:link[@rel='next']/@href");
                url = (next == null || next.isBlank()) ? null : next;
            }

            return out;

        } catch (InterruptedException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new IOException("Failed to fetch wiki pages for wiki " + wikiUuid, ex);
        }
    }

    private static String encodePathSegment(String s) {
        // URLEncoder ist für Query-Strings -> macht Spaces zu '+'
        // Für Path-Segmente brauchen wir '%20'
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String fetchParentUuidFromNavigation(String wikiUuid, WikiPage page) throws InterruptedException {
        try {
            String pageLabel = page.getLabel();
            if (pageLabel == null || pageLabel.isBlank()) return null;

            String url = Config.URLS.get("wikis") + Config.URLS.get("wikis_getWikiPageNavigation");
            url = url.replace("${wikiUuid}", wikiUuid)
                    .replace("${pageLabel}", encodePathSegment(pageLabel));

            String xml = broker.doGetAuth(url);
            if (xml == null || xml.isBlank()) return null;

            XmlUtil xu = XmlUtil.parse(xml);
            String parentUuid = xu.text("//td:parentUuid"); // ggf. anpassen
            return (parentUuid == null || parentUuid.isBlank()) ? null : parentUuid;

        } catch (InterruptedException ex) {
            throw ex;
        }  catch (Exception ex) {
            return null;
        }
    }

    private List<WikiPageComment> getWikiPageComments(String wikiUuid, String wikiPageUuid) throws IOException, InterruptedException {
        try {
            String url = Config.URLS.get("wikis") + Config.URLS.get("wikis_getWikiPageComments");
            url = url.replace("${wikiUuid}", wikiUuid)
                    .replace("${pageUuid}", wikiPageUuid);

            String xml = broker.doGetAuth(url);
            if (xml == null || xml.isBlank()) return List.of();

            XmlUtil xu = XmlUtil.parse(xml);
            NodeList entries = xu.nodes("/atom:feed/atom:entry");

            List<WikiPageComment> out = new ArrayList<>();
            for (int i = 0; i < entries.getLength(); i++) {
                Node e = entries.item(i);
                WikiPageComment c = parseWikiPageCommentEntry(xu, e);
                c.setWikiUuid(wikiUuid);
                c.setPageUuid(wikiPageUuid);
                out.add(c);
            }
            return out;

        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Failed to fetch comments for wiki=" + wikiUuid + ", page=" + wikiPageUuid, ex);
        }
    }

    private String getWikiPageHTML(String wikiUuid, String wikiPageUuid) throws IOException, InterruptedException {
        try {
            String url = Config.URLS.get("wikis") + Config.URLS.get("wikis_getWikiPageMedia");
            url = url.replace("${wikiUuid}", wikiUuid)
                    .replace("${pageUuid}", wikiPageUuid);

            String html = broker.doGetAuth(url);
            if (html == null || html.isBlank()) return "";

            if (html.startsWith("<?xml")) {
                int end = html.indexOf("?>");
                if (end > 0) {
                    html = html.substring(end + 2).trim();
                }
            }

            return html;

        }
        catch (InterruptedException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IOException("Failed to fetch html for wiki=" + wikiUuid + ", page=" + wikiPageUuid, ex);
        }
    }

    private List<File> getWikiPageAttachments(String wikiUuid, String wikiPageUuid, String comUuid) throws IOException, InterruptedException {
        try {
            String url = Config.URLS.get("wikis") + Config.URLS.get("wikis_getWikiPageFiles");
            url = url.replace("${wikiUuid}", wikiUuid)
                    .replace("${pageUuid}", wikiPageUuid);

            String xml = broker.doGetAuth(url);
            if (xml == null || xml.isBlank()) return List.of();


            XmlUtil xu = XmlUtil.parse(xml);
            NodeList entries = xu.nodes("/atom:feed/atom:entry");

            List<File> out = new ArrayList<>();
            for (int i = 0; i < entries.getLength(); i++) {
                Node e = entries.item(i);
                File file = parseWikiPageAttachment(xu, e);
                out.add(file);
                downloadAttachment(comUuid, wikiUuid, wikiPageUuid, file);
            }
            return out;

        }
        catch (InterruptedException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IOException("Failed to fetch attachments for wiki=" + wikiUuid + ", page=" + wikiPageUuid, ex);
        }
    }


    // -----------------------------
    // Parsing helpers
    // -----------------------------

    private Wiki parseWikiEntry(XmlUtil xu, Node e) throws XPathExpressionException {
        Wiki w = new Wiki();

        w.setUuid(xu.text("td:uuid", e));
        w.setLabel(xu.text("td:label", e)); // often same as external id, but keep it
        w.setExternalInstanceId(xu.text("td:externalInstanceId", e));

        w.setTitle(xu.text("atom:title", e));
        w.setSummary(xu.text("atom:summary", e));

        w.setPublished(xu.text("atom:published", e));
        w.setUpdated(xu.text("atom:updated", e));
        w.setCreated(xu.text("td:created", e));
        w.setModified(xu.text("td:modified", e));

        w.setCommunityUuid(xu.text("snx:communityUuid", e));
        w.setVisibilityComputed(xu.text("td:visibilityComputed", e));
        w.setThemeName(xu.text("td:themeName", e));
        w.setExternal(xu.bool("snx:isExternal", e));

        w.setAuthorUserId(xu.text("atom:author/snx:userid", e));
        w.setModifierUserId(xu.text("td:modifier/snx:userid", e));

        return w;
    }

    private WikiPage parseWikiPageEntry(XmlUtil xu, Node e) throws XPathExpressionException {
        WikiPage p = new WikiPage();

        // IDs / labels
        p.setUuid(xu.text("td:uuid", e));
        p.setLabel(xu.text("td:label", e));

        // Atom fields
        p.setTitle(xu.text("atom:title", e));
        p.setSummary(xu.text("atom:summary", e));

        // Timestamps
        p.setPublished(xu.text("atom:published", e));
        p.setUpdated(xu.text("atom:updated", e));
        p.setCreated(xu.text("td:created", e));
        p.setModified(xu.text("td:modified", e));

        // Visibility + versioning
        p.setVisibility(xu.text("td:visibility", e));
        p.setVersionUuid(xu.text("td:versionUuid", e));
        p.setVersionLabel(xu.text("td:versionLabel", e));

        // Author / modifier
        p.setAuthorUserId(xu.text("atom:author/snx:userid", e));
        p.setModifierUserId(xu.text("td:modifier/snx:userid", e));

        return p;
    }

    private WikiPageComment parseWikiPageCommentEntry(XmlUtil xu, Node e) throws XPathExpressionException {
        WikiPageComment c = new WikiPageComment();

        c.setUuid(xu.text("td:uuid", e));

        c.setTitle(xu.text("atom:title", e));
        c.setContent(xu.text("atom:content", e));       // your XML has <content type="text">...</content>

        c.setPublished(xu.text("atom:published", e));
        c.setUpdated(xu.text("atom:updated", e));
        c.setCreated(xu.text("td:created", e));
        c.setModified(xu.text("td:modified", e));

        c.setLanguage(xu.text("td:language", e));
        c.setVersionLabel(xu.text("td:versionLabel", e));

        // If your XmlUtil doesn't have a bool() that tolerates missing nodes, you can keep it as String.
        c.setDeleteWithRecord(Boolean.parseBoolean(xu.text("td:deleteWithRecord", e)));

        // author + modifier
        c.setAuthorName(xu.text("atom:author/atom:name", e));
        c.setAuthorUserId(xu.text("atom:author/snx:userid", e));

        c.setModifierName(xu.text("td:modifier/atom:name", e));
        c.setModifierUserId(xu.text("td:modifier/snx:userid", e));

        return c;
    }

    private File parseWikiPageAttachment(XmlUtil xu, Node e) throws XPathExpressionException {
        File f = new File();

        f.setUuid(xu.text("td:uuid", e));
        f.setLabel(xu.text("td:label", e));
        f.setLibraryId(xu.text("td:libraryId", e));
        f.setAddedBy(xu.text("atom:author/snx:userid", e));
        f.setModifiedBy(xu.text("td:modifier/snx:userid", e));
        f.setPublished(xu.text("atom:published", e));
        f.setUpdated(xu.text("atom:updated", e));
        f.setCreated(xu.text("td:created", e));
        f.setDownloadUrl(xu.text("atom:content/@src", e));

        return f;
    }


    // -----------------------------
    // File Management
    // -----------------------------

    private void downloadAttachment(String comUuid, String wikiUuid, String wikiPageUuid, File file) throws IOException {
        if (file.getDownloadUrl() == null || file.getDownloadUrl().isBlank()) {
            throw new IOException("No download URL for file: " + file.getLabel() + " (" + file.getUuid() + ")");
        }

        Path path = Path.of(Config.EXPORTDIR, comUuid, "wiki", wikiUuid, wikiPageUuid, file.getLabel());
        Path outDir = path.getParent();
        if (outDir != null) Files.createDirectories(outDir);

        try {
            LogUtil.log(this.getClass(), "Downloading file: " + file.getLabel() + " (" + file.getUuid() + ")");
            byte[] data = broker.doGetAuthBytes(file.getDownloadUrl());
            Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new IOException("Failed downloading: " + file.getLabel() + " -> " + path, e);
        }
    }
}
