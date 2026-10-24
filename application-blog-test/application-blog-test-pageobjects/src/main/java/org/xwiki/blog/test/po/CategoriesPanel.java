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
 * The "Blog Categories" panel, displayed next to the pages of a blog whose panels are enabled.
 *
 * @version $Id$
 * @since 9.15.15
 */
public class CategoriesPanel extends BaseElement
{
    private static final By PANEL = By.xpath("//div[contains(concat(' ', @class, ' '), ' panel ') "
        + "and contains(concat(' ', @class, ' '), ' CategoriesPanel ')]");

    /**
     * The links to the categories, which are followed by the number of posts in the category.
     */
    private static final By CATEGORY_LINKS = By.xpath(".//div[contains(@class, 'blog-category-level')]"
        + "//a[not(@title = 'RSS') and .//*[contains(@class, 'itemCount')]]");

    /**
     * The first line of the panel, about all the categories: a link to their RSS feed and a link to their home page.
     */
    private static final By ALL_CATEGORIES = By.xpath(".//div[contains(@class, 'blog-category-level')]");

    private static final By RSS_LINK = By.xpath(".//a[@title = 'RSS']");

    private static final By ALL_LINK = By.xpath(".//a[not(@title = 'RSS')]");

    private static final By MANAGE_LINK = By.xpath(".//*[contains(@class, 'edit-categories')]//a");

    private static final String HREF = "href";

    /**
     * @return {@code true} if the panel is displayed on the current page, {@code false} otherwise
     */
    public boolean isDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(PANEL);
    }

    /**
     * @return the names of the categories listed by the panel, without their number of posts
     */
    public List<String> getCategories()
    {
        return getContainer().findElements(CATEGORY_LINKS).stream()
            .map(link -> link.getText().replaceFirst("\\s*\\(\\d+\\)$", "")).collect(Collectors.toList());
    }

    /**
     * @return the URL of the RSS feed of all the categories listed by the panel
     */
    public String getRSSURL()
    {
        return getAllCategories().findElement(RSS_LINK).getAttribute(HREF);
    }

    /**
     * @return the URL of the page that lists all the categories
     */
    public String getAllCategoriesURL()
    {
        return getAllCategories().findElement(ALL_LINK).getAttribute(HREF);
    }

    /**
     * @return the URL of the page where the categories are managed (displayed to administrators only)
     */
    public String getManageCategoriesURL()
    {
        return getContainer().findElement(MANAGE_LINK).getAttribute(HREF);
    }

    /**
     * @return the texts displayed in bold in the panel
     * @since 9.15.16
     */
    public List<String> getBoldTexts()
    {
        return getContainer().findElements(By.xpath(".//strong")).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    private WebElement getAllCategories()
    {
        return getContainer().findElement(ALL_CATEGORIES);
    }

    private WebElement getContainer()
    {
        return getDriver().findElementWithoutWaiting(PANEL);
    }
}
