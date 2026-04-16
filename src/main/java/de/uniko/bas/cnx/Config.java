package de.uniko.bas.cnx;

import java.util.HashMap;
import java.util.Map;

public class Config {
    private Config() {}

    public static Auth<String, String> AUTH;

    public static String EXPORTDIR = "export";

    public static final Map<String, String> URLS = new HashMap<>();
    static {
        // activities
        URLS.put("activities_addMember", "/service/atom2/acl?activityUuid=${uUid}");
        URLS.put("activities_createActivity", "/service/atom2/activities");
        URLS.put("activities_createCommunityActivity", "/service/atom2/activities?commUuid=${commUuid}");
        URLS.put("activities_createNode", "/service/atom2/activity?activityUuid=${uUid}");
        URLS.put("activities_deleteActivity", "/service/atom2/activitynode?activityNodeUuid=${uUid}");
        URLS.put("activities_getMembers", "/service/atom2/acl?activityUuid=${uUid}");
        URLS.put("activities_getNodes", "/service/atom2/activity?activityUuid=${uUid}&sortBy=createdby&sortOrder=asc");
        URLS.put("activities_getCommunityActivities", "/service/atom2/activities?commUuid=${commUuid}");
        URLS.put("activities_getMyActivities", "/service/atom2/activities?sortBy=createdby&sortOrder=asc");
        URLS.put("activities_getMyCompletedActivities", "/service/atom2/completed?sortBy=createdby&sortOrder=asc");

        // blogs
        URLS.put("blogs_createBlog", "/homepage/api/blogs");
        URLS.put("blogs_createBlogPost", "/${handle}/api/entries");
        URLS.put("blogs_createBlogPostCommentLikes", "/${handle}/api/recommend/comments/${uUid}");
        URLS.put("blogs_createBlogPostComments", "/${handle}/api/comments");
        URLS.put("blogs_createBlogPostLikes", "/${handle}/api/recommend/entries/${uUid}");
        URLS.put("blogs_createIdeationBlog", "/homepage/api/blogs?commUuid=${commUuid}&blogType=ideationblog");

        URLS.put("blogs_deleteBlog", "/homepage/api/blogs/${uUid}");

        URLS.put("blogs_getBlog", "/${handle}/api/entries");
        URLS.put("blogs_getBlogPosts", "/${handle}/api/entries");
        URLS.put("blogs_getBlogPostCommentLikes", "/${handle}/api/recommend/comments/${uUid}");
        URLS.put("blogs_getBlogPostComments", "/${handle}/api/entrycomments/${uUid}");
        URLS.put("blogs_getBlogPostLikes", "/${handle}/api/recommend/entries/${uUid}");
        URLS.put("blogs_getIdeationBlogs", "/homepage/feed/ideationblogs/atom?commUuid=${commUuid}&blogType=ideationblog");
        URLS.put("blogs_getMyBlogs", "/homepage/api/blogs");
        URLS.put("blogs_getMyVotes", "/homepage/feed/myvotes/atom?lang=en_us");

        URLS.put("blogs_updateBlog", "/homepage/api/blogs/${uUid}");

        // bookmarks
        URLS.put("dogear_createBookmark", "/api/app");
        URLS.put("dogear_deleteBookmark", "/api/app?url=${url}");
        URLS.put("dogear_getMyBookmarks", "/api/app?userid=${uUid}");

        // common
        URLS.put("serviceconfigs", "/communities/serviceconfigs");

        // communities
        URLS.put("communities_addMember", "/service/atom/community/members?communityUuid=${uUid}");
        URLS.put("communities_addWidget", "/service/atom/community/widgets?communityUuid=${uUid}");
        URLS.put("communities_createBookmark", "/service/atom/community/bookmarks?communityUuid=${uUid}");
        URLS.put("communities_createCommunity", "/service/atom/communities/my");
        URLS.put("communities_createEvent", "/calendar/atom/calendar/event?calendarUuid=${uUid}");
        URLS.put("communities_createFeedLink", "/service/atom/community/feeds?communityUuid=${uUid}");
        URLS.put("communities_createSubCommunity", "/service/atom/community/subcommunities?communityUuid=${uUid}");

        URLS.put("communities_deleteCommunity", "/service/atom/community/instance?communityUuid=${uUid}");

        URLS.put("communities_updateCommunity", "/service/atom/community/instance?communityUuid=${uUid}"); // dpc
        URLS.put("communities_updateLogo", "/service/html/image?communityUuid=${uUid}");

        URLS.put("communities_getBookmarks", "/service/atom/community/bookmarks?communityUuid=${uUid}");    // Done
        URLS.put("communities_getCommunity", "/service/atom/community/instance?communityUuid=${uUid}");     // Done
        URLS.put("communities_getEvents", "/calendar/atom/calendar/event?calendarUuid=${uUid}&type=event"); // Done
        URLS.put("communities_getFeedLinks", "/service/atom/community/feeds?communityUuid=${uUid}");
        URLS.put("communities_getLogo", "/service/html/image?communityUuid=${uUid}");                       // Done
        URLS.put("communities_getMembers", "/service/atom/community/members?communityUuid=${uUid}");        // Done
        URLS.put("communities_getMyCommunities", "/service/atom/communities/my");
        URLS.put("communities_getRemoteApplications", "/service/atom/community/remoteApplications?communityUuid=${uUid}");  // Done
        URLS.put("communities_getSubCommunities", "/service/atom/community/subcommunities?communityUuid=${uUid}"); // Done
        URLS.put("communities_getWidgets", "/service/atom/community/widgets?communityUuid=${uUid}");        // Done


        // files
        URLS.put("files_addFileToFolder", "/basic/api/collection/${folderUuid}/feed?itemId=${fileUuid}");
        URLS.put("files_createFile", "/basic/api/myuserlibrary/feed");
        URLS.put("files_createFileComment", "/basic/api/library/${libraryUuid}/document/${documentUuid}/feed");
        URLS.put("files_createFolder", "/basic/api/collections/feed");

        URLS.put("files_deleteFile", "/basic/api/myuserlibrary/document/${uUid}/entry");

        URLS.put("files_deleteFolder", "/basic/api/collection/${uUid}/entry");

        URLS.put("files_getCommunityIntrospection", "/basic/api/community/${commUuid}/introspection");
        URLS.put("files_downloadFile", "/basic/api/library/${libraryUuid}/document/${documentUuid}/media");
        URLS.put("files_getCommunityFilesAndFolders", "/basic/api/communitycollection/${commUuid}/feed");
        URLS.put("files_getFile", "/basic/api/library/${libraryUuid}/document/${documentUuid}/entry?includeTags=true");
        URLS.put("files_getFileComments", "/basic/api/library/${libraryUuid}/document/${documentUuid}/feed");
        URLS.put("files_getFolderFiles", "/basic/api/collection/${uUid}/feed");
        URLS.put("files_getMyFiles", "/basic/api/myuserlibrary/feed");
        URLS.put("files_getMyFolders", "/basic/api/collections/feed?creator=${uUid}");

        // forums
        URLS.put("forums_createCommunityForum", "/atom/forums?communityUuid=${commUuid}");
        URLS.put("forums_createForum", "/atom/forums");
        URLS.put("forums_createForumTopic", "/atom/topics?forumUuid=${uUid}");
        URLS.put("forums_createForumTopicReply", "/atom/replies?topicUuid=${uUid}");
        URLS.put("forums_createPostLike", "/atom/recommendation/entries?postUuid=${uUid}");

        URLS.put("forums_deleteForum", "/atom/forum?forumUuid=${uUid}");
        URLS.put("forums_deleteForumTopicReply", "/atom/reply?replyUuid=${uUid}");

        URLS.put("forums_getCommunityForums", "/atom/forums?communityUuid=${commUuid}");
        URLS.put("forums_getMyForums", "/atom/forums/my?view=owner&sortBy=created&sortOrder=asc");
        URLS.put("forums_getPostLikes", "/atom/recommendation/entries?postUuid=${uUid}");
        URLS.put("forums_getTopics", "/atom/topics?forumUuid=${uUid}&sortBy=created&sortOrder=asc");
        URLS.put("forums_getTopicReplies", "/atom/replies?topicUuid=${uUid}&sortBy=created&sortOrder=asc");

        // profiles
        URLS.put("profiles_acceptColleague", "/atom/connection.do?connectionId=${uUid}");
        URLS.put("profiles_createColleague", "/atom/connections.do?key=${key}&connectionType=colleague");
        URLS.put("profiles_createTags", "/atom/profileTags.do?targetKey=${targetKey}&sourceKey=${sourceKey}");

        URLS.put("profiles_deleteColleague", "/atom/connection.do?connectionId=${uUid}");

        URLS.put("profiles_getColleagues", "/atom/connections.do?connectionType=colleague&inclMessage=true&status=${status}&userid=${uUid}");
        URLS.put("profiles_getProfile", "/atom/profile.do?userid=${uUid}");
        URLS.put("profiles_getTags", "/atom/profileTags.do?targetKey=${key}");

        // wikis
        URLS.put("wikis_addMembers", "/basic/api/wiki/${uUid}/members");
        URLS.put("wikis_addWikiPageTags", "/basic/api/wiki/${wikiUuid}/page/${pageUuid}/entry");

        URLS.put("wikis_createWiki", "/basic/api/wikis/feed");
        URLS.put("wikis_createWikiPage", "/basic/api/wiki/${uUid}/feed");
        URLS.put("wikis_createWikiPageComment", "/basic/api/wiki/${wikiUuid}/page/${pageUuid}/feed");

        URLS.put("wikis_deleteWiki", "/basic/api/wiki/${uUid}/entry");
        URLS.put("wikis_deleteWikiPage", "/basic/api/wiki/${wikiUuid}/page/${pageUuid}/entry");

        URLS.put("wikis_getCommunityWikis", "/basic/api/community/${commUuid}/wikis/feed");
        URLS.put("wikis_getMembers", "/basic/api/wiki/${uUid}/members");
        URLS.put("wikis_getMyWikis", "/basic/api/mywikis/feed?role=manager&sortBy=created&sortOrder=asc");
        URLS.put("wikis_getWiki", "/basic/api/wiki/${uUid}/entry");
        URLS.put("wikis_getWikiPageComments", "/basic/api/wiki/${wikiUuid}/page/${pageUuid}/feed?sortOrder=asc");
        URLS.put("wikis_getWikiPageFiles", "/basic/api/wiki/${wikiUuid}/page/${pageUuid}/feed?category=attachment");
        URLS.put("wikis_getWikiPageMedia", "/basic/api/wiki/${wikiUuid}/page/${pageUuid}/media");
        URLS.put("wikis_getWikiPages", "/basic/anonymous/api/wiki/${uUid}/feed?includeTags=true&sortBy=created&sortOrder=asc");
        URLS.put("wikis_getWikiPageNavigation", "/basic/anonymous/api/wiki/${wikiUuid}/navigation/${pageLabel}/entry");

        URLS.put("wikis_updateWiki", "/basic/api/wiki/${uUid}/entry");
    }
    public record Auth<T1, T2>(T1 username, T2 password) { }
}
