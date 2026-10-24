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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the blog archive, displayed by {@code Blog.ArchiveSheet}: either the tree of the years and months in
 * which posts were published, or the list of the posts published in a given year, grouped by month.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class BlogArchivePage extends ViewPage
{
    private static final String ARCHIVE_SHEET = "sheet=Blog.ArchiveSheet";

    private static final String YEAR_PARAMETER = "year=";

    private static final String CONTENT_XPATH = "//div[@id = 'xwikicontent']";

    private static final By FIRST_LINK = By.xpath("(.//a)[1]");

    private static final String HREF = "href";

    /**
     * Opens the archive page of the default blog, which displays the years and months in which posts were published.
     *
     * @return the archive page
     */
    public static BlogArchivePage gotoPage()
    {
        getUtil().gotoPage("Blog", "Archive");
        return new BlogArchivePage();
    }

    /**
     * Opens the archive page of the default blog for the given year.
     *
     * @param year the year of the listed posts
     * @return the archive page listing the posts published in the given year
     */
    public static BlogArchivePage gotoPage(int year)
    {
        getUtil().gotoPage("Blog", "Archive", "view", YEAR_PARAMETER + year);
        return new BlogArchivePage();
    }

    /**
     * Opens a page displayed with the archive sheet, which displays the years and months in which the posts of the
     * blog or the category of that page were published.
     *
     * @param page the page displayed with the archive sheet, e.g. a blog or a category page
     * @return the archive page
     */
    public static BlogArchivePage gotoArchiveOf(EntityReference page)
    {
        getUtil().gotoPage(page, "view", ARCHIVE_SHEET);
        return new BlogArchivePage();
    }

    /**
     * Opens a page displayed with the archive sheet for the given year.
     *
     * @param page the page displayed with the archive sheet, e.g. a blog or a category page
     * @param year the year of the listed posts
     * @return the archive page listing the posts published in the given year
     */
    public static BlogArchivePage gotoArchiveOf(EntityReference page, int year)
    {
        getUtil().gotoPage(page, "view", ARCHIVE_SHEET + '&' + YEAR_PARAMETER + year);
        return new BlogArchivePage();
    }

    /**
     * @return the years in which posts were published, as displayed in the archive tree, with the number of posts,
     *         e.g. {@code 2011 (3)}
     */
    public List<String> getYears()
    {
        return getTree().keySet().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * @param year a year of the archive tree, as returned by {@link #getYears()}
     * @return the URL of the link of that year
     */
    public String getYearURL(String year)
    {
        return getYearEntry(year).getKey().getAttribute(HREF);
    }

    /**
     * @param year a year of the archive tree, as returned by {@link #getYears()}
     * @return the months of that year in which posts were published, with the number of posts, e.g.
     *         {@code February (2)}
     */
    public List<String> getMonths(String year)
    {
        return getYearEntry(year).getValue().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * @param year a year of the archive tree, as returned by {@link #getYears()}
     * @param month a month of that year, as returned by {@link #getMonths(String)}
     * @return the URL of the link of that month
     */
    public String getMonthURL(String year, String month)
    {
        return getYearEntry(year).getValue().stream().filter(link -> month.equals(link.getText())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No month [" + month + "] in year [" + year + "]"))
            .getAttribute(HREF);
    }

    /**
     * @return the month headings of the archive of a year, with the number of posts, e.g. {@code February (2)}
     */
    public List<String> getMonthHeadings()
    {
        return getMonthHeadingElements().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * @param month a month heading of the archive of a year, as returned by {@link #getMonthHeadings()}
     * @return the URL of the link of that month heading
     */
    public String getMonthHeadingURL(String month)
    {
        return getMonthHeading(month).findElement(FIRST_LINK).getAttribute(HREF);
    }

    /**
     * @param month a month heading of the archive of a year, as returned by {@link #getMonthHeadings()}
     * @return the posts listed below that month heading, as displayed: the title of the post, followed by
     *         {@code (unpublished)} or {@code (hidden)} for the posts that only their author can see
     */
    public List<String> getPosts(String month)
    {
        List<String> posts = new ArrayList<>();
        boolean inMonth = false;
        for (WebElement element : getDriver().findElementsWithoutWaiting(
            By.xpath(CONTENT_XPATH + "//*[(self::h3 and .//a) or (self::li and not(ancestor::li))]")))
        {
            if ("h3".equals(element.getTagName())) {
                inMonth = month.equals(element.getText());
            } else if (inMonth) {
                posts.add(element.getText());
            }
        }
        return posts;
    }

    /**
     * @param title the title of one of the listed posts, as displayed
     * @return the URL of the link to that post
     */
    public String getPostURL(String title)
    {
        return findLink(CONTENT_XPATH + "//li//a", title)
            .orElseThrow(() -> new IllegalArgumentException("No post [" + title + "]")).getAttribute(HREF);
    }

    /**
     * @return the texts displayed in bold in the archive
     */
    public List<String> getBoldTexts()
    {
        return getDriver().findElementsWithoutWaiting(By.xpath(CONTENT_XPATH + "//strong")).stream()
            .map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * @return the links of the archive tree, in the order in which they are displayed: the links of the years (first
     *         level of the tree), each with the links of its months (second level of the tree)
     */
    private Map<WebElement, List<WebElement>> getTree()
    {
        Map<WebElement, List<WebElement>> tree = new LinkedHashMap<>();
        List<WebElement> months = null;
        for (WebElement link : getDriver().findElementsWithoutWaiting(
            By.xpath(CONTENT_XPATH + "//li//a[count(ancestor::ul) <= 2]")))
        {
            if (getDriver().findElementsWithoutWaiting(link, By.xpath("ancestor::ul")).size() == 1) {
                months = new ArrayList<>();
                tree.put(link, months);
            } else if (months != null) {
                months.add(link);
            }
        }
        return tree;
    }

    private Map.Entry<WebElement, List<WebElement>> getYearEntry(String year)
    {
        return getTree().entrySet().stream().filter(entry -> year.equals(entry.getKey().getText())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No year [" + year + "]"));
    }

    private List<WebElement> getMonthHeadingElements()
    {
        return getDriver().findElementsWithoutWaiting(By.xpath(CONTENT_XPATH + "//h3[.//a]"));
    }

    private WebElement getMonthHeading(String month)
    {
        return getMonthHeadingElements().stream().filter(heading -> month.equals(heading.getText())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No month heading [" + month + "]"));
    }

    private Optional<WebElement> findLink(String xpath, String text)
    {
        return getDriver().findElementsWithoutWaiting(By.xpath(xpath)).stream()
            .filter(link -> text.equals(link.getText())).findFirst();
    }
}
