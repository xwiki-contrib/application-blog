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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.blog.test.po.BlogPostInlinePage;
import org.xwiki.blog.test.po.BlogPostListPage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LoginPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify who can see a blog post depending on whether it is published and hidden.
 *
 * @version $Id$
 */
@UITest
class BlogPostVisibilityIT
{
    private static final String PASSWORD = "password";

    private static final String PUBLISHED_TITLE = "Visibility published post";

    private static final String UNPUBLISHED_TITLE = "Visibility unpublished post";

    private static final String HIDDEN_TITLE = "Visibility hidden post";

    @Test
    @Order(1)
    void unpublishedAndHiddenPostsAreVisibleOnlyToTheirAuthorUntilPublished(TestUtils setup,
        TestReference testReference)
    {
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        String author = testClassName + "_" + testReference.getLastSpaceReference().getName();
        LocalDocumentReference publishedPost = new LocalDocumentReference("Blog", testClassName + "Published");
        LocalDocumentReference unpublishedPost = new LocalDocumentReference("Blog", testClassName + "Unpublished");
        LocalDocumentReference hiddenPost = new LocalDocumentReference("Blog", testClassName + "Hidden");

        // The posts are created by a regular user, who becomes their author.
        setup.loginAsSuperAdmin();
        setup.createUserAndLogin(author, PASSWORD);
        new BlogPostFixture(setup, publishedPost, PUBLISHED_TITLE).create();
        new BlogPostFixture(setup, unpublishedPost, UNPUBLISHED_TITLE).withPublished(false).create();
        new BlogPostFixture(setup, hiddenPost, HIDDEN_TITLE).withHidden(true).create();

        // The author sees all the posts, and the unpublished one is listed among their unpublished posts.
        List<String> titles = BlogHomePage.gotoPage().getPostTitles();
        assertThat(titles, hasItems(PUBLISHED_TITLE, UNPUBLISHED_TITLE, HIDDEN_TITLE));
        titles = BlogPostListPage.gotoUnpublished().getPostTitles();
        assertThat(titles, hasItem(UNPUBLISHED_TITLE));
        assertThat(titles, not(hasItem(PUBLISHED_TITLE)));
        assertTrue(BlogPostViewPage.gotoPage(hiddenPost).isHidden());

        // Guests see only the post that is published and not hidden, and are asked to log in to see the other ones.
        setup.forceGuestUser();
        titles = BlogHomePage.gotoPage().getPostTitles();
        assertThat(titles, hasItem(PUBLISHED_TITLE));
        assertThat(titles, not(hasItem(UNPUBLISHED_TITLE)));
        assertThat(titles, not(hasItem(HIDDEN_TITLE)));
        setup.gotoPage(unpublishedPost);
        new LoginPage().assertOnPage();
        setup.gotoPage(hiddenPost);
        new LoginPage().assertOnPage();

        // Once the author publishes the post, everyone can see it.
        setup.login(author, PASSWORD);
        BlogPostInlinePage editPage = BlogPostViewPage.gotoPage(unpublishedPost).clickEditBlogPostIcon();
        assertFalse(editPage.isPublished());
        editPage.setPublished(true);
        BlogPostViewPage publishedPage = editPage.clickSaveAndView();
        assertTrue(publishedPage.isPublished());

        setup.forceGuestUser();
        assertThat(BlogHomePage.gotoPage().getPostTitles(), hasItem(UNPUBLISHED_TITLE));
        assertEquals(UNPUBLISHED_TITLE, BlogPostViewPage.gotoPage(unpublishedPost).getDocumentTitle());
    }

    /**
     * BLOG-172: publishing a draft that another user also saved must make it visible to everyone.
     */
    @Test
    @Order(2)
    void draftEditedByAnotherUserIsVisibleToEveryoneOncePublished(TestUtils setup, TestReference testReference)
    {
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        String author = testClassName + "_" + testReference.getLastSpaceReference().getName();
        LocalDocumentReference draft = new LocalDocumentReference("Blog", testClassName + "EditedDraft");
        String title = "Visibility draft edited by another user";

        setup.loginAsSuperAdmin();
        setup.createUserAndLogin(author, PASSWORD);
        new BlogPostFixture(setup, draft, title).withPublished(false).create();

        // An admin edits the draft before the author publishes it.
        setup.loginAsSuperAdmin();
        BlogPostViewPage.gotoPage(draft).clickEditBlogPostIcon().clickSaveAndView();

        setup.login(author, PASSWORD);
        BlogPostInlinePage editPage = BlogPostViewPage.gotoPage(draft).clickEditBlogPostIcon();
        editPage.setPublished(true);
        assertTrue(editPage.clickSaveAndView().isPublished());

        setup.forceGuestUser();
        assertThat(BlogHomePage.gotoPage().getPostTitles(), hasItem(title));
        assertEquals(title, BlogPostViewPage.gotoPage(draft).getDocumentTitle());
    }

    /**
     * BLOG-173: the users a draft is shared with can view it from a link and publish it.
     */
    @Test
    @Order(3)
    void draftSharedWithAnotherUserCanBeViewedAndPublishedByThem(TestUtils setup, TestReference testReference)
    {
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        String author = testClassName + "_" + testReference.getLastSpaceReference().getName();
        String collaborator = author + "_Collaborator";
        LocalDocumentReference draft = new LocalDocumentReference("Blog", testClassName + "SharedDraft");
        String title = "Visibility draft shared with another user";

        setup.loginAsSuperAdmin();
        setup.createUserAndLogin(collaborator, PASSWORD);
        setup.loginAsSuperAdmin();
        setup.createUserAndLogin(author, PASSWORD);
        new BlogPostFixture(setup, draft, title).withPublished(false).withSharedUsers("XWiki." + collaborator)
            .create();

        // Guests still cannot see the draft.
        setup.forceGuestUser();
        setup.gotoPage(draft);
        new LoginPage().assertOnPage();

        // The collaborator opens the draft from a link, and publishes it.
        setup.login(collaborator, PASSWORD);
        assertEquals(title, BlogPostViewPage.gotoPage(draft).getDocumentTitle());
        BlogPostInlinePage editPage = BlogPostViewPage.gotoPage(draft).clickEditBlogPostIcon();
        editPage.setPublished(true);
        assertTrue(editPage.clickSaveAndView().isPublished());

        setup.forceGuestUser();
        assertEquals(title, BlogPostViewPage.gotoPage(draft).getDocumentTitle());
    }

    /**
     * BLOG-53: an admin can hide a post published by another user, and the post stays visible to its author.
     */
    @Test
    @Order(4)
    void adminCanHidePostOfAnotherUser(TestUtils setup, TestReference testReference)
    {
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        String author = testClassName + "_" + testReference.getLastSpaceReference().getName();
        LocalDocumentReference post = new LocalDocumentReference("Blog", testClassName + "HiddenByAdmin");
        String title = "Visibility post hidden by an admin";

        setup.loginAsSuperAdmin();
        setup.createUserAndLogin(author, PASSWORD);
        new BlogPostFixture(setup, post, title).create();

        setup.loginAsSuperAdmin();
        assertTrue(BlogPostViewPage.gotoPage(post).clickHideBlogPostIcon().isHidden());

        setup.login(author, PASSWORD);
        BlogPostViewPage authorView = BlogPostViewPage.gotoPage(post);
        assertEquals(title, authorView.getDocumentTitle());
        assertTrue(authorView.isHidden());

        setup.forceGuestUser();
        setup.gotoPage(post);
        new LoginPage().assertOnPage();
    }
}
