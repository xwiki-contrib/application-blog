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

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the {@code Blog.Management} page, used to create new blogs.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogManagementPage extends ViewPage
{
    @FindBy(id = "blogLocationTitle")
    private WebElement titleInput;

    @FindBy(id = "blogLocationName")
    private WebElement nameInput;

    @FindBy(id = "blogLocationParentReference")
    private WebElement parentInput;

    @FindBy(id = "blogCategoriesLocationParentReference")
    private WebElement categoriesParentInput;

    @FindBy(id = "blogCategoriesLocationName")
    private WebElement categoriesNameInput;

    @FindBy(xpath = "//form[@id = 'newBlog']//input[@name = 'createDefaultCategories']")
    private WebElement createDefaultCategoriesCheckBox;

    @FindBy(xpath = "//form[@id = 'newBlog']//input[@type = 'submit']")
    private WebElement createButton;

    /**
     * Opens the blog management page.
     *
     * @return the blog management page
     */
    public static BlogManagementPage gotoPage()
    {
        getUtil().gotoPage("Blog", "Management");
        return new BlogManagementPage();
    }

    /**
     * @param title the title of the new blog
     */
    public void setTitle(String title)
    {
        this.titleInput.clear();
        // Leave the field so that its change event, which computes the page name from the title, is fired now rather
        // than when another field takes the focus.
        this.titleInput.sendKeys(title, Keys.TAB);
    }

    /**
     * @param parent the parent of the page of the new blog, e.g. {@code Path.To}; the top level of the wiki by default
     * @since 9.15.15
     */
    public void setParent(String parent)
    {
        openLocationEdit(this.parentInput);
        this.parentInput.clear();
        this.parentInput.sendKeys(parent);
    }

    /**
     * @param name the name of the page of the new blog, created under the parent set with {@link #setParent(String)}
     */
    public void setName(String name)
    {
        // The name field is disabled while the name is computed from the title.
        openLocationEdit(this.nameInput);
        getDriver().waitUntilCondition(driver -> this.nameInput.isEnabled());
        this.nameInput.clear();
        this.nameInput.sendKeys(name);
    }

    /**
     * Sets where the categories of the new blog are created, which is under the new blog by default.
     *
     * @param parent the parent of the categories page, e.g. {@code Path.To}, empty for the top level of the wiki
     * @param name the name of the categories page
     * @since 9.15.13
     */
    public void setCategoriesLocation(String parent, String name)
    {
        openLocationEdit(this.categoriesParentInput);
        this.categoriesParentInput.clear();
        if (!parent.isEmpty()) {
            this.categoriesParentInput.sendKeys(parent);
        }
        this.categoriesNameInput.clear();
        this.categoriesNameInput.sendKeys(name);
    }

    /**
     * @param createDefaultCategories whether to create the default categories (News, Other, Personal) in the new blog
     */
    public void setCreateDefaultCategories(boolean createDefaultCategories)
    {
        if (this.createDefaultCategoriesCheckBox.isSelected() != createDefaultCategories) {
            this.createDefaultCategoriesCheckBox.click();
        }
    }

    /**
     * Creates the blog.
     *
     * @return the home page of the new blog
     */
    public BlogHomePage clickCreate()
    {
        // The form is submitted only after the location previews are updated, so the click may not load the page.
        getDriver().addPageNotYetReloadedMarker();
        this.createButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new BlogHomePage();
    }

    /**
     * The fields of a location picker are hidden behind its "edit" button when the document tree is available (it is
     * not in every XWiki distribution), so show them if needed.
     *
     * @param field a field of the location picker
     */
    private void openLocationEdit(WebElement field)
    {
        if (!field.isDisplayed()) {
            field.findElement(By.xpath("./ancestor::div[contains(@class, 'location-edit')]"
                + "/preceding-sibling::div[contains(@class, 'breadcrumb-container')]"
                + "//a[contains(@class, 'location-action-edit')]")).click();
            getDriver().waitUntilCondition(driver -> field.isDisplayed());
        }
    }
}
