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
 * The {@code Blog.Migration} page, which migrates the articles of the old blog application ({@code XWiki.ArticleClass}
 * objects) to blog posts and reports the migrated articles.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class BlogMigrationPage extends ViewPage
{
    private static final By CONFIRM_BUTTON = By.cssSelector("#xwikicontent input[name='migrate']");

    private static final By REPORT_ENTRIES = By.cssSelector("#xwikicontent li");

    /**
     * The links of a report entry. The conclusion of the report is displayed on a new line of the last entry: its link
     * is not part of the entry.
     */
    private static final By LINKS =
        By.xpath("./span[contains(@class, 'wikilink') or contains(@class, 'wikicreatelink')]"
            + "[not(preceding-sibling::br)]//a");

    /**
     * An entry of the migration report, about one article.
     */
    public static class ReportEntry
    {
        private final String text;

        private final List<String> linkLabels;

        private final List<String> linkURLs;

        ReportEntry(WebElement element)
        {
            // The conclusion of the report is displayed on a new line of the last entry.
            this.text = element.getText().split("\n")[0];
            List<WebElement> links = element.findElements(LINKS);
            this.linkLabels = links.stream().map(WebElement::getText).collect(Collectors.toList());
            this.linkURLs = links.stream().map(link -> link.getAttribute("href")).collect(Collectors.toList());
        }

        /**
         * @return the text of the entry
         */
        public String getText()
        {
            return this.text;
        }

        /**
         * @return the labels of the links of the entry, in the order they are displayed
         */
        public List<String> getLinkLabels()
        {
            return this.linkLabels;
        }

        /**
         * @return the URLs of the links of the entry, in the order they are displayed
         */
        public List<String> getLinkURLs()
        {
            return this.linkURLs;
        }

        @Override
        public String toString()
        {
            return this.text + ' ' + this.linkLabels + ' ' + this.linkURLs;
        }
    }

    /**
     * Opens the migration page, which asks for a confirmation before migrating.
     *
     * @return the migration page
     */
    public static BlogMigrationPage gotoPage()
    {
        getUtil().gotoPage("Blog", "Migration");
        return new BlogMigrationPage();
    }

    /**
     * Confirms the migration and waits for its report.
     *
     * @return the migration page displaying the report of the migration
     */
    public BlogMigrationPage migrate()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(CONFIRM_BUTTON).click();
        getDriver().waitUntilPageIsReloaded();
        return new BlogMigrationPage();
    }

    /**
     * @return the entries of the migration report, one per article
     */
    public List<ReportEntry> getReportEntries()
    {
        return getDriver().findElementsWithoutWaiting(REPORT_ENTRIES).stream().map(ReportEntry::new)
            .collect(Collectors.toList());
    }
}
