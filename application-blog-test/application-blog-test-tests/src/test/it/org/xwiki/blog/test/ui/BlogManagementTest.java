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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogManagementPage;
import org.xwiki.blog.test.po.BlogPostInlinePage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

/**
 * Verify that a new blog can be created and that its posts are kept separate from the posts of the default blog.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogManagementTest extends AbstractTest
{
    private static final String POST_TITLE = "First post of the new blog";

    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void createBlogAndPublishPostInIt()
    {
        String blogName = getTestClassName();
        LocalDocumentReference blog = new LocalDocumentReference(blogName, "WebHome");
        getUtil().deletePage(blog, true);

        BlogManagementPage managementPage = BlogManagementPage.gotoPage();
        managementPage.setTitle(blogName);
        managementPage.setName(blogName);
        managementPage.setCreateDefaultCategories(true);
        BlogHomePage blogHomePage = managementPage.clickCreate();
        Assert.assertEquals(blogName, blogHomePage.getDocumentTitle());
        Assert.assertEquals(Collections.emptyList(), blogHomePage.getPostTitles());

        // Create a post in the new blog, using one of the default categories created in the blog.
        blogHomePage.getCreateBlogPostPane().setTitle(POST_TITLE);
        BlogPostInlinePage editPage = blogHomePage.getCreateBlogPostPane().clickCreateButton();
        editPage.setContent("Content of the first post of the new blog");
        editPage.setCategories(Collections.singletonList(blogName + ".Categories.Personal"));
        editPage.setPublished(true);
        BlogPostViewPage post = editPage.clickSaveAndView();
        Assert.assertEquals(POST_TITLE, post.getDocumentTitle());
        Assert.assertEquals(Collections.singletonList("Personal"), post.getCategories());
        Assert.assertTrue(post.isPublished());

        // The post is listed in the new blog only.
        List<String> titles = BlogHomePage.gotoPage(blog).getPostTitles();
        Assert.assertEquals(Collections.singletonList(POST_TITLE), titles);
        titles = BlogHomePage.gotoPage().getPostTitles();
        Assert.assertFalse(titles.toString(), titles.contains(POST_TITLE));
    }
}
