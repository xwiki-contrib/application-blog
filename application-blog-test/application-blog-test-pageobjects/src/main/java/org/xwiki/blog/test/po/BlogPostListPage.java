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
     * Opens a page displayed with the archive sheet for the given month, which lists the posts of the blog or the
     * category of that page published that month.
     *
     * @param page the page displayed with the archive sheet, e.g. a blog or a category page
     * @param year the year of the archived posts
     * @param month the month of the archived posts, from 1 to 12
     * @return the page listing the posts published in the given month
     * @since 9.15.16
     */
    public static BlogPostListPage gotoArchiveOf(EntityReference page, int year, int month)
    {
        getUtil().gotoPage(page, "view", "sheet=Blog.ArchiveSheet&year=" + year + "&month=" + month);
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

    /**
     * @param title the title of one of the listed blog posts
     * @return the path to the blog of the post, as displayed after "Posted in" below the post (the last element is the
     *         blog itself); empty when the blog of the post is not displayed
     * @since 9.15.15
     */
    public List<String> getPostBlogLocation(String title)
    {
        String xpath = "//div[@id = 'xwikicontent']//*[contains(concat(' ', @class, ' '), ' hentry ')]"
            + "[.//*[contains(concat(' ', @class, ' '), ' entry-title ')]//a[. = '" + title + "']]"
            + "//*[contains(concat(' ', @class, ' '), ' blogrefpath ')]//li";
        return getDriver().findElementsWithoutWaiting(By.xpath(xpath)).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * @return the "Blog Categories" panel of the page
     * @since 9.15.15
     */
    public CategoriesPanel getCategoriesPanel()
    {
        return new CategoriesPanel();
    }

    /**
     * @return the "Related Blogs" panel of the page
     * @since 9.15.15
     */
    public RelatedBlogsPanel getRelatedBlogsPanel()
    {
        return new RelatedBlogsPanel();
    }

    /**
     * @return the "Blog Archive" panel of the page
     * @since 9.15.16
     */
    public ArchivePanel getArchivePanel()
    {
        return new ArchivePanel();
    }

    /**
     * @return the "Unpublished Blog Posts" panel of the page
     * @since 9.15.16
     */
    public UnpublishedPanel getUnpublishedPanel()
    {
        return new UnpublishedPanel();
    }

    /**
     * @return the "All Blogs" panel of the page
     * @since 9.15.16
     */
    public AllBlogsPanel getAllBlogsPanel()
    {
        return new AllBlogsPanel();
    }

    /**
     * @return the texts displayed in bold in the content of the page
     * @since 9.15.16
     */
    public List<String> getBoldTexts()
    {
        return getDriver().findElementsWithoutWaiting(By.xpath("//div[@id = 'xwikicontent']//strong")).stream()
            .map(WebElement::getText).collect(Collectors.toList());
    }
}
