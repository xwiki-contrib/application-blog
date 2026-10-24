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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the display of the blog posts on their page and on the blog home page, whether the posts are displayed by
 * page, by week or by month.
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
class BlogCalendarDisplayIT
{
    /**
     * A title made of characters that have a meaning in HTML, which used to be displayed escaped twice by some
     * displays (see BLOG-252), and of accented characters.
     */
    private static final String POST_TITLE = "Fish & \"Chips\" l'été";

    private static final String BLOG_CLASS = "Blog.BlogClass";

    private static final String DISPLAY_TYPE = "displayType";

    @Test
    @Order(1)
    void postTitlesWithSpecialCharacters(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        LocalDocumentReference post = new LocalDocumentReference("Blog", testClassName);
        new BlogPostFixture(setup, post, POST_TITLE).create();

        // The post page.
        assertEquals(POST_TITLE, BlogPostViewPage.gotoPage(post).getDocumentTitle());

        // The default display of the blog home page, which uses the image layout.
        assertThat(BlogHomePage.gotoPage().getPostTitles(), hasItem(POST_TITLE));

        for (String displayType : Arrays.asList("weekly", "monthly")) {
            setup.updateObject("Blog", "WebHome", BLOG_CLASS, 0, DISPLAY_TYPE, displayType);
            try {
                assertThat(displayType, BlogHomePage.gotoPage().getPostTitles(), hasItem(POST_TITLE));
            } finally {
                // The other tests list the posts of the default blog through its paginated display.
                setup.updateObject("Blog", "WebHome", BLOG_CLASS, 0, DISPLAY_TYPE, "paginated");
            }
        }
    }
}
