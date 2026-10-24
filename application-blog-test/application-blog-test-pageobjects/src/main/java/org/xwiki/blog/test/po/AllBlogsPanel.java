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
 * The "All Blogs" panel, which lists all the blogs of the wiki.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class AllBlogsPanel extends BaseElement
{
    private static final By PANEL = By.xpath("//div[contains(concat(' ', @class, ' '), ' panel ') "
        + "and contains(concat(' ', @class, ' '), ' AllBlogsPanel ')]");

    private static final By BLOG_LINKS = By.xpath(".//li//a");

    /**
     * @return {@code true} if the panel is displayed on the current page, {@code false} otherwise
     */
    public boolean isDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(PANEL);
    }

    /**
     * @return the titles of the listed blogs, in the order in which they are displayed
     */
    public List<String> getBlogTitles()
    {
        return getContainer().findElements(BLOG_LINKS).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * @param title the title of a listed blog, as returned by {@link #getBlogTitles()}
     * @return the URL of the link to that blog
     */
    public String getBlogURL(String title)
    {
        return getContainer().findElements(BLOG_LINKS).stream().filter(link -> title.equals(link.getText()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("No blog [" + title + "]"))
            .getAttribute("href");
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
