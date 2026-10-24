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
import org.xwiki.test.ui.po.BaseElement;

/**
 * The "Blog Archive" panel, which lists the years in which the posts of the blog or the category of the current page
 * were published and, for the current year, the months.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class ArchivePanel extends BaseElement
{
    private static final By PANEL = By.xpath("//div[contains(concat(' ', @class, ' '), ' panel ') "
        + "and contains(concat(' ', @class, ' '), ' ArchivePanel ')]");

    private static final By YEARS = By.xpath(".//li[not(ancestor::li)]");

    private static final By YEAR_LINK = By.xpath("(.//a)[1]");

    private static final By MONTH_LINKS = By.xpath(".//li//a");

    private static final String HREF = "href";

    /**
     * @return {@code true} if the panel is displayed on the current page, {@code false} otherwise
     */
    public boolean isDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(PANEL);
    }

    /**
     * @return the years in which posts were published, with the number of posts, e.g. {@code 2011 (3)}
     */
    public List<String> getYears()
    {
        return getYearItems().stream().map(year -> year.findElement(YEAR_LINK).getText())
            .collect(Collectors.toList());
    }

    /**
     * @param year a year listed by the panel, as returned by {@link #getYears()}
     * @return the URL of the link of that year
     */
    public String getYearURL(String year)
    {
        return getYearItem(year).findElement(YEAR_LINK).getAttribute(HREF);
    }

    /**
     * @param year a year listed by the panel, as returned by {@link #getYears()}
     * @return the months of that year in which posts were published, with the number of posts, e.g.
     *         {@code February (2)}; they are listed for the current year only
     */
    public List<String> getMonths(String year)
    {
        return getYearItem(year).findElements(MONTH_LINKS).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * @param year a year listed by the panel, as returned by {@link #getYears()}
     * @param month a month of that year, as returned by {@link #getMonths(String)}
     * @return the URL of the link of that month
     */
    public String getMonthURL(String year, String month)
    {
        return getYearItem(year).findElements(MONTH_LINKS).stream().filter(link -> month.equals(link.getText()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("No month [" + month + "] in [" + year + "]"))
            .getAttribute(HREF);
    }

    /**
     * @return the texts displayed in bold in the panel
     */
    public List<String> getBoldTexts()
    {
        return getContainer().findElements(By.xpath(".//strong")).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    private List<WebElement> getYearItems()
    {
        return getContainer().findElements(YEARS);
    }

    private WebElement getYearItem(String year)
    {
        return getYearItems().stream().filter(item -> year.equals(item.findElement(YEAR_LINK).getText())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No year [" + year + "]"));
    }

    private WebElement getContainer()
    {
        return getDriver().findElementWithoutWaiting(PANEL);
    }
}
