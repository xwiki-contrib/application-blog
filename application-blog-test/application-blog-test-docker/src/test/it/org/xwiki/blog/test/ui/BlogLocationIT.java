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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostInlinePage;
import org.xwiki.blog.test.po.BlogPostListMacroPage;
import org.xwiki.blog.test.po.BlogPostListPage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.blog.test.po.CategoriesPanel;
import org.xwiki.blog.test.po.CreateBlogPostPane;
import org.xwiki.blog.test.po.ManageCategoriesPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.blog.test.ui.BlogTestData.WIKI;

/**
 * Verify which blog a page belongs to, where the posts and the categories of a blog live, and what the pages, panels
 * and feeds of a blog show because of it, for the kinds of blogs other than the default one:
 * <ul>
 * <li>B: a whole-space blog ({@code BlogLocationB.WebHome}) storing its own space and {@code BlogLocationB.Categories},
 * as {@code Blog.Management} does;</li>
 * <li>C: a whole-space blog whose posts and categories live in other spaces;</li>
 * <li>D: an in-space blog ({@code BlogLocationD.Blog}, not a {@code WebHome}) storing no location;</li>
 * <li>E: a nested blog whose space name contains a dot, {@code ]]} and {@code ~}, storing no location;</li>
 * <li>F: a blog page that is neither the {@code WebHome} nor the {@code Blog} page of its space, sharing the categories
 * of the default blog ({@code Blog.Categories}) explicitly;</li>
 * <li>G: a whole-space blog sharing the categories of the default blog explicitly too.</li>
 * </ul>
 *
 * @version $Id$
 */
@UITest
class BlogLocationIT
{
    private static final String B = "BlogLocationB";

    private static final String B_CATEGORIES = B + ".Categories";

    private static final String C = "BlogLocationC";

    private static final String C_POSTS = "BlogLocationCPosts";

    private static final String C_CATEGORIES = "BlogLocationCCategories";

    private static final String D = "BlogLocationD";

    /**
     * The space of blog E, as a string reference.
     */
    private static final String E = "BlogLocationE.Child\\.Space]] [[~";

    private static final String F = "BlogLocationF";

    private static final String G = "BlogLocationG";

    private static final String DEFAULT_BLOG = "Blog.WebHome";

    private static final String DEFAULT_CATEGORIES = "Blog.Categories";

    private static final String WEB_HOME = "WebHome";

    private static final SpaceReference E_SPACE =
        new SpaceReference(WIKI, Arrays.asList("BlogLocationE", "Child.Space]] [[~"));

    private static final DocumentReference BLOG_B = new DocumentReference(WIKI, B, WEB_HOME);

    private static final DocumentReference BLOG_C = new DocumentReference(WIKI, C, WEB_HOME);

    private static final DocumentReference BLOG_D = new DocumentReference(WIKI, D, "Blog");

    private static final DocumentReference BLOG_E = new DocumentReference(WEB_HOME, E_SPACE);

    private static final DocumentReference CATEGORIES_HOME_B =
        new DocumentReference(WIKI, Arrays.asList(B, "Categories"), WEB_HOME);

    private static final DocumentReference CATEGORIES_HOME_C = new DocumentReference(WIKI, C_CATEGORIES, WEB_HOME);

    private static final DocumentReference DEFAULT_CATEGORIES_HOME =
        new DocumentReference(WIKI, Arrays.asList("Blog", "Categories"), WEB_HOME);

    private static final DocumentReference SPORT = new DocumentReference(WIKI, Arrays.asList(B, "Categories"), "Sport");

    private static final String SPORT_POST_TITLE = "Sport post";

    private static final List<String> BLOG_TITLES = Arrays.asList("B", "C", "D", "E");

    private static final String FEED_QUERY_STRING = "xpage=plain&sheet=Blog.CategoryRss";

    private static final String MANAGE_QUERY_STRING = "action=manage";

    @Test
    @Order(1)
    void whichBlogAPageBelongsTo(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);

        data.createBlog(BLOG_B, "Blog B", B, B_CATEGORIES, true);
        data.createCategoriesHome(CATEGORIES_HOME_B);
        data.createCategory(SPORT, "Sport", B_CATEGORIES + ".WebHome");
        data.createPost(new DocumentReference(WIKI, B, "SportPost"), SPORT_POST_TITLE, B_CATEGORIES + ".Sport");
        data.enablePanels(BLOG_B.getLastSpaceReference());
        data.createBlog(BLOG_C, "Blog C", C_POSTS, C_CATEGORIES, true);
        data.createCategoriesHome(CATEGORIES_HOME_C);
        data.createCategory(new DocumentReference(WIKI, C_CATEGORIES, "Chess"), "Chess", C_CATEGORIES + ".WebHome");
        data.enablePanels(new SpaceReference(WIKI, C_POSTS));
        data.createBlog(BLOG_D, "Blog D", "", "", true);
        data.enablePanels(BLOG_D.getLastSpaceReference());
        data.createBlog(BLOG_E, "Blog E", "", "", true);
        data.createBlog(new DocumentReference(WIKI, F, "MyBlog"), "Blog F", "", DEFAULT_CATEGORIES, false);
        data.createBlog(new DocumentReference(WIKI, G, WEB_HOME), "Blog G", G, DEFAULT_CATEGORIES, false);
        // A space with both a WebHome blog and a Blog blog.
        data.createBlog(new DocumentReference(WIKI, "BlogLocationH", "Blog"), "Blog H (Blog)", "", "", false);
        data.createBlog(new DocumentReference(WIKI, "BlogLocationH", WEB_HOME), "Blog H", "", "", false);
        // A space with two blog pages, the Blog page coming after the other one in alphabetical order.
        data.createBlog(new DocumentReference(WIKI, "BlogLocationI", "Alpha"), "Blog I (Alpha)", "", "", false);
        data.createBlog(new DocumentReference(WIKI, "BlogLocationI", "Blog"), "Blog I", "", "", false);
        // A space showing the panels of blog B, although it has a blog of its own.
        data.createBlog(new DocumentReference(WIKI, "BlogLocationOverride", "Blog"), "Overridden", "", "", false);
        data.setPanelsBlog(new SpaceReference(WIKI, "BlogLocationOverride"), B + ".WebHome");
        data.setPanelsBlog(new SpaceReference(WIKI, "BlogLocationElsewhere"), B + ".WebHome");

        // Each line is: <blog of the space>|<posts location>|<categories location>|<blog owning the space when it is
        // a categories space>.
        // The default blog.
        assertEquals("Blog.WebHome|Blog|Blog.Categories|Blog.WebHome", data.resolve("Blog"));
        // B: a whole-space blog, found through its WebHome, with its stored locations.
        assertEquals("BlogLocationB.WebHome|BlogLocationB|BlogLocationB.Categories|Blog.WebHome", data.resolve(B));
        // C: posts and categories stored elsewhere, and the space of its posts, found through the posts location of
        // the blog.
        String blogC = "BlogLocationC.WebHome|BlogLocationCPosts|BlogLocationCCategories|Blog.WebHome";
        assertEquals(blogC, data.resolve(C));
        assertEquals(blogC, data.resolve(C_POSTS));
        // D: an in-space blog, found through the Blog page of its space; without stored locations its posts are in its
        // space and it shares the categories of the default blog.
        assertEquals("BlogLocationD.Blog|BlogLocationD|Blog.Categories|Blog.WebHome", data.resolve(D));
        // E: a nested blog with special characters in its space name.
        assertEquals(E + ".WebHome|" + E + "|Blog.Categories|Blog.WebHome", data.resolve(E));
        // F: found as the first blog page of its space.
        assertEquals("BlogLocationF.MyBlog|BlogLocationF|Blog.Categories|Blog.WebHome", data.resolve(F));
        // The WebHome blog of a space is preferred to its Blog page.
        assertEquals("BlogLocationH.WebHome|BlogLocationH|Blog.Categories|Blog.WebHome",
            data.resolve("BlogLocationH"));
        // The Blog page of a space is preferred to the first blog page of the space.
        assertEquals("BlogLocationI.Blog|BlogLocationI|Blog.Categories|Blog.WebHome", data.resolve("BlogLocationI"));
        // The blog configured in the WebPreferences of a space is preferred to the blog of the space, and gives a blog
        // to a space that has none.
        String blogB = "BlogLocationB.WebHome|BlogLocationB|BlogLocationB.Categories|Blog.WebHome";
        assertEquals(blogB, data.resolve("BlogLocationOverride"));
        assertEquals(blogB, data.resolve("BlogLocationElsewhere"));
        // A space outside of any blog belongs to the default blog.
        assertEquals("Blog.WebHome|Blog|Blog.Categories|Blog.WebHome", data.resolve("Main"));
        // A categories space belongs to the blog that stores it as its categories location...
        assertEquals("Blog.WebHome|Blog|Blog.Categories|BlogLocationB.WebHome", data.resolve(B_CATEGORIES));
        assertEquals("Blog.WebHome|Blog|Blog.Categories|BlogLocationC.WebHome", data.resolve(C_CATEGORIES));
        // ... but the categories of the default blog belong to the first blog that stores them explicitly. This is a
        // bug: Blog.Categories should belong to the default blog, Blog.WebHome, whose categories location is empty
        // (the code only recognizes the default blog by the legacy categories space, Blog), so the category pages of
        // the default blog use the posts layout of another blog. See BLOG-281: The default blog is not recognized as
        // the owner of its categories once they are in Blog.Categories.
        assertEquals("Blog.WebHome|Blog|Blog.Categories|BlogLocationF.MyBlog", data.resolve(DEFAULT_CATEGORIES));
    }

    @Test
    @Order(2)
    void newPostsAreCreatedInThePostsLocationOfTheirBlog(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        List<DocumentReference> blogs = Arrays.asList(BLOG_B, BLOG_C, BLOG_D, BLOG_E);
        List<SpaceReference> postsLocations = Arrays.asList(BLOG_B.getLastSpaceReference(),
            new SpaceReference(WIKI, C_POSTS), BLOG_D.getLastSpaceReference(), E_SPACE);
        // The category a post gets when it is saved without one: the home page of the categories of its blog.
        List<String> defaultCategories = Arrays.asList("BlogLocationB.Categories.WebHome",
            "BlogLocationCCategories.WebHome", "Blog.Categories.WebHome", "Blog.Categories.WebHome");

        for (int i = 0; i < blogs.size(); i++) {
            String title = getPostTitle(i);
            CreateBlogPostPane createPane = BlogHomePage.gotoPage(blogs.get(i)).getCreateBlogPostPane();
            createPane.setTitle(title);
            BlogPostInlinePage editPage = createPane.clickCreateButton();
            BlogPostViewPage post = editPage.clickSaveAndViewWithDefaultCategory();
            assertEquals(title, post.getDocumentTitle());

            DocumentReference postReference = new DocumentReference(title, postsLocations.get(i));
            assertTrue(data.exists(postReference), "Post not created in its posts location: " + postReference);
            // The parent of the post is its blog.
            assertEquals(setup.serializeReference(blogs.get(i)).substring(WIKI.length() + 1),
                data.getParent(postReference));
            assertEquals(defaultCategories.get(i), data.getPostCategory(postReference));
        }

        // Each blog lists its own posts, and a post listed outside of its blog says which blog it is in.
        DocumentReference postedIn = new DocumentReference(WIKI, "BlogLocationPostedIn", WEB_HOME);
        StringBuilder content = new StringBuilder();
        for (String blog : Arrays.asList(B + ".WebHome", C + ".WebHome", D + ".Blog", E + ".WebHome")) {
            content.append(String.format("{{blogpostlist blog=\"%s\" layout=\"full\" /}}%n%n",
                blog.replace("~", "~~")));
        }
        data.createPage(postedIn, content.toString(), "Posted in");
        setup.gotoPage(postedIn);
        BlogPostListPage page = new BlogPostListPage();
        assertEquals(Arrays.asList(getPostTitle(0), SPORT_POST_TITLE, getPostTitle(1), getPostTitle(2),
            getPostTitle(3)), page.getPostTitles());
        assertEquals(Collections.singletonList("Blog B"), page.getPostBlogLocation(getPostTitle(0)));
        // This is a bug: the blog of the post is found from the posts location of its blog, BlogLocationCPosts, whose
        // WebHome is not a blog, so nothing is displayed. It should say that the post is in "Blog C". See BLOG-279:
        // The "Posted in" blog link is missing for the posts of a blog whose page is not the WebHome of its space.
        assertEquals(Collections.emptyList(), page.getPostBlogLocation(getPostTitle(1)));
        // This is a bug: the blog is looked for at BlogLocationD.WebHome instead of BlogLocationD.Blog, so nothing is
        // displayed. It should say that the post is in "Blog D". See BLOG-279.
        assertEquals(Collections.emptyList(), page.getPostBlogLocation(getPostTitle(2)));
        assertEquals(Arrays.asList("BlogLocationE", "Blog E"), page.getPostBlogLocation(getPostTitle(3)));
        // The blog of a post is not displayed on the blog itself.
        BlogHomePage blogB = BlogHomePage.gotoPage(BLOG_B);
        assertThat(blogB.getPostTitles(), hasItem(getPostTitle(0)));
        assertEquals(Collections.emptyList(), blogB.getPostBlogLocation(getPostTitle(0)));
    }

    @Test
    @Order(3)
    void categoriesOfTheBlogs(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);

        // The categories panel shows the categories of the blog of the displayed page.
        CategoriesPanel panel = BlogPostViewPage.gotoPage(new DocumentReference(getPostTitle(0),
            BLOG_B.getLastSpaceReference())).getCategoriesPanel();
        assertEquals(Collections.singletonList("Sport"), panel.getCategories());
        assertCategoriesPanelLinks(setup, panel, CATEGORIES_HOME_B);
        // A category page lists the posts of the category.
        BlogPostListPage sportPage = BlogPostListPage.gotoCategory(SPORT);
        assertEquals(Collections.singletonList(SPORT_POST_TITLE), sportPage.getPostTitles());
        assertCategoriesPanelLinks(setup, sportPage.getCategoriesPanel(), CATEGORIES_HOME_B);
        // Blog B is the only blog using its categories.
        assertFalse(sportPage.getRelatedBlogsPanel().isDisplayed());
        panel = BlogPostViewPage.gotoPage(new DocumentReference(getPostTitle(1), new SpaceReference(WIKI, C_POSTS)))
            .getCategoriesPanel();
        assertEquals(Collections.singletonList("Chess"), panel.getCategories());
        assertCategoriesPanelLinks(setup, panel, CATEGORIES_HOME_C);
        // Blog D shares the categories of the default blog.
        panel = BlogPostViewPage.gotoPage(new DocumentReference(getPostTitle(2), BLOG_D.getLastSpaceReference()))
            .getCategoriesPanel();
        assertThat(panel.getCategories(), hasItem("News"));
        assertCategoriesPanelLinks(setup, panel, DEFAULT_CATEGORIES_HOME);

        // This is a bug: the default blog is not listed among the blogs sharing its categories, nor are the blogs that
        // share them without storing Blog.Categories as their categories location (D, E). The panel should list Blog,
        // Blog D, Blog E, Blog F and Blog G. See BLOG-281: The default blog is not recognized as the owner of its
        // categories once they are in Blog.Categories.
        BlogHomePage defaultBlog = BlogHomePage.gotoPage();
        assertEquals(Arrays.asList("Blog F", "Blog G"), defaultBlog.getRelatedBlogsPanel().getBlogTitles());
        assertCategoriesPanelLinks(setup, defaultBlog.getCategoriesPanel(), DEFAULT_CATEGORIES_HOME);

        // This is a bug: the categories home page of blog B is not recognized as such, so it lists no post and its
        // categories panel shows the categories of the default blog. It should list the categorized posts of blog B
        // (Sport post) and show the categories of blog B. See BLOG-284: The categories home page of a blog other than
        // the default one lists no post and shows the categories of the default blog.
        BlogPostListPage categoriesHomePage = BlogPostListPage.gotoCategory(CATEGORIES_HOME_B);
        assertEquals(Collections.emptyList(), categoriesHomePage.getPostTitles());
        assertThat(categoriesHomePage.getCategoriesPanel().getCategories(), not(hasItem("Sport")));
        assertCategoriesPanelLinks(setup, categoriesHomePage.getCategoriesPanel(), DEFAULT_CATEGORIES_HOME);

        // The category feeds.
        assertEquals("BlogLocationB/Categories/Sport", data.getFeedCategory(SPORT, ""));
        // This is a bug: the feed of the categories home page of blog B is the feed of the categories of the default
        // blog. It should be "BlogLocationB/Categories/". See BLOG-284.
        assertEquals("Blog/Categories/", data.getFeedCategory(CATEGORIES_HOME_B, ""));
        // A reference to a category in the space of a blog is mapped to the category in the categories space of the
        // blog, as for the legacy categories of the default blog (Blog.News is now Blog.Categories.News).
        assertEquals("BlogLocationB/Categories/Sport",
            data.getFeedCategory(new DocumentReference(WIKI, B, "Sport"), ""));
        assertEquals("Blog/Categories/News", data.getFeedCategory(new DocumentReference(WIKI, "Blog", "News"), ""));
        assertEquals("BlogLocationB/Categories/Sport", data.getFeedCategory(new DocumentReference(WIKI, "Main",
            WEB_HOME), B_CATEGORIES + ".Sport"));
        // The feed of a page that is not a category is about the categories of the default blog.
        assertEquals("Blog/Categories/", data.getFeedCategory(new DocumentReference(WIKI, "Main", WEB_HOME), ""));
        assertEquals("Blog/Categories/", data.getFeedCategory(DEFAULT_CATEGORIES_HOME, ""));

        // The posts of a categories space are its categorized posts.
        DocumentReference categoriesSpacePosts = new DocumentReference(WIKI, "BlogLocationCategoriesSpace", WEB_HOME);
        data.createPage(categoriesSpacePosts, "(% id=\"posts\" %)(((\n{{blogpostlist category=\"" + B_CATEGORIES
            + "\" layout=\"link\" /}}\n)))", "Posts of a categories space");
        setup.gotoPage(categoriesSpacePosts);
        assertEquals(Collections.singletonList(SPORT_POST_TITLE),
            new BlogPostListMacroPage().getPostLinkTexts("posts"));

        // The categories of a blog are managed in its categories space.
        ManageCategoriesPage managePage = ManageCategoriesPage.gotoPage(CATEGORIES_HOME_B);
        assertEquals(Collections.singletonList("Sport"), managePage.getCategories());
        managePage.clickAddCategory();
        managePage.addCategory("Tennis");
        DocumentReference tennis = new DocumentReference("Tennis", CATEGORIES_HOME_B.getLastSpaceReference());
        assertTrue(data.exists(tennis));
        assertEquals("BlogLocationB.Categories.WebHome", data.getParent(tennis));
        managePage = ManageCategoriesPage.gotoPage(CATEGORIES_HOME_B);
        managePage.renameCategory("Tennis", "Squash");
        assertFalse(data.exists(tennis));
        assertTrue(data.exists(new DocumentReference("Squash", CATEGORIES_HOME_B.getLastSpaceReference())));
        managePage = ManageCategoriesPage.gotoPage(CATEGORIES_HOME_C);
        assertEquals(Collections.singletonList("Chess"), managePage.getCategories());
        managePage.clickAddCategory();
        managePage.addCategory("Go");
        DocumentReference go = new DocumentReference(WIKI, C_CATEGORIES, "Go");
        assertTrue(data.exists(go));
        assertEquals("BlogLocationCCategories.WebHome", data.getParent(go));
        assertThat(ManageCategoriesPage.gotoPage(CATEGORIES_HOME_B).getCategories(), not(hasItem("Go")));
    }

    private void assertCategoriesPanelLinks(TestUtils setup, CategoriesPanel panel, DocumentReference categoriesHome)
    {
        assertEquals(BlogTestData.getViewURL(setup, categoriesHome, FEED_QUERY_STRING), panel.getRSSURL());
        assertEquals(BlogTestData.getViewURL(setup, categoriesHome, ""), panel.getAllCategoriesURL());
        assertEquals(BlogTestData.getViewURL(setup, categoriesHome, MANAGE_QUERY_STRING),
            panel.getManageCategoriesURL());
    }

    private static String getPostTitle(int blogIndex)
    {
        return "Post of " + BLOG_TITLES.get(blogIndex);
    }
}
