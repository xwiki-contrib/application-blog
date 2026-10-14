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
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.blog.test.po.BlogPostListMacroPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the filters and the layouts of the {@code blogpostlist} macro.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The date filters of the macro use the JodaTime plugin.
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
class BlogPostListMacroIT
{
    /**
     * A title with an ampersand, which used to be displayed escaped twice by some layouts (see BLOG-252 and BLOG-253).
     */
    private static final String JULY_TITLE = "Fish & Chips";

    private static final String AUGUST_TITLE = "Bread and butter";

    private static final List<String> LAYOUTS = Arrays.asList("full", "link", "compact", "cards", "image");

    private static final String DATE_FILTER = "dateFilter";

    private static final String CATEGORY_FILTER = "categoryFilter";

    private static final String LAYOUT_PREFIX = "layout-";

    /**
     * Restricts the listed posts to the July 2015 posts, i.e. to the post created by this test with that date.
     */
    private static final String JULY_PARAMETERS = "fromDate=\"2015-07-01\" toDate=\"2015-07-31\"";

    @Test
    @Order(1)
    void blogPostListFiltersAndLayouts(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        String testClassName = testReference.getLastSpaceReference().getParent().getName();

        new BlogPostFixture(setup, new LocalDocumentReference("Blog", testClassName + "July"), JULY_TITLE)
            .withPublishDate(new GregorianCalendar(2015, 6, 10).getTime()).withCategory("Blog.Categories.News")
            .create();
        new BlogPostFixture(setup, new LocalDocumentReference("Blog", testClassName + "August"), AUGUST_TITLE)
            .withPublishDate(new GregorianCalendar(2015, 7, 20).getTime()).withCategory("Blog.Categories.Personal")
            .create();

        StringBuilder content = new StringBuilder();
        appendMacroCall(content, DATE_FILTER, "link", JULY_PARAMETERS);
        appendMacroCall(content, CATEGORY_FILTER, "link",
            "category=\"Blog.Categories.Personal\" fromDate=\"2015-01-01\" toDate=\"2015-12-31\"");
        for (String layout : LAYOUTS) {
            appendMacroCall(content, LAYOUT_PREFIX + layout, layout, JULY_PARAMETERS);
        }
        setup.createPage(testReference, content.toString(), testReference.getLastSpaceReference().getName());
        BlogPostListMacroPage page = new BlogPostListMacroPage();

        assertEquals(Collections.singletonList(JULY_TITLE), page.getPostLinkTexts(DATE_FILTER));
        assertEquals(Collections.singletonList(AUGUST_TITLE), page.getPostLinkTexts(CATEGORY_FILTER));
        for (String layout : LAYOUTS) {
            assertTrue(page.isPostDisplayed(LAYOUT_PREFIX + layout, JULY_TITLE),
                "The post title is not displayed by the [" + layout + "] layout");
        }
    }

    private void appendMacroCall(StringBuilder content, String containerId, String layout, String parameters)
    {
        content.append(String.format("(%% id=\"%s\" %%)(((%n{{blogpostlist blog=\"Blog.WebHome\" layout=\"%s\" %s /}}"
            + "%n)))%n%n", containerId, layout, parameters));
    }
}
