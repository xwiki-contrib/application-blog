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
package org.xwiki.blog.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents a page that lists blog posts in its content: a blog home page, the blog archive, a category page or the
 * unpublished posts page.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogPostListPage extends ViewPage
{
    private static final String POST_TITLE_LINKS_XPATH = "//div[@id = 'xwikicontent']"
        + "//*[contains(concat(' ', @class, ' '), ' hentry ')]"
        + "//*[contains(concat(' ', @class, ' '), ' entry-title ')]//a";

    /**
     * Opens the archive of the default blog for the given month.
     *
     * @param year the year of the archived posts
     * @param month the month of the archived posts, from 1 to 12
     * @return the page listing the posts published in the given month
     */
    public static BlogPostListPage gotoArchive(int year, int month)
    {
        getUtil().gotoPage("Blog", "Archive", "view", "year=" + year + "&month=" + month);
        return new BlogPostListPage();
    }

    /**
     * Opens a category page, which lists the posts of that category.
     *
     * @param category the reference of the category page
     * @return the page listing the posts of the category
     */
    public static BlogPostListPage gotoCategory(EntityReference category)
    {
        getUtil().gotoPage(category);
        return new BlogPostListPage();
    }

    /**
     * Opens the page listing the posts of the current user that are not published yet.
     *
     * @return the page listing the unpublished posts
     */
    public static BlogPostListPage gotoUnpublished()
    {
        getUtil().gotoPage("Blog", "Unpublished");
        return new BlogPostListPage();
    }

    /**
     * @return the titles of the listed blog posts, in the order in which they are displayed
     */
    public List<String> getPostTitles()
    {
        return getDriver().findElementsWithoutWaiting(By.xpath(POST_TITLE_LINKS_XPATH)).stream()
            .map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * Opens one of the listed blog posts.
     *
     * @param title the title of the blog post to open
     * @return the opened blog post
     */
    public BlogPostViewPage clickPost(String title)
    {
        getDriver().findElementWithoutWaiting(By.xpath(POST_TITLE_LINKS_XPATH + "[. = '" + title + "']")).click();
        return new BlogPostViewPage();
    }
}
