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
import org.xwiki.test.ui.po.ViewPage;

/**
 * The {@code Blog.BlogPostsMigration} page, which sets the default category to the blog posts that have no category.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class BlogPostsMigrationPage extends ViewPage
{
    private static final By START_LINK = By.cssSelector("#xwikicontent a.button");

    private static final By LISTED_POSTS = By.cssSelector("#xwikicontent li a");

    /**
     * Opens the migration page, which lists the blog posts to migrate and asks for a confirmation.
     *
     * @return the migration page
     */
    public static BlogPostsMigrationPage gotoPage()
    {
        return gotoPage("");
    }

    /**
     * Opens the migration page with the given query string.
     *
     * @param queryString the query string
     * @return the migration page
     */
    public static BlogPostsMigrationPage gotoPage(String queryString)
    {
        getUtil().gotoPage("Blog", "BlogPostsMigration", "view", queryString);
        return new BlogPostsMigrationPage();
    }

    /**
     * @return the full names of the listed blog posts: the posts to migrate, or the migrated posts once the migration
     *         is done
     */
    public List<String> getListedPosts()
    {
        return getDriver().findElementsWithoutWaiting(LISTED_POSTS).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * @return {@code true} if the link that starts the migration is displayed
     */
    public boolean canStart()
    {
        return getDriver().hasElementWithoutWaiting(START_LINK);
    }

    /**
     * Starts the migration and waits for its report.
     *
     * @return the migration page displaying the migrated blog posts
     */
    public BlogPostsMigrationPage start()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(START_LINK).click();
        getDriver().waitUntilPageIsReloaded();
        return new BlogPostsMigrationPage();
    }
}
