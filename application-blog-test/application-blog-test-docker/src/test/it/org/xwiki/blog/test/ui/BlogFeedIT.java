/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.blog.test.ui;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.blog.test.ui.BlogTestData.WIKI;

/**
 * Verify the feeds of the blogs: the feed of a blog and the feed of a category.
 *
 * @version $Id$
 */
@UITest
class BlogFeedIT
{
    private static final String BLOG = "Blog";

    private static final String WEB_HOME = "WebHome";

    private static final String BLOG_RSS = "Blog.BlogRss";

    private static final String CATEGORY_RSS = "Blog.CategoryRss";

    private static final String TITLE = "title";

    private static final String DESCRIPTION = "description";

    private static final String CATEGORY = "category";

    private static final String BLOG_PARAMETER = "blog";

    private static final DocumentReference BLOG_RSS_PAGE = new DocumentReference(WIKI, BLOG, "BlogRss");

    private static final String BLOG_A_SPACE = "BlogFeedA";

    private static final String BLOG_B_SPACE = "BlogFeedB.Nested";

    private static final DocumentReference BLOG_A = new DocumentReference(WIKI, BLOG_A_SPACE, WEB_HOME);

    private static final DocumentReference BLOG_B =
        new DocumentReference(WIKI, Arrays.asList("BlogFeedB", "Nested"), WEB_HOME);

    private static final String BLOG_A_TITLE = "Feed blog A";

    private static final String BLOG_B_TITLE = "Feed blog B";

    private static final String DEFAULT_BLOG_POST_TITLE = "Feed post of the default blog";

    private static final String POST_A_TITLE = "Feed post of blog A";

    private static final String POST_B_TITLE = "Feed post of blog B";

    @Test
    @Order(1)
    void feedsOfTheBlogs(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        data.createBlog(BLOG_A, BLOG_A_TITLE, BLOG_A_SPACE, BLOG_A_SPACE + ".Categories", false);
        data.createBlog(BLOG_B, BLOG_B_TITLE, BLOG_B_SPACE, BLOG_B_SPACE + ".Categories", false);
        DocumentReference defaultBlogPost = new DocumentReference(WIKI, BLOG, "BlogFeedPost");
        DocumentReference postA = new DocumentReference("PostA", BLOG_A.getLastSpaceReference());
        DocumentReference postB = new DocumentReference("PostB", BLOG_B.getLastSpaceReference());
        new BlogPostFixture(setup, defaultBlogPost, DEFAULT_BLOG_POST_TITLE).create();
        new BlogPostFixture(setup, postA, POST_A_TITLE).create();
        new BlogPostFixture(setup, postB, POST_B_TITLE).create();

        // The feed of the default blog.
        Feed feed = new Feed(data.getFeed(BLOG_RSS_PAGE, ""));
        assertEquals("Blog/", feed.getChannelPage());
        assertEquals(BLOG, feed.getChannelText(TITLE));
        assertThat(feed.getChannelText("link"), endsWith("/bin/view/Blog/"));
        assertEquals("Most recent blog posts in the Blog space", feed.getChannelText(DESCRIPTION));
        assertThat(feed.getItemTitles(), hasItem(DEFAULT_BLOG_POST_TITLE));
        int index = feed.getItemTitles().indexOf(DEFAULT_BLOG_POST_TITLE);
        assertThat(feed.getItemLinks().get(index), containsString("/bin/view/Blog/BlogFeedPost?language="));
        assertThat(feed.getItemTitles(), not(hasItem(POST_A_TITLE)));

        // The feed of a blog given in the request.
        feed = new Feed(data.getFeed(BLOG_RSS_PAGE, "", BLOG_PARAMETER, "BlogFeedA.WebHome"));
        assertEquals("BlogFeedA/", feed.getChannelPage());
        assertEquals(BLOG_A_TITLE, feed.getChannelText(TITLE));
        assertEquals("Most recent blog posts in the BlogFeedA space", feed.getChannelText(DESCRIPTION));
        assertEquals(List.of(POST_A_TITLE), feed.getItemTitles());

        // The feed of a blog in a nested space, given in the request or displayed on the blog.
        feed = new Feed(data.getFeed(BLOG_RSS_PAGE, "", BLOG_PARAMETER, "BlogFeedB.Nested.WebHome"));
        assertEquals("BlogFeedB/Nested/", feed.getChannelPage());
        assertEquals(BLOG_B_TITLE, feed.getChannelText(TITLE));
        assertEquals("Most recent blog posts in the BlogFeedB.Nested space", feed.getChannelText(DESCRIPTION));
        assertEquals(List.of(POST_B_TITLE), feed.getItemTitles());
        feed = new Feed(data.getFeed(BLOG_B, BLOG_RSS));
        assertEquals(BLOG_B_TITLE, feed.getChannelText(TITLE));
        assertEquals(List.of(POST_B_TITLE), feed.getItemTitles());

        setup.deletePage(defaultBlogPost);
    }

    @Test
    @Order(2)
    void feedsOfTheCategories(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference news = new DocumentReference(WIKI, Arrays.asList(BLOG, "Categories"), "News");
        DocumentReference post = new DocumentReference(WIKI, BLOG, "BlogFeedNewsPost");
        new BlogPostFixture(setup, post, "Feed post in News").withCategory("Blog.Categories.News").create();

        Feed feed = new Feed(data.getFeed(news, CATEGORY_RSS));
        assertEquals("Blog/Categories/News", feed.getChannelPage());
        assertEquals("News", feed.getChannelText(TITLE));
        assertEquals("Most recent blog posts in the News category", feed.getChannelText(DESCRIPTION));
        assertThat(feed.getItemTitles(), hasItem("Feed post in News"));

        // The feed of the default categories home. It has no sheet binding value, so the feed displays the name of its
        // category, which it doesn't have.
        feed = new Feed(data.getFeed(new DocumentReference(WIKI, Arrays.asList(BLOG, "Categories"), WEB_HOME),
            CATEGORY_RSS));
        assertEquals("Blog/Categories/", feed.getChannelPage());
        assertEquals("", feed.getChannelText(TITLE));

        // The feed of the category given in the request.
        DocumentReference mainPage = new DocumentReference(WIKI, "Main", WEB_HOME);
        feed = new Feed(data.getFeed(mainPage, CATEGORY_RSS, CATEGORY, "Blog.Categories.News"));
        assertEquals("Blog/Categories/News", feed.getChannelPage());
        assertEquals("News", feed.getChannelText(TITLE));

        // The feed of the categories home given in the request.
        data.createCategoriesHome(new DocumentReference(WIKI, Arrays.asList(BLOG_A_SPACE, "Categories"), WEB_HOME));
        feed = new Feed(data.getFeed(mainPage, CATEGORY_RSS, CATEGORY, "BlogFeedA.Categories.WebHome"));
        assertEquals("BlogFeedA/Categories/", feed.getChannelPage());

        setup.deletePage(post);
    }
}
