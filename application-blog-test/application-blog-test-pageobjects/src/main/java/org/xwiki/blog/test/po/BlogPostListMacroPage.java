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
 * Represents a page displaying calls to the {@code blogpostlist} macro, each wrapped in an element with a known id.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogPostListMacroPage extends ViewPage
{
    /**
     * @param containerId the id of the element wrapping the macro call
     * @return the texts of the links to blog posts displayed by the macro; with the {@code link} layout these are
     *         exactly the titles of the listed posts
     */
    public List<String> getPostLinkTexts(String containerId)
    {
        return getDriver()
            .findElementsWithoutWaiting(By.xpath("//*[@id = '" + containerId + "']//a[contains(@href, '/view/')]"))
            .stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * @param containerId the id of the element wrapping the macro call
     * @param title the title of a blog post
     * @return {@code true} if the macro displays a link to the blog post whose text contains the given title,
     *         {@code false} otherwise
     */
    public boolean isPostDisplayed(String containerId, String title)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//*[@id = '" + containerId + "']//a[contains(normalize-space(.), '" + title + "')]"));
    }
}
