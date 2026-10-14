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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

/**
 * Verify the blog home page when the posts are displayed by week or by month.
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
    private static final String POST_TITLE = "Fish & \"Chips\"";

    private static final String BLOG_CLASS = "Blog.BlogClass";

    private static final String DISPLAY_TYPE = "displayType";

    @Test
    @Order(1)
    void weeklyDisplayShowsThePostTitles(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        new BlogPostFixture(setup, new LocalDocumentReference("Blog", testClassName), POST_TITLE).create();

        setup.updateObject("Blog", "WebHome", BLOG_CLASS, 0, DISPLAY_TYPE, "weekly");
        try {
            assertThat(BlogHomePage.gotoPage().getPostTitles(), hasItem(POST_TITLE));
        } finally {
            // The other tests list the posts of the default blog through its paginated display.
            setup.updateObject("Blog", "WebHome", BLOG_CLASS, 0, DISPLAY_TYPE, "paginated");
        }
    }
}
