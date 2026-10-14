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
import org.xwiki.blog.test.po.BlogPostInlinePage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.blog.test.po.CreateBlogPostPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for blog posts.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The weekly and monthly blog displays use the JodaTime plugin.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.jodatime.JodaTimePlugin"
    },
    extraJARs = {
        // A plugin listed in xwiki.cfg is loaded at startup, before extensions get installed, so its JAR has to be
        // in WEB-INF/lib.
        "org.xwiki.platform:xwiki-platform-jodatime"
    },
    // Resolve the version of the extra JAR from the dependencies of the POM (it's not the version of this module).
    resolveExtraJARs = true
)
class BlogPostIT
{
    /**
     * Tests how a blog post is created and then edited.
     */
    @Test
    @Order(1)
    void createAndEditBlogPost(TestUtils setup, TestReference testReference)
    {
        String testMethodName = testReference.getLastSpaceReference().getName();
        String testClassName = testReference.getLastSpaceReference().getParent().getName();

        // Login as superadmin to create a new user.
        setup.loginAsSuperAdmin();
        // Now login as a simple user to verify normal users can create blog posts. Since the Blog PO consider that
        // WYISWYG is enable, we need to force our user to use the WYSIWYG editor (since there's no
        // XWiki.XWikiPreferences set that would set it in our minimal instance).
        setup.createUserAndLogin(testClassName + "_" + testMethodName, "password", "editor", "Wysiwyg");

        // Create the blog post.
        CreateBlogPostPane createBlogPostPane = BlogHomePage.gotoPage().getCreateBlogPostPane();
        createBlogPostPane.setTitle(testMethodName);
        BlogPostInlinePage blogPostInlinePage = createBlogPostPane.clickCreateButton();

        assertEquals(testMethodName, blogPostInlinePage.getTitle());

        blogPostInlinePage.setTitle("Test blog title");
        blogPostInlinePage.setContent("Test blog content");
        blogPostInlinePage.setCategories(Collections.singletonList("Personal"));
        BlogPostViewPage blogPostViewPage = blogPostInlinePage.clickSaveAndView();

        // Assert the result.
        assertEquals("Test blog title", blogPostViewPage.getDocumentTitle());
        assertEquals("Test blog content", blogPostViewPage.getContent());
        assertEquals(Collections.singletonList("Personal"), blogPostViewPage.getCategories());
        assertFalse(blogPostViewPage.isPublished());

        // Edit the blog post.
        blogPostInlinePage = blogPostViewPage.clickEditBlogPostIcon();

        assertEquals("Test blog title", blogPostInlinePage.getTitle());
        assertEquals("Test blog content", blogPostInlinePage.getContent());
        assertEquals(Collections.singletonList("Personal"), blogPostInlinePage.getCategories());
        assertFalse(blogPostInlinePage.isPublished());

        // Modify the blog post.
        blogPostInlinePage.setTitle("Modified title");
        blogPostInlinePage.setContent("Modified content");
        blogPostInlinePage.setCategories(Arrays.asList("News", "Personal"));
        blogPostInlinePage.setPublished(true);

        // Assert the result.
        blogPostViewPage = blogPostInlinePage.clickSaveAndView();

        assertEquals("Modified title", blogPostViewPage.getDocumentTitle());
        assertEquals("Modified content", blogPostViewPage.getContent());
        assertEquals(Arrays.asList("News", "Personal"), blogPostViewPage.getCategories());
        assertTrue(blogPostViewPage.isPublished());
        assertFalse(blogPostViewPage.isHidden());
    }
}
