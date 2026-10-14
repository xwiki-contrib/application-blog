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
import java.util.Collections;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogManagementPage;
import org.xwiki.blog.test.po.BlogPostInlinePage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.blog.test.po.ManageCategoriesPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.RenamePage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.blog.test.ui.BlogTestData.WIKI;

/**
 * Verify that a new blog can be created and that its posts are kept separate from the posts of the default blog.
 *
 * @version $Id$
 */
@UITest
class BlogManagementIT
{
    private static final String POST_TITLE = "First post of the new blog";

    private static final String WEB_HOME = "WebHome";

    private static final String POSTS_LOCATION = "postsLocation";

    private static final String CATEGORIES_LOCATION = "categoriesLocation";

    private static final String CATEGORIES_SUFFIX = ".Categories";

    @Test
    @Order(1)
    void createBlogAndPublishPostInIt(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String blogName = getTestClassName(testReference);
        LocalDocumentReference blog = new LocalDocumentReference(blogName, "WebHome");

        BlogManagementPage managementPage = BlogManagementPage.gotoPage();
        managementPage.setTitle(blogName);
        managementPage.setName(blogName);
        managementPage.setCreateDefaultCategories(true);
        BlogHomePage blogHomePage = managementPage.clickCreate();
        assertEquals(blogName, blogHomePage.getDocumentTitle());
        assertEquals(Collections.emptyList(), blogHomePage.getPostTitles());

        // Create a post in the new blog, using one of the default categories created in the blog.
        blogHomePage.getCreateBlogPostPane().setTitle(POST_TITLE);
        BlogPostInlinePage editPage = blogHomePage.getCreateBlogPostPane().clickCreateButton();
        editPage.setContent("Content of the first post of the new blog");
        editPage.setCategories(Collections.singletonList(blogName + ".Categories.Personal"));
        editPage.setPublished(true);
        BlogPostViewPage post = editPage.clickSaveAndView();
        assertEquals(POST_TITLE, post.getDocumentTitle());
        assertEquals(Collections.singletonList("Personal"), post.getCategories());
        assertTrue(post.isPublished());

        // The post is listed in the new blog only.
        assertEquals(Collections.singletonList(POST_TITLE), BlogHomePage.gotoPage(blog).getPostTitles());
        assertThat(BlogHomePage.gotoPage().getPostTitles(), not(hasItem(POST_TITLE)));

        // The new blog stores where its posts and its categories are, and its posts are its children.
        BlogTestData data = new BlogTestData(setup);
        DocumentReference blogReference = new DocumentReference(WIKI, blogName, WEB_HOME);
        assertEquals(blogName, data.getProperty(blogReference, BlogTestData.BLOG_CLASS, POSTS_LOCATION));
        assertEquals(blogName + CATEGORIES_SUFFIX,
            data.getProperty(blogReference, BlogTestData.BLOG_CLASS, CATEGORIES_LOCATION));
        assertEquals(blogName + ".WebHome", data.getParent(new DocumentReference(WIKI, blogName, POST_TITLE)));
    }

    @Test
    @Order(2)
    void createBlogWithCategoriesInAnotherLocation(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        String blogName = getTestClassName(testReference) + "OtherCategories";
        String categoriesParent = getTestClassName(testReference) + "Categories";
        String categoriesLocation = categoriesParent + ".Categories";

        BlogManagementPage managementPage = BlogManagementPage.gotoPage();
        managementPage.setTitle(blogName);
        managementPage.setName(blogName);
        managementPage.setCategoriesLocation(categoriesParent, "Categories");
        managementPage.setCreateDefaultCategories(true);
        BlogHomePage blogHomePage = managementPage.clickCreate();

        // The posts of the new blog are categorized with the default categories created in the chosen location.
        blogHomePage.getCreateBlogPostPane().setTitle(POST_TITLE);
        BlogPostInlinePage editPage = blogHomePage.getCreateBlogPostPane().clickCreateButton();
        editPage.setCategories(Collections.singletonList(categoriesLocation + ".News"));
        BlogPostViewPage post = editPage.clickSaveAndView();
        assertEquals(Collections.singletonList("News"), post.getCategories());
    }

    @Test
    @Order(3)
    void moveBlogAndPublishPostInIt(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String blogName = getTestClassName(testReference) + "Moved";
        String newParent = getTestClassName(testReference) + "Parent";
        setup.createPage(new LocalDocumentReference(newParent, "WebHome"), "", newParent);

        BlogManagementPage managementPage = BlogManagementPage.gotoPage();
        managementPage.setTitle(blogName);
        managementPage.setName(blogName);
        RenamePage renamePage = managementPage.clickCreate().rename();
        renamePage.getDocumentPicker().setParent(newParent);
        renamePage.clickRenameButton().waitUntilFinished();

        // The posts of the moved blog are created and listed at its new location.
        LocalDocumentReference movedBlog = new LocalDocumentReference(Arrays.asList(newParent, blogName), "WebHome");
        BlogHomePage blogHomePage = BlogHomePage.gotoPage(movedBlog);
        blogHomePage.getCreateBlogPostPane().setTitle(POST_TITLE);
        BlogPostInlinePage editPage = blogHomePage.getCreateBlogPostPane().clickCreateButton();
        editPage.setPublished(true);
        BlogPostViewPage post = editPage.clickSaveAndView();
        assertEquals(newParent + "." + blogName, post.getMetaDataValue("space"));
        assertEquals(Collections.singletonList(POST_TITLE), BlogHomePage.gotoPage(movedBlog).getPostTitles());

        // The locations stored by the moved blog follow it, and its new posts are its children.
        BlogTestData data = new BlogTestData(setup);
        DocumentReference blogReference = new DocumentReference(WIKI, Arrays.asList(newParent, blogName), WEB_HOME);
        String location = newParent + "." + blogName;
        assertEquals(location, data.getProperty(blogReference, BlogTestData.BLOG_CLASS, POSTS_LOCATION));
        assertEquals(location + CATEGORIES_SUFFIX,
            data.getProperty(blogReference, BlogTestData.BLOG_CLASS, CATEGORIES_LOCATION));
        assertEquals(location + ".WebHome",
            data.getParent(new DocumentReference(WIKI, Arrays.asList(newParent, blogName), POST_TITLE)));
    }

    @Test
    @Order(4)
    void createBlogWithADotInItsNameAndItsCategoriesAtTheTopLevel(TestUtils setup, TestReference testReference)
        throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        // A legacy category page of the Blog space can be gone: deleted by an admin, or moved to Blog.Categories by
        // the category migration when the new page was missing.
        DocumentReference legacyCategory = new DocumentReference(WIKI, "Blog", "Other");
        if (data.exists(legacyCategory)) {
            setup.rest().delete(legacyCategory);
        }
        String parent = getTestClassName(testReference) + "Dotted";

        BlogManagementPage managementPage = BlogManagementPage.gotoPage();
        managementPage.setTitle("Dotted blog");
        managementPage.setParent(parent);
        managementPage.setName("Team.Blog");
        managementPage.setCategoriesLocation("", "TopLevelCategories");
        managementPage.setCreateDefaultCategories(true);
        BlogHomePage blogHomePage = managementPage.clickCreate();
        assertEquals("Dotted blog", blogHomePage.getDocumentTitle());

        // This is a bug: the dot of the name is taken as a space separator, so the blog is created in nested spaces,
        // Team then Blog, instead of in a space named "Team.Blog". See BLOG-282: Creating a blog whose name contains a
        // dot creates it in nested spaces.
        assertFalse(data.exists(new DocumentReference(WIKI, Arrays.asList(parent, "Team.Blog"), WEB_HOME)));
        DocumentReference blogReference = new DocumentReference(WIKI, Arrays.asList(parent, "Team", "Blog"), WEB_HOME);
        assertTrue(data.exists(blogReference));
        assertEquals(parent + ".Team.Blog", data.getProperty(blogReference, BlogTestData.BLOG_CLASS, POSTS_LOCATION));

        // Without a parent, the categories are created at the top level of the wiki, as shown by the form.
        assertEquals("TopLevelCategories",
            data.getProperty(blogReference, BlogTestData.BLOG_CLASS, CATEGORIES_LOCATION));
        // This is a bug: the default categories are copies of the legacy category pages of the Blog space, so a
        // default category whose legacy page is gone is not created (an empty page is created instead). They should be
        // copies of the categories of the default blog, in Blog.Categories: News, Other and Personal. See BLOG-283: New
        // blogs miss the default categories whose legacy page is gone from the Blog space.
        assertEquals(Arrays.asList("News", "Personal"), ManageCategoriesPage
            .gotoPage(new DocumentReference(WIKI, "TopLevelCategories", WEB_HOME)).getCategories());
    }

    private static String getTestClassName(TestReference testReference)
    {
        return testReference.getLastSpaceReference().getParent().getName();
    }
}
