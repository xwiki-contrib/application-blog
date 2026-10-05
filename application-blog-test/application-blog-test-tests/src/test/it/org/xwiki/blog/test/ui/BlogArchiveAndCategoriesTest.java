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
import org.xwiki.blog.test.po.BlogPostListPage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

/**
 * Verify that the blog posts can be browsed by month, in the blog archive, and by category.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogArchiveAndCategoriesTest extends AbstractTest
{
    /**
     * A year in which no other test publishes posts, so that the archive of its months is known.
     */
    private static final int YEAR = 2014;

    private static final String FEBRUARY_TITLE = "Archived personal post";

    private static final String MAY_TITLE = "Archived news post";

    private static final List<String> CATEGORIES_SPACE = Arrays.asList("Blog", "Categories");

    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void postsAreListedInTheArchiveOfTheirMonthAndInTheirCategory()
    {
        new BlogPostFixture(getUtil(), new LocalDocumentReference("Blog", getTestClassName() + "February"),
            FEBRUARY_TITLE).withPublishDate(new GregorianCalendar(YEAR, 1, 10).getTime())
                .withCategory("Blog.Categories.Personal").create();
        new BlogPostFixture(getUtil(), new LocalDocumentReference("Blog", getTestClassName() + "May"), MAY_TITLE)
            .withPublishDate(new GregorianCalendar(YEAR, 4, 20).getTime()).withCategory("Blog.Categories.News")
            .create();

        // The archive of a month lists only the posts published that month.
        Assert.assertEquals(Collections.singletonList(FEBRUARY_TITLE),
            BlogPostListPage.gotoArchive(YEAR, 2).getPostTitles());
        Assert.assertEquals(Collections.singletonList(MAY_TITLE),
            BlogPostListPage.gotoArchive(YEAR, 5).getPostTitles());

        // A category page lists only the posts of that category.
        List<String> titles =
            BlogPostListPage.gotoCategory(new LocalDocumentReference(CATEGORIES_SPACE, "News")).getPostTitles();
        Assert.assertTrue(titles.toString(), titles.contains(MAY_TITLE));
        Assert.assertFalse(titles.toString(), titles.contains(FEBRUARY_TITLE));

        BlogPostListPage personalCategory =
            BlogPostListPage.gotoCategory(new LocalDocumentReference(CATEGORIES_SPACE, "Personal"));
        titles = personalCategory.getPostTitles();
        Assert.assertTrue(titles.toString(), titles.contains(FEBRUARY_TITLE));
        Assert.assertFalse(titles.toString(), titles.contains(MAY_TITLE));

        // The listed posts lead to the post pages.
        BlogPostViewPage post = personalCategory.clickPost(FEBRUARY_TITLE);
        Assert.assertEquals(FEBRUARY_TITLE, post.getDocumentTitle());
        Assert.assertEquals(Collections.singletonList("Personal"), post.getCategories());
    }
}
