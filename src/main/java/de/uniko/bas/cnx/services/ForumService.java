package de.uniko.bas.cnx.services;

import de.uniko.bas.cnx.Config;
import de.uniko.bas.cnx.LogUtil;
import de.uniko.bas.cnx.RestBroker;
import de.uniko.bas.cnx.XmlUtil;
import de.uniko.bas.cnx.objects.Community;
import de.uniko.bas.cnx.objects.Forum;
import de.uniko.bas.cnx.objects.Topic;
import de.uniko.bas.cnx.objects.Reply;
import de.uniko.bas.cnx.objects.File;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class ForumService {
    private final RestBroker broker = new RestBroker();

    public void getCommunityForums(Community c) throws IOException, InterruptedException {
        c.setForums(getForums(c));
    }

    public List<Forum> getForums(Community c) throws IOException, InterruptedException {
        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Getting forums for community: " + c.getUuid());

        try {
            String url = Config.URLS.get("forums") + Config.URLS.get("forums_getCommunityForums");
            url = url.replace("${commUuid}", c.getUuid());

            List<Forum> forums = new ArrayList<>();
            for (String pageXml : fetchAllPages(url)) {
                XmlUtil xu = XmlUtil.parse(pageXml);
                NodeList entries = xu.nodes("/atom:feed/atom:entry");

                for (int i = 0; i < entries.getLength(); i++) {
                    LogUtil.log(this.getClass(), "Parsing " + i + "/" +  entries.getLength());

                    Node e = entries.item(i);
                    Forum f = parseForumEntry(xu, e);
                    forums.add(f);

                    // topics for this forum
                    LogUtil.log(this.getClass(), "Retrieving topics for forum: " + f.getTitle());
                    f.setTopics(getTopicsForForum(f.getUuid()));
                }
            }
            return forums;
        } catch(InterruptedException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new IOException("Failed to load community forums for " + c.getUuid(), ex);
        }
    }

    public List<Topic> getTopicsForForum(String forumUuid) throws IOException, InterruptedException {
        try {
            String url = Config.URLS.get("forums") + Config.URLS.get("forums_getTopics");
            url = url.replace("${uUid}", forumUuid);

            List<Topic> topics = new ArrayList<>();
            for (String pageXml : fetchAllPages(url)) {
                XmlUtil xu = XmlUtil.parse(pageXml);
                NodeList entries = xu.nodes("/atom:feed/atom:entry");

                for (int i = 0; i < entries.getLength(); i++) {
                    Node e = entries.item(i);
                    Topic t = parseTopicEntry(xu, e);
                    topics.add(t);

                    // replies for this topic
                    LogUtil.log(this.getClass(), "Retrieving replies for topic: " + t.getTitle());
                    t.setReplies(getRepliesForTopic(t.getUuid()));
                }
            }
            return topics;
        } catch(InterruptedException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new IOException("Failed to load topics for forum " + forumUuid, ex);
        }
    }

    public List<Reply> getRepliesForTopic(String topicUuid) throws IOException, InterruptedException {
        try {
            String url = Config.URLS.get("forums") + Config.URLS.get("forums_getTopicReplies");
            url = url.replace("${uUid}", topicUuid);

            List<Reply> replies = new ArrayList<>();
            for (String pageXml : fetchAllPages(url)) {
                XmlUtil xu = XmlUtil.parse(pageXml);
                NodeList entries = xu.nodes("/atom:feed/atom:entry");

                for (int i = 0; i < entries.getLength(); i++) {
                    Node e = entries.item(i);
                    Reply r = parseReplyEntry(xu, e);
                    replies.add(r);
                }
            }
            return replies;
        } catch(InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Failed to load replies for topic " + topicUuid, ex);
        }
    }

    // ---------------------------------------------------------------------
    // Downloads
    // ---------------------------------------------------------------------


    public void downloadForumAttachments(Community c) throws IOException {
        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Downloading forum attachments");

        if (c.getForums() == null) {
            throw new IOException("Community has no forums loaded.");
        }

        try {
            Path forumsRoot = Path.of(Config.EXPORTDIR, c.getUuid(), "forum");

            for (Forum forum : c.getForums()) {
                if (forum.getTopics() == null) continue;
                Path forumPath = forumsRoot.resolve(forum.getTitle());

                for (Topic topic : forum.getTopics()) {
                    String topicDirName = safePathSegment(firstNonBlank(topic.getTitle(), topic.getUuid()));
                    Path topicDir = forumPath.resolve(topicDirName);

                    // topic-level attachments
                    if (topic.getAttachments() != null && !topic.getAttachments().isEmpty()) {
                        Path topicAttachDir = topicDir.resolve("topic");

                        for (File f : topic.getAttachments()) {
                            Files.createDirectories(topicAttachDir);
                            downloadOneAttachment(f, topicAttachDir);
                        }
                    }

                    // reply-level attachments
                    if (topic.getReplies() != null && !topic.getReplies().isEmpty()) {
                        for (Reply reply : topic.getReplies()) {
                            String replyFolder = safePathSegment(firstNonBlank(reply.getUuid(), "reply"));
                            Path replyDir = topicDir.resolve(replyFolder);

                            if (reply.getAttachments() != null && !reply.getAttachments().isEmpty()) {
                                for (File f : reply.getAttachments()) {
                                    Files.createDirectories(replyDir);
                                    downloadOneAttachment(f, replyDir);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed downloading forum attachments", e);
        }
    }

    private void downloadOneAttachment(File file, Path destDir) throws Exception {
        if (file == null) return;
        String url = file.getDownloadUrl();
        if (url == null || url.isBlank()) return;

        String name = firstNonBlank(file.getLabel(), guessFileNameFromUrl(url), "attachment.bin");
        name = safePathSegment(name);

        Path out = uniquePath(destDir.resolve(name));
        Files.createDirectories(out.getParent());

        LogUtil.log(this.getClass(), "Downloading file: " + file.getLabel() + " (" + file.getUuid() + ")");
        byte[] data = broker.doGetAuthBytes(url);
        Files.write(out, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Path uniquePath(Path p) {
        if (!Files.exists(p)) return p;

        String fileName = p.getFileName().toString();
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }

        int i = 1;
        while (true) {
            Path candidate = p.getParent().resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(candidate)) return candidate;
            i++;
        }
    }

    private static String safePathSegment(String s) {
        if (s == null) return "unknown";
        String t = s.trim();
        if (t.isEmpty()) return "unknown";
        // windows + generic fs safe
        t = t.replaceAll("[\\\\/:*?\"<>|]", "_");
        // avoid trailing dots/spaces on Windows
        t = t.replaceAll("[.\\s]+$", "");
        if (t.isEmpty()) return "unknown";
        return t;
    }

    // ------------------------------------------------------------
    // Parsing (XPath) — tuned for Atom + snx + td + thr
    // ------------------------------------------------------------

    private Forum parseForumEntry(XmlUtil xu, Node e) throws Exception {
        Forum f = new Forum();
        f.setUuid(firstNonBlank(
                xu.text("snx:uuid", e),
                xu.text("td:uuid", e),
                uuidFromIdUrn(xu.text("atom:id", e))
        ));
        f.setTitle(xu.text("atom:title", e));
        f.setPublished(xu.text("atom:published", e));
        f.setUpdated(xu.text("atom:updated", e));
        f.setAuthorUserId(firstNonBlank(
                xu.text("atom:author/snx:userid", e),
                xu.text("snx:userid", e)
        ));
        return f;
    }

    private Topic parseTopicEntry(XmlUtil xu, Node e) throws Exception {
        Topic t = new Topic();
        t.setUuid(firstNonBlank(
                xu.text("snx:uuid", e),
                xu.text("td:uuid", e),
                uuidFromIdUrn(xu.text("atom:id", e))
        ));
        t.setTitle(xu.text("atom:title", e));
        t.setPublished(xu.text("atom:published", e));
        t.setUpdated(xu.text("atom:updated", e));
        t.setAuthorUserId(firstNonBlank(
                xu.text("atom:author/snx:userid", e),
                xu.text("snx:userid", e)
        ));
        t.setContent(xu.text("atom:content", e));

        t.setAttachments(extractSnxFieldAttachments(xu, e));

        return t;
    }

    private Reply parseReplyEntry(XmlUtil xu, Node e) throws Exception {
        Reply r = new Reply();

        String entryId = xu.text("atom:id", e);
        r.setUuid(uuidFromIdUrn(entryId));

        r.setPublished(xu.text("atom:published", e));
        r.setUpdated(xu.text("atom:updated", e));
        r.setAuthorUserId(xu.text("atom:author/snx:userid", e));

        String replyToRef = xu.text("thr:in-reply-to/@ref", e);
        r.setReplyToId(uuidFromIdUrn(replyToRef));

        r.setContent(xu.text("atom:content", e));

        String likeCount = xu.text("atom:link[@rel='recommendations']/@snx:recommendation", e);
        if (likeCount != null && !likeCount.isBlank()) {
            try { r.setLikeCount(Integer.parseInt(likeCount)); } catch (Exception ignore) {
                // Ignored
            }
        }

        r.setRecommendationsUrl(xu.text("atom:link[@rel='recommendations']/@href", e));

        r.setAttachments(extractSnxFieldAttachments(xu, e));

        return r;
    }

    private List<File> extractSnxFieldAttachments(XmlUtil xu, Node entry) throws Exception {
        List<File> out = new ArrayList<>();

        NodeList fileFields = xu.nodes("snx:field[@type='file']", entry);
        for (int i = 0; i < fileFields.getLength(); i++) {
            Node field = fileFields.item(i);

            String fieldName = xu.text("@name", field);

            NodeList links = xu.nodes("atom:link", field);
            for (int j = 0; j < links.getLength(); j++) {
                Node l = links.item(j);

                String href = xu.text("@href", l);
                if (href == null || href.isBlank()) continue;

                String name = xu.text("@name", l);

                File f = new File();
                f.setDownloadUrl(href);
                f.setLabel(firstNonBlank(name, fieldName, guessFileNameFromUrl(href)));

                out.add(f);
            }
        }

        return out;
    }

    private static String guessFileNameFromUrl(String href) {
        if (href == null) return null;
        String s = href;
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        int slash = s.lastIndexOf('/');
        return (slash >= 0 && slash + 1 < s.length()) ? s.substring(slash + 1) : s;
    }

    // ------------------------------------------------------------
    // Pagination helper
    // ------------------------------------------------------------

    private List<String> fetchAllPages(String firstUrl) throws IOException, InterruptedException {
        List<String> pages = new ArrayList<>();
        String url = firstUrl;

        while (url != null && !url.isBlank()) {
            String xml = broker.doGetAuth(url);
            if (xml == null || xml.isBlank()) break;

            pages.add(xml);

            try {
                XmlUtil xu = XmlUtil.parse(xml);
                url = xu.text("/atom:feed/atom:link[@rel='next']/@href");
            } catch (Exception parseEx) {
                url = null;
            }
        }
        return pages;
    }

    private static String uuidFromIdUrn(String atomId) {
        if (atomId == null) return null;
        int idx = atomId.lastIndexOf(':');
        if (idx >= 0 && idx + 1 < atomId.length()) return atomId.substring(idx + 1);
        return atomId;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}
