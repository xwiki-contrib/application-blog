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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The "Blog configuration" modal, opened from the "More actions" menu of a page, which configures the blog panels
 * displayed in the space of the page.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class BlogPanelsConfigurationModal extends BaseElement
{
    private static final By MODAL = By.id("blogModal");

    private static final String FIELD_PREFIX = "Blog.EnablePanelsConfigurationClass_";

    private static final By PANELS = By.cssSelector("input[type='checkbox'][name^='" + FIELD_PREFIX + "']"
        + "[name$='_panels']");

    private static final By INHERIT_PANELS = By.cssSelector("input[type='checkbox'][name^='" + FIELD_PREFIX + "']"
        + "[name$='_inheritPanels']");

    private static final By SAVE_BUTTON = By.cssSelector("#enableBlogPanels input[type='submit']");

    private static final By ERROR_MESSAGE = By.cssSelector("#enableBlogPanels .errormessage");

    /**
     * Opens the modal from the "More actions" menu of the current page.
     *
     * @param page the current page
     * @return the opened modal
     */
    public static BlogPanelsConfigurationModal open(ViewPage page)
    {
        page.clickMoreActionsSubMenuEntry("tmBlogTrigger");
        getUtil().getDriver().waitUntilElementIsVisible(SAVE_BUTTON);
        return new BlogPanelsConfigurationModal();
    }

    /**
     * Selects the given panels and only them.
     *
     * @param panels the full names of the panel pages to select
     * @return this modal
     */
    public BlogPanelsConfigurationModal setPanels(List<String> panels)
    {
        for (WebElement checkbox : getContainer().findElements(PANELS)) {
            if (checkbox.isSelected() != panels.contains(checkbox.getAttribute("value"))) {
                checkbox.click();
            }
        }
        return this;
    }

    /**
     * @param inheritPanels whether the panels displayed in the parent spaces are kept
     * @return this modal
     */
    public BlogPanelsConfigurationModal setInheritPanels(boolean inheritPanels)
    {
        WebElement checkbox = getContainer().findElement(INHERIT_PANELS);
        if (checkbox.isSelected() != inheritPanels) {
            checkbox.click();
        }
        return this;
    }

    /**
     * @param position {@code before} or {@code after}: where the blog panels are displayed, compared to the kept
     *            panels
     * @return this modal
     */
    public BlogPanelsConfigurationModal setPanelsPosition(String position)
    {
        getContainer().findElement(By.cssSelector("input[type='radio'][name^='" + FIELD_PREFIX + "']"
            + "[name$='_panelsPosition'][value='" + position + "']")).click();
        return this;
    }

    /**
     * Saves the configuration and waits for the page to be reloaded.
     *
     * @return the reloaded page
     */
    public ViewPage save()
    {
        getDriver().addPageNotYetReloadedMarker();
        getContainer().findElement(SAVE_BUTTON).click();
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }

    /**
     * Saves the configuration, expecting it to be refused.
     *
     * @return the error message displayed in the modal
     */
    public String saveWithError()
    {
        getContainer().findElement(SAVE_BUTTON).click();
        getDriver().waitUntilElementIsVisible(ERROR_MESSAGE);
        return getContainer().findElement(ERROR_MESSAGE).getText();
    }

    private WebElement getContainer()
    {
        return getDriver().findElement(MODAL);
    }
}
