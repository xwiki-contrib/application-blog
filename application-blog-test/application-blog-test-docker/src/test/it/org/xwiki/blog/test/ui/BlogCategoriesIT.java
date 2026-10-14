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
package org.xwiki.blog.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostListPage;
import org.xwiki.blog.test.po.CategoriesPanel;
import org.xwiki.blog.test.po.CreateBlogPostPane;
import org.xwiki.blog.test.po.ManageCategoriesPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.blog.test.ui.BlogTestData.WIKI;

/**
 * Test Blog categories. Tested features: add, rename, delete, and the legacy categories of the default blog.
 *
 * @version $Id$
 */
@UITest
class BlogCategoriesIT
{
    private static final String BLOG = "Blog";

    private static final String CATEGORIES_HOME = "Blog.Categories.WebHome";

    /**
     * We make sure to have spaces and special chars to ensure categories can be named with any char.
     */
    private static final String CATEGORY = "The \"Do\"s & Don'ts";

    private static final String CATEGORY_RENAME = "New \"categor'y\"";

    private static final String CATEGORY_RENAME_2 =
        "A category with [[//wiki// syntax]] # || onclick=\"alert('fail');return false;\"";

    @Test
    @Order(1)
    void categoryAddRenameRemove(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        categoryAdd(CATEGORY);
        // A category created without choosing a parent is a child of the categories home page.
        assertEquals(CATEGORIES_HOME, new BlogTestData(setup).getParent(getCategory(CATEGORY)));
        categoryRename(CATEGORY, CATEGORY_RENAME);
        categoryRename(CATEGORY_RENAME, CATEGORY_RENAME_2);
        categoryRemove(CATEGORY_RENAME_2);
    }

    @Test
    @Order(2)
    void renameOntoAnExistingCategoryAndDeleteCategoriesWithSubcategories(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference source = getCategory("RenameSource");
        DocumentReference target = getCategory("RenameTarget");
        DocumentReference parent = getCategory("DeletedParent");
        DocumentReference child = getCategory("KeptChild");
        DocumentReference orphanParent = getCategory("OrphanParent");
        DocumentReference orphanChild = getCategory("OrphanChild");
        createCategory(data, source, CATEGORIES_HOME);
        createCategory(data, target, CATEGORIES_HOME);
        createCategory(data, parent, CATEGORIES_HOME);
        createCategory(data, child, "Blog.Categories.DeletedParent");
        createCategory(data, orphanParent, "");
        createCategory(data, orphanChild, "Blog.Categories.OrphanParent");

        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        categoriesPage.renameCategory(source.getName(), target.getName());
        // This is a bug: the renamed category is lost, the existing one being kept as is. The rename should be
        // refused, keeping both categories. See BLOG-271: Renaming a category to the name of an existing category is
        // not refused.
        assertFalse(data.exists(source));
        assertEquals("Description of RenameTarget",
            data.getProperty(target, BlogTestData.CATEGORY_CLASS, "description"));

        // The subcategories of a deleted category are moved to its parent.
        categoriesPage.deleteCategory(parent.getName());
        assertFalse(data.exists(parent));
        assertEquals(CATEGORIES_HOME, data.getParent(child));
        categoriesPage.deleteCategory(orphanParent.getName());
        // This is a bug: the subcategories of a deleted category without parent get an invalid parent. They should be
        // moved to the categories home page, Blog.Categories.WebHome. See BLOG-272: Deleting a category without a
        // parent sets an invalid parent on its subcategories.
        assertEquals("{Blog.Categories.WebHome}", data.getParent(orphanChild));
    }

    /**
     * When blog posts are still assigned to categories of the legacy {@code Blog} space (the categories have not been
     * migrated to {@code Blog.Categories} yet), the default blog and the blogs sharing its categories use the legacy
     * categories.
     */
    @Test
    @Order(3)
    void legacyCategories(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference inSpaceBlog = new DocumentReference(WIKI, "BlogCategoriesITBlog", "Blog");
        DocumentReference inSpacePost = new DocumentReference(WIKI, "BlogCategoriesITBlog", "Uncategorized post");
        DocumentReference legacyPost = new DocumentReference(WIKI, BLOG, "BlogCategoriesITLegacyPost");
        DocumentReference added = new DocumentReference(WIKI, BLOG, "LegacyAdded");
        DocumentReference renamed = new DocumentReference(WIKI, BLOG, "LegacyRenamed");
        DocumentReference defaultBlog = new DocumentReference(WIKI, BLOG, "WebHome");
        DocumentReference mainPage = new DocumentReference(WIKI, "Main", "WebHome");
        data.createBlog(inSpaceBlog, "In-space blog", "", "", true);
        data.createBlog(new DocumentReference(WIKI, "BlogCategoriesITStored", "WebHome"), "Stored", "",
            "BlogCategoriesITStored.Categories", false);
        data.createPost(legacyPost, "Legacy post", "Blog.Personal");
        try {
            assertEquals("Blog.WebHome|Blog|Blog|Blog.WebHome", data.resolve(BLOG));
            assertEquals("BlogCategoriesITBlog.Blog|BlogCategoriesITBlog|Blog|Blog.WebHome",
                data.resolve("BlogCategoriesITBlog"));
            // The categories location stored by a blog is kept.
            assertEquals("BlogCategoriesITStored.WebHome|BlogCategoriesITStored|BlogCategoriesITStored.Categories|"
                + "Blog.WebHome", data.resolve("BlogCategoriesITStored"));

            BlogHomePage home = BlogHomePage.gotoPage();
            CategoriesPanel panel = home.getCategoriesPanel();
            assertThat(panel.getCategories(), hasItems("News", "Personal"));
            assertEquals(BlogTestData.getViewURL(setup, defaultBlog, "xpage=plain&sheet=Blog.BlogRss"),
                panel.getRSSURL());
            assertEquals(BlogTestData.getViewURL(setup, defaultBlog, ""), panel.getAllCategoriesURL());
            assertEquals(BlogTestData.getViewURL(setup, new DocumentReference(WIKI, BLOG, "ManageCategories"), ""),
                panel.getManageCategoriesURL());
            // The blogs sharing the legacy categories of the default blog.
            assertThat(home.getRelatedBlogsPanel().getBlogTitles(), hasItems(BLOG, "In-space blog"));
            // Same on a legacy category page.
            panel = BlogPostListPage.gotoCategory(new DocumentReference(WIKI, BLOG, "News")).getCategoriesPanel();
            assertEquals(BlogTestData.getViewURL(setup, defaultBlog, "xpage=plain&sheet=Blog.BlogRss"),
                panel.getRSSURL());
            assertEquals(BlogTestData.getViewURL(setup, new DocumentReference(WIKI, BLOG, "ManageCategories"), ""),
                panel.getManageCategoriesURL());

            // The legacy categories are managed in the Blog space, under the legacy categories home page.
            // A post saved without category gets the legacy categories home page.
            CreateBlogPostPane createPane = BlogHomePage.gotoPage(inSpaceBlog).getCreateBlogPostPane();
            createPane.setTitle(inSpacePost.getName());
            createPane.clickCreateButton().clickSaveAndViewWithDefaultCategory();
            assertEquals("Blog.Categories", data.getPostCategory(inSpacePost));

            ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
            assertThat(categoriesPage.getCategories(), hasItems("News", "Personal"));
            categoriesPage.clickAddCategory();
            categoriesPage.addCategory(added.getName());
            assertTrue(data.exists(added));
            assertEquals("Blog.Categories", data.getParent(added));
            categoriesPage = ManageCategoriesPage.gotoPage();
            categoriesPage.renameCategory(added.getName(), renamed.getName());
            assertFalse(data.exists(added));
            assertTrue(data.exists(renamed));

            // The feeds are about the legacy categories.
            assertEquals("Blog/News", data.getFeedCategory(new DocumentReference(WIKI, BLOG, "News"), ""));
            assertEquals("Blog/Categories", data.getFeedCategory(mainPage, ""));
        } finally {
            for (DocumentReference page : Arrays.asList(legacyPost, inSpacePost, added, renamed)) {
                if (data.exists(page)) {
                    setup.rest().delete(page);
                }
            }
        }

        // Without blog posts assigned to legacy categories, the categories of the default blog are back in
        // Blog.Categories.
        assertEquals("Blog.WebHome|Blog|Blog.Categories|Blog.WebHome", data.resolve(BLOG));
    }

    private static DocumentReference getCategory(String name)
    {
        return new DocumentReference(WIKI, Arrays.asList(BLOG, "Categories"), name);
    }

    private static void createCategory(BlogTestData data, DocumentReference category, String parent)
        throws Exception
    {
        data.createCategory(category, category.getName(), parent);
    }

    /**
     * Adds a new category and checks for success.
     *
     * @param name the name of the category to add
     */
    private void categoryAdd(String name)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        assertFalse(categoriesPage.isCategoryPresent(name));

        categoriesPage.clickAddCategory();
        categoriesPage.addCategory(name);
        assertTrue(categoriesPage.isCategoryPresent(name));
    }

    /**
     * Renames a category and checks for success.
     *
     * @param fromName source name, must exist
     * @param toName target name, must not exist
     */
    private void categoryRename(String fromName, String toName)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        assertTrue(categoriesPage.isCategoryPresent(fromName));
        assertFalse(categoriesPage.isCategoryPresent(toName));

        categoriesPage.renameCategory(fromName, toName);
        assertFalse(categoriesPage.isCategoryPresent(fromName));
        assertTrue(categoriesPage.isCategoryPresent(toName));
    }

    /**
     * Removes a category and checks for success.
     *
     * @param name category name, must exist
     */
    private void categoryRemove(String name)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        assertTrue(categoriesPage.isCategoryPresent(name));

        categoriesPage.deleteCategory(name);
        assertFalse(categoriesPage.isCategoryPresent(name));
    }
}
