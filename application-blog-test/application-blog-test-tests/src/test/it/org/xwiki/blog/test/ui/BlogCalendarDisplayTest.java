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
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

/**
 * Verify the blog home page when the posts are displayed by week or by month.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogCalendarDisplayTest extends AbstractTest
{
    private static final String POST_TITLE = "Fish & \"Chips\"";

    private static final String BLOG_CLASS = "Blog.BlogClass";

    private static final String DISPLAY_TYPE = "displayType";

    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void weeklyDisplayShowsThePostTitles()
    {
        new BlogPostFixture(getUtil(), new LocalDocumentReference("Blog", getTestClassName()), POST_TITLE).create();

        getUtil().updateObject("Blog", "WebHome", BLOG_CLASS, 0, DISPLAY_TYPE, "weekly");
        try {
            List<String> titles = BlogHomePage.gotoPage().getPostTitles();
            Assert.assertTrue(titles.toString(), titles.contains(POST_TITLE));
        } finally {
            getUtil().updateObject("Blog", "WebHome", BLOG_CLASS, 0, DISPLAY_TYPE, "paginated");
        }
    }
}
