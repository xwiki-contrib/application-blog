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
 * The "Unpublished Blog Posts" panel, which lists the unpublished posts of the current user in the blog or the category
 * of the current page.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class UnpublishedPanel extends BaseElement
{
    private static final By PANEL = By.xpath("//div[contains(concat(' ', @class, ' '), ' panel ') "
        + "and contains(concat(' ', @class, ' '), ' UnpublishedPanel ')]");

    /**
     * The link to the page listing all the unpublished posts, which is followed by their number.
     */
    private static final By VIEW_ALL_LINK = By.xpath(".//a[.//*[contains(@class, 'itemCount')]]");

    /**
     * @return {@code true} if the panel is displayed on the current page, {@code false} otherwise
     */
    public boolean isDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(PANEL);
    }

    /**
     * @return the text of the link to the page listing all the unpublished posts, e.g. {@code View all (2)}
     */
    public String getViewAllText()
    {
        return getContainer().findElement(VIEW_ALL_LINK).getText();
    }

    /**
     * @return the URL of the link to the page listing all the unpublished posts
     */
    public String getViewAllURL()
    {
        return getContainer().findElement(VIEW_ALL_LINK).getAttribute("href");
    }

    /**
     * @return the texts displayed in bold in the panel
     */
    public List<String> getBoldTexts()
    {
        return getContainer().findElements(By.xpath(".//strong")).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    private WebElement getContainer()
    {
        return getDriver().findElementWithoutWaiting(PANEL);
    }
}
