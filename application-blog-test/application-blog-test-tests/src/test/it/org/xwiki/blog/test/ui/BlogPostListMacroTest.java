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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.blog.test.po.BlogPostListMacroPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

/**
 * Verify the filters and the layouts of the {@code blogpostlist} macro.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogPostListMacroTest extends AbstractTest
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

    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void blogPostListFiltersAndLayouts()
    {
        new BlogPostFixture(getUtil(), new LocalDocumentReference("Blog", getTestClassName() + "July"), JULY_TITLE)
            .withPublishDate(new GregorianCalendar(2015, 6, 10).getTime()).withCategory("Blog.Categories.News")
            .create();
        new BlogPostFixture(getUtil(), new LocalDocumentReference("Blog", getTestClassName() + "August"),
            AUGUST_TITLE).withPublishDate(new GregorianCalendar(2015, 7, 20).getTime())
                .withCategory("Blog.Categories.Personal").create();

        StringBuilder content = new StringBuilder();
        appendMacroCall(content, DATE_FILTER, "link", JULY_PARAMETERS);
        appendMacroCall(content, CATEGORY_FILTER, "link",
            "category=\"Blog.Categories.Personal\" fromDate=\"2015-01-01\" toDate=\"2015-12-31\"");
        for (String layout : LAYOUTS) {
            appendMacroCall(content, LAYOUT_PREFIX + layout, layout, JULY_PARAMETERS);
        }
        getUtil().deletePage(getTestClassName(), getTestMethodName());
        getUtil().createPage(getTestClassName(), getTestMethodName(), content.toString(), getTestMethodName());
        BlogPostListMacroPage page = new BlogPostListMacroPage();

        Assert.assertEquals(Collections.singletonList(JULY_TITLE), page.getPostLinkTexts(DATE_FILTER));
        Assert.assertEquals(Collections.singletonList(AUGUST_TITLE), page.getPostLinkTexts(CATEGORY_FILTER));
        for (String layout : LAYOUTS) {
            Assert.assertTrue("The post title is not displayed by the [" + layout + "] layout",
                page.isPostDisplayed(LAYOUT_PREFIX + layout, JULY_TITLE));
        }
    }

    private void appendMacroCall(StringBuilder content, String containerId, String layout, String parameters)
    {
        content.append(String.format("(%% id=\"%s\" %%)(((%n{{blogpostlist blog=\"Blog.WebHome\" layout=\"%s\" %s /}}"
            + "%n)))%n%n", containerId, layout, parameters));
    }
}
