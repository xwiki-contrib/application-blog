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
        this.titleInput.sendKeys(title);
    }

    /**
     * @param name the name of the page of the new blog, created at the top level of the wiki
     */
    public void setName(String name)
    {
        this.nameInput.clear();
        this.nameInput.sendKeys(name);
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
        this.createButton.click();
        return new BlogHomePage();
    }
}
