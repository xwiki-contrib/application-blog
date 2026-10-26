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
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.xwiki.test.ui.po.RenamePage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertAll;
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

    private static final String WEB_HOME = "WebHome";

    private static final String PASSWORD = "password";

    private static final String CATEGORY_PARAMETER = "category";

    private static final String FORM_TOKEN = "form_token";

    private static final DocumentReference MANAGE_CATEGORIES =
        new DocumentReference(WIKI, BLOG, "ManageCategories");

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
        // Renaming a category to the name of an existing category is refused, keeping both categories.
        categoriesPage.renameCategoryWithError(source.getName(), target.getName(),
            "Target page already exists, please choose a different name");
        assertTrue(data.exists(source));
        assertEquals("Description of RenameSource",
            data.getProperty(source, BlogTestData.CATEGORY_CLASS, "description"));
        assertEquals("Description of RenameTarget",
            data.getProperty(target, BlogTestData.CATEGORY_CLASS, "description"));

        // The subcategories of a deleted category are moved to its parent.
        categoriesPage = ManageCategoriesPage.gotoPage();
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

    /**
     * Renaming and deleting a category update the blog posts in it, and the actions sent without the forms of the
     * category management page (but with a valid form token) behave as the forms.
     */
    @Test
    @Order(4)
    void renameAndDeleteUpdateThePostsOfTheCategory(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference categoriesHome = new DocumentReference(WIKI, "BlogCategoriesITActions", WEB_HOME);
        DocumentReference gardening = new DocumentReference(WIKI, "BlogCategoriesITActions", "Gardening");
        DocumentReference plants = new DocumentReference(WIKI, "BlogCategoriesITActions", "Plants");
        DocumentReference post = new DocumentReference(WIKI, "BlogCategoriesITActionsPosts", "Gardening post");
        data.createCategoriesHome(categoriesHome);
        createCategory(data, gardening, "BlogCategoriesITActions.WebHome");
        data.createPost(post, "Gardening post", "BlogCategoriesITActions.Gardening");

        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage(categoriesHome);
        categoriesPage.renameCategory(gardening.getName(), plants.getName());
        assertEquals("BlogCategoriesITActions.Plants", data.getPostCategory(post));
        categoriesPage = ManageCategoriesPage.gotoPage(categoriesHome);
        categoriesPage.deleteCategory(plants.getName());
        assertFalse(data.exists(plants));
        assertEquals("", data.getPostCategory(post));

        // When a space is given, only the subcategories and the blog posts of that space are updated.
        String space = "BlogCategoriesITShared";
        DocumentReference cooking = new DocumentReference(WIKI, space, "Cooking");
        DocumentReference baking = new DocumentReference(WIKI, space, "Baking");
        DocumentReference postInSpace = new DocumentReference(WIKI, space, "Cooking post");
        DocumentReference postElsewhere = new DocumentReference(WIKI, "BlogCategoriesITSharedPosts", "Cooking post");
        createCategory(data, cooking, space + ".WebHome");
        createCategory(data, baking, space + ".Cooking");
        data.createPost(postInSpace, "Cooking post", space + ".Cooking");
        data.createPost(postElsewhere, "Other cooking post", space + ".Cooking");
        sendCategoryAction(setup, "xaction", "delete", CATEGORY_PARAMETER, space + ".Cooking", "space", space,
            FORM_TOKEN, setup.getSecretToken());
        assertFalse(data.exists(cooking));
        assertEquals(space + ".WebHome", data.getParent(baking));
        assertEquals("", data.getPostCategory(postInSpace));
        assertEquals(space + ".Cooking", data.getPostCategory(postElsewhere));

        // A category created without the add form is created where requested, and the user is sent back to the
        // category management page.
        sendCategoryAction(setup, "xaction", "create", "newCategoryName", "Travel", "newCategoryParent",
            space + ".WebHome", FORM_TOKEN, setup.getSecretToken());
        assertEquals("xwiki:Blog.ManageCategories", new ManageCategoriesPage().getMetaDataValue("reference"));
        DocumentReference travel = new DocumentReference(WIKI, space, "Travel");
        assertTrue(data.exists(travel));
        assertEquals(space + ".WebHome", data.getParent(travel));
    }

    /**
     * The categories of a space whose name contains an apostrophe are managed as the others.
     */
    @Test
    @Order(5)
    void deleteACategoryOfASpaceWithAnApostrophe(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        String space = "Team's categories";
        DocumentReference projects = new DocumentReference(WIKI, space, "Projects");
        DocumentReference archived = new DocumentReference(WIKI, space, "Archived");
        DocumentReference post = new DocumentReference(WIKI, space, "Kickoff");
        createCategory(data, projects, space + ".WebHome");
        createCategory(data, archived, space + ".Projects");
        data.createPost(post, "Kickoff", space + ".Projects");

        sendCategoryAction(setup, "xaction", "delete", CATEGORY_PARAMETER, space + ".Projects", "space", space,
            FORM_TOKEN, setup.getSecretToken());
        assertFalse(data.exists(projects));
        assertEquals(space + ".WebHome", data.getParent(archived));
        assertEquals("", data.getPostCategory(post));
    }

    @Test
    @Order(6)
    void categoryActionsRequireAValidFormToken(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        String space = "BlogCategoriesITPages";
        DocumentReference page = new DocumentReference(WIKI, space, "Ordinary page");
        DocumentReference otherPage = new DocumentReference(WIKI, space, "Other ordinary page");
        DocumentReference category = new DocumentReference(WIKI, space, "Music");
        DocumentReference post = new DocumentReference(WIKI, space, "Music post");
        data.createPage(page, "Ordinary content", "Ordinary page");
        data.createPage(otherPage, "Ordinary content", "Other ordinary page");
        createCategory(data, category, space + ".WebHome");
        data.createPost(post, "Music post", space + ".Music");

        sendCategoryAction(setup, "xaction", "delete", CATEGORY_PARAMETER, space + ".Ordinary page");
        sendCategoryAction(setup, "xaction", "delete", CATEGORY_PARAMETER, space + ".Music");
        sendCategoryAction(setup, "xaction", "create", "newCategoryName", "Unrequested", "newCategoryParent",
            space + ".WebHome");
        sendCategoryAction(setup, "xaction", "delete", CATEGORY_PARAMETER, space + ".Other ordinary page",
            FORM_TOKEN, setup.getSecretToken());

        assertAll(
            // Without a form token, the page and the category are kept.
            () -> assertTrue(data.exists(page), "the page is kept"),
            () -> assertTrue(data.exists(category), "the category is kept"),
            () -> assertEquals(space + ".Music", data.getPostCategory(post)),
            // Without a form token, nothing is created.
            () -> assertFalse(data.exists(new DocumentReference(WIKI, space, "Unrequested")),
                "no category is created"),
            // Only categories can be deleted from the category management page.
            () -> assertTrue(data.exists(otherPage), "the page that is not a category is kept"));
    }

    @Test
    @Order(7)
    void renameChecksTheRightsOnTheRenamedCategory(TestUtils setup) throws Exception
    {
        String user = "BlogCategoriesITEditor";
        String space = "BlogCategoriesITProtected";
        DocumentReference source = new DocumentReference(WIKI, space, "Drafts");
        DocumentReference post = new DocumentReference(WIKI, "BlogCategoriesITProtectedPosts", "Draft post");
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        setup.createUser(user, PASSWORD, null);
        createCategory(data, source, space + ".WebHome");
        data.createPost(post, "Draft post", space + ".Drafts");
        // The user can edit and delete the category, but can't create pages next to it.
        DocumentReference preferences = new DocumentReference(WIKI, space, "WebPreferences");
        data.createPage(preferences, "", "");
        setup.addObject(preferences, "XWiki.XWikiGlobalRights", "users", "XWiki." + user, "levels", "edit",
            "allow", 0);
        setup.addObject(source, "XWiki.XWikiRights", "users", "XWiki." + user, "levels", "edit,delete", "allow", 1);

        setup.login(user, PASSWORD);
        setup.recacheSecretToken();
        sendCategoryAction(setup, "xaction", "rename", CATEGORY_PARAMETER, space + ".Drafts", "newCategoryName",
            "Published", FORM_TOKEN, setup.getSecretToken());

        // The rename is refused: nothing is changed.
        assertAll(
            () -> assertTrue(data.exists(source), "the category is kept"),
            () -> assertFalse(data.exists(new DocumentReference(WIKI, space, "Published"))),
            () -> assertEquals("Drafts", data.getProperty(source, BlogTestData.CATEGORY_CLASS, "name")),
            () -> assertEquals(space + ".Drafts", data.getPostCategory(post)));
    }

    /**
     * Renaming a category with the standard page rename (not the category management page) updates the blog posts in
     * it.
     */
    @Test
    @Order(8)
    void pageRenameUpdatesThePostsOfTheCategory(TestUtils setup) throws Exception
    {
        String space = "BlogCategoriesITPageRename";
        DocumentReference source = new DocumentReference(WIKI, space, "Hiking");
        DocumentReference post = new DocumentReference(WIKI, "BlogCategoriesITPageRenamePosts", "Hiking post");
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        createCategory(data, source, space + ".WebHome");
        data.createPost(post, "Hiking post", space + ".Hiking");

        renamePage(setup, source, "Trekking");

        assertAll(
            () -> assertTrue(data.exists(new DocumentReference(WIKI, space, "Trekking")), "the category is renamed"),
            () -> assertEquals(space + ".Trekking", data.getPostCategory(post)));
    }

    /**
     * When the user renaming a category can't edit some of its blog posts, only the blog posts they can edit are
     * updated.
     */
    @Test
    @Order(9)
    void pageRenameUpdatesThePostsTheUserCanEdit(TestUtils setup) throws Exception
    {
        String user = "BlogCategoriesITRenamer";
        String space = "BlogCategoriesITRenamed";
        String postsSpace = "BlogCategoriesITRenamedPosts";
        DocumentReference source = new DocumentReference(WIKI, space, "Sailing");
        DocumentReference editablePost = new DocumentReference(WIKI, postsSpace, "Sailing post");
        DocumentReference readOnlyPost = new DocumentReference(WIKI, postsSpace, "Other sailing post");
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        setup.createUser(user, PASSWORD, null);
        createCategory(data, source, space + ".WebHome");
        data.createPost(editablePost, "Sailing post", space + ".Sailing");
        data.createPost(readOnlyPost, "Other sailing post", space + ".Sailing");
        // The user can edit and delete the categories, and can edit only one of the blog posts.
        DocumentReference preferences = new DocumentReference(WIKI, space, "WebPreferences");
        data.createPage(preferences, "", "");
        setup.addObject(preferences, "XWiki.XWikiGlobalRights", "users", "XWiki." + user, "levels", "edit,delete",
            "allow", 1);
        setup.addObject(readOnlyPost, "XWiki.XWikiRights", "users", "XWiki." + user, "levels", "edit", "allow", 0);

        setup.login(user, PASSWORD);
        renamePage(setup, source, "Rowing");

        assertAll(
            () -> assertTrue(data.exists(new DocumentReference(WIKI, space, "Rowing")), "the category is renamed"),
            () -> assertEquals(space + ".Rowing", data.getPostCategory(editablePost)),
            () -> assertEquals(space + ".Sailing", data.getPostCategory(readOnlyPost)));
    }

    /**
     * Renames a page with the standard rename form, in the same space, and waits for the rename to finish.
     *
     * @param page the page to rename
     * @param newName the new title and name of the page
     */
    private static void renamePage(TestUtils setup, DocumentReference page, String newName)
    {
        setup.gotoPage(page, "view", "xpage=rename&step=1");
        RenamePage renamePage = new RenamePage();
        // The page name follows the new title.
        renamePage.getDocumentPicker().setTitle(newName)
            .waitForLocation(Arrays.asList("", page.getLastSpaceReference().getName(), newName));
        renamePage.clickRenameButton().waitUntilFinished();
    }

    /**
     * Opens the category management page with the given request parameters, as a link or a bookmark would.
     *
     * @param parameters the request parameters, as name-value pairs
     */
    private static void sendCategoryAction(TestUtils setup, String... parameters)
    {
        Map<String, String> queryString = new LinkedHashMap<>();
        for (int i = 0; i + 1 < parameters.length; i += 2) {
            queryString.put(parameters[i], parameters[i + 1]);
        }
        setup.gotoPage(MANAGE_CATEGORIES, "view", queryString);
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
