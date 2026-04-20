package de.uniko.bas.cnx.services;

import de.uniko.bas.cnx.Config;
import de.uniko.bas.cnx.LogUtil;
import de.uniko.bas.cnx.RestBroker;
import de.uniko.bas.cnx.XmlUtil;
import de.uniko.bas.cnx.objects.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommunityService {
    private final RestBroker broker = new RestBroker();

    private final FileService fileService = new FileService();
    private final ForumService forumService = new ForumService();
    private final WikiService wikiService = new WikiService();
    private final BlogService blogService = new BlogService();

    public Community getCommunity(String uUid) throws Exception {
        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Getting community: " + uUid);

        Community c = getCommunityBase(uUid);
        getBookmarks(c);
        getEvents(c);
        getFeedLinks(c);
        getLogo(c);
        getMembers(c);
        getRemoteApplications(c);
        getWidgets(c);

        blogService.getCommunityBlogs(c);
        fileService.getCommunityFiles(c);
        forumService.getCommunityForums(c);
        wikiService.getCommunityWikis(c);


        fileService.downloadAllFiles(c);
        forumService.downloadForumAttachments(c);

        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Getting subcommunities for community: " + uUid);

        getSubCommunities(c);

        return c;
    }

    public Community getCommunityBase(String uUid) throws Exception {
        LogUtil.log(this.getClass(), "Getting base data");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getCommunity");
        url = url.replace("${uUid}", uUid);
        String result = broker.doGetAuth(url);

        if (result == null || result.isBlank()) {
            return null;
        }

        XmlUtil xml = XmlUtil.parse(result);

        Community c = new Community();
        c.setUuid(xml.text("/atom:entry/snx:communityUuid"));
        c.setName(xml.text("/atom:entry/atom:title"));
        c.setType(xml.text("/atom:entry/snx:communityType"));
        c.setContent(xml.text("/atom:entry/atom:content"));
        c.setSummary(xml.text("/atom:entry/atom:summary"));
        c.setAuthorUuid(xml.text("/atom:entry/atom:author/snx:userid"));
        c.setListWhenRestricted(xml.bool("/atom:entry/snx:listWhenRestricted"));
        c.setMemberCount(xml.intVal("/atom:entry/snx:membercount"));
        c.setPreModeration(xml.bool("/atom:entry/snx:preModeration"));
        c.setPostModeration(xml.bool("/atom:entry/snx:postModeration"));
        c.setPublished(xml.text("/atom:entry/atom:published"));
        c.setUpdated(xml.text("/atom:entry/atom:updated"));
        c.setTheme(xml.text("/atom:entry/snx:communityTheme/@snx:uuid"));

        return c;
    }

    public void getBookmarks(Community c) throws Exception {
        LogUtil.log(this.getClass(), "Getting bookmarks");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getBookmarks");
        url = url.replace("${uUid}", c.getUuid());

        List<Bookmark> bookmarks = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        while (url != null && !url.isBlank() && seen.add(url)) {
            String result = broker.doGetAuth(url);

            if (result == null || result.isBlank()) {
                return;
            }

            XmlUtil x = XmlUtil.parse(result);
            NodeList entries = x.nodes("/atom:feed/atom:entry");

            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                Bookmark b = new Bookmark();

                b.setTitle(x.text("atom:title", entry));
                b.setSummary(x.text("atom:summary", entry));
                b.setContent(x.text("atom:content", entry));
                b.setPublished(x.text("atom:published", entry));
                b.setUpdated(x.text("atom:updated", entry));

                b.setAuthor(x.text("atom:author/snx:userid", entry));
                b.setContributor(x.text("atom:contributor/snx:userid", entry));

                // external bookmark URL = link without rel attribute
                b.setLink(x.text("atom:link[not(@rel)]/@href", entry));

                bookmarks.add(b);
            }

            String next = x.text("/atom:feed/atom:link[@rel='next']/@href");
            url = (next == null || next.isBlank()) ? null : next;
        }

        c.setBookmarks(bookmarks);
    }

    public void getEvents(Community c) throws Exception {
        LogUtil.log(this.getClass(), "Getting events");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getEvents");
        url = url.replace("${uUid}", c.getUuid());
        String result = broker.doGetAuth(url);

        if (result == null || result.isBlank()) {
            return;
        }

        XmlUtil x = XmlUtil.parse(result);
        NodeList entries = x.nodes("/atom:feed/atom:entry");

        List<Event> events = new ArrayList<>();

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            Event e = new Event();

            e.setUuid(x.text("snx:eventUuid", entry));
            e.setTitle(x.text("atom:title", entry));
            e.setPublished(x.text("atom:published", entry));
            e.setUpdated(x.text("atom:updated", entry));
            e.setAuthor(x.text("atom:author/snx:userid", entry));

            e.setLocation(x.text("snx:location", entry));

            // snx:allday is "1" or "0"
            String allDayRaw = x.text("snx:allday", entry);
            e.setAllDay("1".equals(allDayRaw) || "true".equalsIgnoreCase(allDayRaw));

            // Start/end may be under snx:recurrence/snx:period/... OR directly under snx:recurrence/...
            e.setStartDate(firstNonNull(
                    x.text("snx:recurrence/snx:period/snx:startDate", entry),
                    x.text("snx:recurrence/snx:startDate", entry)
            ));

            e.setEndDate(firstNonNull(
                    x.text("snx:recurrence/snx:period/snx:endDate", entry),
                    x.text("snx:recurrence/snx:endDate", entry)
            ));

            // Until is only present in some recurrence modes
            e.setUntilDate(x.text("snx:recurrence/snx:until", entry));

            // Recurrence attributes (only present for some entries)
            e.setByDay(x.text("snx:recurrence/snx:byDay", entry));
            e.setFrequency(x.text("snx:recurrence/@frequency", entry));
            e.setInterval(x.text("snx:recurrence/@interval", entry)); // in your sample it's "null" as a string

            // Optional cleanup: treat literal "null" as null
            if ("null".equalsIgnoreCase(e.getInterval())) e.setInterval(null);

            events.add(e);
        }

        c.setEvents(events);
    }

    public void getFeedLinks(Community c) throws Exception {
        LogUtil.log(this.getClass(), "Getting feed links");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getFeedLinks");
        url = url.replace("${uUid}", c.getUuid());
        String result = broker.doGetAuth(url);

        if (result == null || result.isBlank()) {
            return;
        }

        XmlUtil x = XmlUtil.parse(result);
        // Unsure if this is required
    }

    public void getLogo(Community c) throws Exception {
        LogUtil.log(this.getClass(), "Getting logo");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getLogo");
        url = url.replace("${uUid}", c.getUuid());
        byte[] data = broker.doGetAuthBytes(url);

        if (data == null || data.length == 0) return;

        Path dir = Path.of(Config.EXPORTDIR ,c.getUuid());
        Files.createDirectories(dir);

        String safeName = (c.getName() != null ? c.getName() : c.getUuid()).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        Path file = dir.resolve(safeName + "-logo.png");

        Files.write(file, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        c.setLogo(file.toString());
    }

    public void getMembers(Community c) throws Exception {
        LogUtil.log(this.getClass(), "Getting members");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getMembers");
        url = url.replace("${uUid}", c.getUuid());
        String result = broker.doGetAuth(url);

        if (result == null || result.isBlank()) {
            return;
        }

        XmlUtil x = XmlUtil.parse(result);

        NodeList entries = x.nodes("/atom:feed/atom:entry");
        List<Member> members = new ArrayList<>();

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);

            Member m = new Member();
            m.setUserid(x.text("atom:contributor/snx:userid", entry));
            m.setRole(x.text("snx:role", entry));
            m.setName(x.text("atom:title", entry));
            m.setEmail(x.text("atom:contributor/atom:email", entry));

            String state = x.text("atom:contributor/snx:userState", entry);
            m.setActive("active".equalsIgnoreCase(state));

            String ext = x.text("atom:contributor/snx:isExternal", entry);
            m.setExternal("true".equalsIgnoreCase(ext) || "1".equals(ext));

            members.add(m);
        }

        c.setMembers(members);
    }

    public void getRemoteApplications(Community c) throws Exception {
        LogUtil.log(this.getClass(), "Getting remote applications");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getRemoteApplications");
        url = url.replace("${uUid}", c.getUuid());
        String result = broker.doGetAuth(url);

        if (result == null || result.isBlank()) {
            return;
        }

        XmlUtil x = XmlUtil.parse(result);
        NodeList entries = x.nodes("/atom:feed/atom:entry");

        List<RemoteApplication> apps = new ArrayList<>();

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);

            RemoteApplication a = new RemoteApplication();
            a.setId(x.text("atom:id", entry));
            a.setTitle(x.text("atom:title", entry));
            a.setContent(x.text("atom:content", entry));
            a.setPublished(x.text("atom:published", entry));
            a.setUpdated(x.text("atom:updated", entry));
            a.setFeedUrl(x.text("atom:link[@rel='http://www.ibm.com/xmlns/prod/sn/remote-application/feed']/@href", entry));
            a.setPublishUrl(x.text("atom:link[@rel='http://www.ibm.com/xmlns/prod/sn/remote-application/publish']/@href", entry));

            apps.add(a);
        }

        c.setRemoteApplications(apps);
    }

    public void getSubCommunities(Community c) throws Exception {
        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getSubCommunities");
        url = url.replace("${uUid}", c.getUuid());
        String result = broker.doGetAuth(url);

        if (result == null || result.isBlank()) {
            return;
        }

        XmlUtil x = XmlUtil.parse(result);
        NodeList entries = x.nodes("/atom:feed/atom:entry");

        List<Community> communities = new ArrayList<>();

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String subComUuid = x.text("snx:communityUuid", entry);

            Community subCom = getCommunity(subComUuid);
            subCom.setParentId(c.getUuid());

            communities.add(subCom);
        }

        c.setSubCommunities(communities);
    }

    public void getWidgets(Community c) throws Exception {
        LogUtil.log(this.getClass(), "Getting widgets");

        String url = Config.URLS.get("communities") + Config.URLS.get("communities_getWidgets");
        url = url.replace("${uUid}", c.getUuid());
        String result = broker.doGetAuth(url);

        if (result == null || result.isBlank()) {
            return;
        }

        XmlUtil x = XmlUtil.parse(result);
        NodeList entries = x.nodes("/atom:feed/atom:entry");

        List<Widget> widgets = new ArrayList<>();

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);

            Widget w = new Widget();
            w.setTitle(x.text("atom:title", entry));

            // "alternate" points to the UI page for that widget
            w.setLink(x.text("atom:link[@rel='alternate']/@href", entry));

            w.setDefId(x.text("snx:widgetDefId", entry));
            w.setInstanceId(x.text("snx:widgetInstanceId", entry));
            w.setHidden(x.bool("snx:hidden", entry));
            w.setLocation(x.text("snx:location", entry));
            w.setPrevInstanceId(x.text("snx:previousWidgetInstanceId", entry));

            widgets.add(w);
        }

        c.setWidgets(widgets);
    }

    private static String firstNonNull(String a, String b) {
        return (a != null) ? a : b;
    }
}
