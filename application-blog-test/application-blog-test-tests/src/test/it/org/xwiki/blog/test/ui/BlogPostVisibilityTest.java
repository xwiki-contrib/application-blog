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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.blog.test.po.BlogPostInlinePage;
import org.xwiki.blog.test.po.BlogPostListPage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.LoginPage;

/**
 * Verify who can see a blog post depending on whether it is published and hidden.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogPostVisibilityTest extends AbstractTest
{
    private static final String PASSWORD = "password";

    private static final String PUBLISHED_TITLE = "Visibility published post";

    private static final String UNPUBLISHED_TITLE = "Visibility unpublished post";

    private static final String HIDDEN_TITLE = "Visibility hidden post";

    @Test
    public void unpublishedAndHiddenPostsAreVisibleOnlyToTheirAuthorUntilPublished()
    {
        String author = getTestClassName() + "_" + getTestMethodName();
        LocalDocumentReference publishedPost = new LocalDocumentReference("Blog", getTestClassName() + "Published");
        LocalDocumentReference unpublishedPost =
            new LocalDocumentReference("Blog", getTestClassName() + "Unpublished");
        LocalDocumentReference hiddenPost = new LocalDocumentReference("Blog", getTestClassName() + "Hidden");

        // Clean up any left-over from a previous run.
        getUtil().loginAsSuperAdmin();
        getUtil().deletePage(publishedPost);
        getUtil().deletePage(unpublishedPost);
        getUtil().deletePage(hiddenPost);
        getUtil().deletePage("XWiki", author);

        // The posts are created by a regular user, who becomes their author.
        getUtil().createUserAndLogin(author, PASSWORD);
        new BlogPostFixture(getUtil(), publishedPost, PUBLISHED_TITLE).create();
        new BlogPostFixture(getUtil(), unpublishedPost, UNPUBLISHED_TITLE).withPublished(false).create();
        new BlogPostFixture(getUtil(), hiddenPost, HIDDEN_TITLE).withHidden(true).create();

        // The author sees all the posts, and the unpublished one is listed among their unpublished posts.
        List<String> titles = BlogHomePage.gotoPage().getPostTitles();
        Assert.assertTrue(titles.toString(), titles.contains(PUBLISHED_TITLE));
        Assert.assertTrue(titles.toString(), titles.contains(UNPUBLISHED_TITLE));
        Assert.assertTrue(titles.toString(), titles.contains(HIDDEN_TITLE));
        titles = BlogPostListPage.gotoUnpublished().getPostTitles();
        Assert.assertTrue(titles.toString(), titles.contains(UNPUBLISHED_TITLE));
        Assert.assertFalse(titles.toString(), titles.contains(PUBLISHED_TITLE));
        Assert.assertTrue(BlogPostViewPage.gotoPage(hiddenPost).isHidden());

        // Guests see only the post that is published and not hidden, and are asked to log in to see the other ones.
        getUtil().forceGuestUser();
        titles = BlogHomePage.gotoPage().getPostTitles();
        Assert.assertTrue(titles.toString(), titles.contains(PUBLISHED_TITLE));
        Assert.assertFalse(titles.toString(), titles.contains(UNPUBLISHED_TITLE));
        Assert.assertFalse(titles.toString(), titles.contains(HIDDEN_TITLE));
        getUtil().gotoPage(unpublishedPost);
        new LoginPage().assertOnPage();
        getUtil().gotoPage(hiddenPost);
        new LoginPage().assertOnPage();

        // Once the author publishes the post, everyone can see it.
        getUtil().login(author, PASSWORD);
        BlogPostInlinePage editPage = BlogPostViewPage.gotoPage(unpublishedPost).clickEditBlogPostIcon();
        Assert.assertFalse(editPage.isPublished());
        editPage.setPublished(true);
        BlogPostViewPage publishedPage = editPage.clickSaveAndView();
        Assert.assertTrue(publishedPage.isPublished());

        getUtil().forceGuestUser();
        titles = BlogHomePage.gotoPage().getPostTitles();
        Assert.assertTrue(titles.toString(), titles.contains(UNPUBLISHED_TITLE));
        Assert.assertEquals(UNPUBLISHED_TITLE, BlogPostViewPage.gotoPage(unpublishedPost).getDocumentTitle());
    }
}
