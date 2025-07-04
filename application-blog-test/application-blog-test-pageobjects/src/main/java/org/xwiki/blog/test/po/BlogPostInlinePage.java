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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.InlinePage;

/**
 * The "Inline form" edit mode page for a blog post.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class BlogPostInlinePage extends InlinePage
{
    /**
     * The blog post title input field.
     */
    @FindBy(id = "Blog.BlogPostClass_0_title")
    private WebElement titleInput;

    /**
     * The list of available categories to choose from.
     */
    @FindBy(className = "blog-categories-list")
    private WebElement categoryList;

    /**
     * The check box used to hide the blog post from the other users.
     */
    @FindBy(id = "Blog.BlogPostClass_0_hidden")
    private WebElement hiddenCheckBox;

    /**
     * The text area used to edit the blog post content.
     */
    @FindBy(id = "Blog.BlogPostClass_0_content")
    private WebElement contentTextArea;

    /**
     * The text area used to edit the blog post summary.
     */
    @FindBy(id = "Blog.BlogPostClass_0_extract")
    private WebElement summaryTextArea;

    /**
     * Sets the value of the title input field.
     * 
     * @param title the new blog post title
     */
    public void setTitle(String title)
    {
        titleInput.clear();
        titleInput.sendKeys(title);
    }

    /**
     * @return the value of the title input field
     */
    public String getTitle()
    {
        return titleInput.getAttribute("value");
    }

    /**
     * Sets the value of the blog post content field.
     * 
     * @param content the new blog post content
     */
    public void setContent(String content)
    {
        contentTextArea.clear();
        contentTextArea.sendKeys(content);
    }

    @Override
    public String getContent()
    {
        return contentTextArea.getText();
    }

    /**
     * Sets the value of the blog post summary field.
     *
     * @param summary the new blog post summary
     */
    public void setSummary(String summary)
    {
        summaryTextArea.clear();
        summaryTextArea.sendKeys(summary);
    }

    /**
     * @return the summary of the blog post
     */
    public String getSummary()
    {
        return summaryTextArea.getText();
    }

    /**
     * Selects the specified categories.
     * 
     * @param categories the list of categories to select
     */
    public void setCategories(List<String> categories)
    {
        for (String categoryName : categories) {
            String categoryReference = categoryName;
            if (categoryReference.indexOf('.') < 0) {
                categoryReference = "Blog.Categories." + categoryName;
            }
            String categoryXPath =
                "//input[@name = 'Blog.BlogPostClass_0_category' and @value = '" + categoryReference + "']";
            for (WebElement category : categoryList.findElements(By.xpath(categoryXPath))) {
                if (!category.isSelected()) {
                    category.click();
                }
            }
        }
    }

    /**
     * @return the list of selected categories
     */
    public List<String> getCategories()
    {
        List<String> categories = new ArrayList<String>();
        for (WebElement category : categoryList.findElements(By
            .xpath("//input[@name = 'Blog.BlogPostClass_0_category']"))) {
            if (category.isSelected()) {
                categories.add(StringUtils.substringAfter(category.getAttribute("value"), "Blog.Categories."));
            }
        }
        return categories;
    }

    /**
     * @return {@code true} if the blog post has been published, {@code false} otherwise
     */
    public boolean isPublished()
    {
        for (WebElement publishCheckBox : getDriver().findElements(By.id("Blog.BlogPostClass_0_published"))) {
            return publishCheckBox.isSelected();
        }
        // When editing a published blog post the publish check box is not displayed.
        return true;
    }

    /**
     * Publish the blog post after it is saved.
     * 
     * @param published whether to publish the blog post or not
     */
    public void setPublished(boolean published)
    {
        for (WebElement publishCheckBox : getDriver().findElements(By.id("Blog.BlogPostClass_0_published"))) {
            if (publishCheckBox.isSelected() != published) {
                publishCheckBox.click();
            }
        }
    }

    /**
     * @return {@code true} if the blog post is hidden, {@code false} otherwise
     */
    public boolean isHidden()
    {
        return hiddenCheckBox.isSelected();
    }

    /**
     * Controls whether the blog post is visible for the rest of the users.
     * 
     * @param hidden {@code true} to hide the blog post, {@code false} to make it visible for the other the users
     */
    public void setHidden(boolean hidden)
    {
        if (hiddenCheckBox.isSelected() != hidden) {
            hiddenCheckBox.click();
        }
    }

    @Override
    protected BlogPostViewPage createViewPage()
    {
        return new BlogPostViewPage();
    }
}
