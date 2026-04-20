package de.uniko.bas.cnx.services;

import de.uniko.bas.cnx.Config;
import de.uniko.bas.cnx.LogUtil;
import de.uniko.bas.cnx.RestBroker;
import de.uniko.bas.cnx.XmlUtil;
import de.uniko.bas.cnx.objects.Blog;
import de.uniko.bas.cnx.objects.BlogComment;
import de.uniko.bas.cnx.objects.Community;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlogService {
    private final RestBroker broker = new RestBroker();

    public void getCommunityBlogs(Community c) throws IOException, InterruptedException {
        List<Blog> blogs = getBlogs(c);
        c.setBlogs(blogs);
    }

    public List<Blog> getBlogs(Community c) throws IOException, InterruptedException {
        LogUtil.printSeparator();
        LogUtil.log(this.getClass(), "Getting blogs for community: " + c.getUuid());

        try {
            String url = Config.URLS.get("blogs") + Config.URLS.get("blogs_getBlog");
            url = url.replace("${handle}", c.getUuid());

            List<Blog> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            while (url != null && !url.isBlank() && seen.add(url)) {
                String xml = broker.doGetAuth(url);
                if (xml == null || xml.isBlank()) return List.of();

                XmlUtil xu = XmlUtil.parse(xml);
                NodeList entries = xu.nodes("/atom:feed/atom:entry");


                for (int i = 0; i < entries.getLength(); i++) {
                    LogUtil.log(this.getClass(), "Parsing " + (i + 1) + "/" + entries.getLength());

                    Node entry = entries.item(i);

                    Blog blog = parseBlogEntry(xu, entry);

                    if (blog.getCommentCount() > 0) {
                        blog.setComments(getBlogComments(c.getUuid(), blog.getUuid()));
                    }

                    out.add(blog);
                }

                String next = xu.text("/atom:feed/atom:link[@rel='next']/@href");
                url = (next == null || next.isBlank()) ? null : next;
            }

            return out;
        } catch (InterruptedException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new IOException("Failed to fetch blogs for community " + c.getUuid(), ex);
        }
    }

    private List<BlogComment> getBlogComments(String comUuid, String blogUuid) throws IOException, InterruptedException {
        try {
            String url = Config.URLS.get("blogs") + Config.URLS.get("blogs_getBlogPostComments");
            url = url.replace("${handle}", comUuid).replace("${uUid}", blogUuid);

            String xml = broker.doGetAuth(url);
            if (xml == null || xml.isBlank()) return List.of();

            XmlUtil xu = XmlUtil.parse(xml);
            NodeList entries = xu.nodes("/atom:feed/atom:entry");

            List<BlogComment> out = new ArrayList<>();
            for (int i = 0; i < entries.getLength(); i++) {
                LogUtil.log(this.getClass(), "Parsing " + (i + 1) + "/" + entries.getLength());

                Node entry = entries.item(i);

                BlogComment comment = parseBlogCommentEntry(xu, entry);
                out.add(comment);
            }

            return out;
        } catch (InterruptedException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new IOException("Failed to fetch comments for blog " + blogUuid, ex);
        }
    }



    // -------------
    //    Parser
    // -------------

    private Blog parseBlogEntry(XmlUtil xu, Node e) throws XPathExpressionException {
        Blog blog = new Blog();

        blog.setId(xu.text("atom:id", e));
        blog.setUuid(blog.getId().split(":entry-")[1]);
        blog.setLink(xu.text("atom:link/@href", e));
        blog.setTitle(xu.text("atom:title", e));
        blog.setUpdated(xu.text("atom:updated", e));
        blog.setEdited(xu.text("app:edited", e));
        blog.setPublished(xu.text("atom:published", e));

        blog.setRecommendationCount(xu.intVal("snx:rank[@scheme='http://www.ibm.com/xmlns/prod/sn/recommendations']", e));
        blog.setCommentCount(xu.intVal("snx:rank[@scheme='http://www.ibm.com/xmlns/prod/sn/comment']", e));
        blog.setHitCount(xu.intVal("snx:rank[@scheme='http://www.ibm.com/xmlns/prod/sn/hit']", e));

        blog.setAuthorName(xu.text("atom:author/atom:name", e));
        blog.setAuthorEmail(xu.text("atom:author/atom:email", e));
        blog.setAuthorUuid(xu.text("atom:author/snx:userid", e));
        blog.setAuthorState(xu.bool("atom:author/snx:userState", e));
        blog.setAuthorIsExternal(xu.bool("atom:author/snx:isExternal", e));

        blog.setSummary(xu.text("atom:summary", e));
        blog.setContent(xu.text("atom:content", e));

        return blog;
    }

    private BlogComment parseBlogCommentEntry(XmlUtil xu, Node e) throws XPathExpressionException {
        BlogComment comment = new BlogComment();

        comment.setId(xu.text("atom:id", e));
        comment.setUuid(comment.getId().split(":comment-")[1]);
        comment.setTitle(xu.text("atom:title", e));
        comment.setUpdated(xu.text("atom:updated", e));
        comment.setEdited(xu.text("app:edited", e));
        comment.setPublished(xu.text("atom:published", e));

        comment.setAuthorName(xu.text("atom:author/atom:name", e));
        comment.setAuthorEmail(xu.text("atom:author/atom:email", e));
        comment.setAuthorUuid(xu.text("atom:author/snx:userid", e));
        comment.setAuthorState(xu.bool("atom:author/snx:userState", e));
        comment.setAuthorIsExternal(xu.bool("atom:author/snx:isExternal", e));

        comment.setContributorName(xu.text("atom:contributor/atom:name", e));
        comment.setContributorEmail(xu.text("atom:contributor/atom:email", e));
        comment.setContributorUuid(xu.text("atom:contributor/snx:userid", e));
        comment.setContributorState(xu.bool("atom:contributor/snx:userState", e));
        comment.setContributorIsExternal(xu.bool("atom:contributor/snx:isExternal", e));

        comment.setContent(xu.text("atom:content", e));
        comment.setRecommendationCount(xu.intVal("snx:rank[@scheme='http://www.ibm.com/xmlns/prod/sn/recommendations']", e));

        return comment;
    }
}
