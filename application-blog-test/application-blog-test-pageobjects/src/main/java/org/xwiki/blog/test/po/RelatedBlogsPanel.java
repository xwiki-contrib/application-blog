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
 * The "Related Blogs" panel, which lists the blogs sharing the categories of the current blog. The panel is displayed
 * only when there are at least two such blogs.
 *
 * @version $Id$
 * @since 9.15.15
 */
public class RelatedBlogsPanel extends BaseElement
{
    private static final By PANEL = By.xpath("//div[contains(concat(' ', @class, ' '), ' panel ') "
        + "and contains(concat(' ', @class, ' '), ' RelatedBlogsPanel ')]");

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
        return getDriver().findElementWithoutWaiting(PANEL).findElements(By.xpath(".//li//a")).stream()
            .map(WebElement::getText).collect(Collectors.toList());
    }
}
