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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.AllBlogsPanel;
import org.xwiki.blog.test.po.ArchivePanel;
import org.xwiki.blog.test.po.BlogHomePage;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.blog.test.po.BlogPostListPage;
import org.xwiki.blog.test.po.BlogPostViewPage;
import org.xwiki.blog.test.po.CategoriesPanel;
import org.xwiki.blog.test.po.UnpublishedPanel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the links of the blog panels displayed next to the pages of a blog: the archive, the unpublished posts, the
 * categories and the list of all the blogs.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The archive panel uses the JodaTime plugin.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.jodatime.JodaTimePlugin"
    },
    extraJARs = {
        // A plugin listed in xwiki.cfg is loaded at startup, before extensions get installed, so its JAR has to be
        // in WEB-INF/lib.
        "org.xwiki.platform:xwiki-platform-jodatime"
    },
    // Resolve the version of the extra JAR from the dependencies of the POM (it's not the version of this module).
    resolveExtraJARs = true
)
class BlogPanelsIT
{
    private static final String WIKI = "xwiki";

    private static final String BLOG = "Blog";

    private static final String CATEGORIES = "Categories";

    private static final String WEB_HOME = "WebHome";

    /**
     * A year in which no other test publishes posts, so that the archive of that year is known.
     */
    private static final int YEAR = 2009;

    /**
     * Another year in which no other test publishes posts, used by the test about the special characters in names.
     */
    private static final int SPECIAL_NAMES_YEAR = 2010;

    private static final String ARCHIVE_QUERY_STRING = "sheet=Blog.ArchiveSheet&year=";

    private static final String UNPUBLISHED_QUERY_STRING = "sheet=Blog.UnpublishedPostsSheet";

    private static final String FEED_QUERY_STRING = "xpage=plain&sheet=Blog.CategoryRss";

    private static final String MANAGE_QUERY_STRING = "action=manage";

    private static final String VIEW_ALL = "View all (1)";

    /**
     * A blog title containing wiki syntax, expected to be displayed as is.
     */
    private static final String TITLE_WITH_WIKI_SYNTAX = "Notes]] with **bold** and {{info}}box{{/info}} [[syntax";

    private static final String PASSWORD = "password";

    @Test
    @Order(1)
    void panelsLinkToThePagesOfTheBlog(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String prefix = getTestClassName(testReference);
        BlogTestData data = new BlogTestData(setup);

        // A category of the default blog, whose name contains spaces and an ampersand.
        DocumentReference defaultCategory =
            new DocumentReference(WIKI, Arrays.asList(BLOG, CATEGORIES), prefix + " Tips & Tricks");
        data.createCategory(defaultCategory, "Tips & Tricks", "Blog.Categories.WebHome");

        // Another blog, whose space name contains spaces, a dot and an ampersand, with its own categories.
        DocumentReference otherBlog = new DocumentReference(WIKI, prefix + " Panels 1.0 & Co", WEB_HOME);
        SpaceReference otherBlogSpace = otherBlog.getLastSpaceReference();
        SpaceReference otherCategoriesSpace = new SpaceReference(CATEGORIES, otherBlogSpace);
        String otherBlogTitle = prefix + " panels 1.0 notes";
        data.createBlog(otherBlog, otherBlogTitle, "", serializeLocal(setup, otherCategoriesSpace), false);
        data.enablePanels(otherBlogSpace);
        DocumentReference otherCategoriesHome = new DocumentReference(WEB_HOME, otherCategoriesSpace);
        data.createCategoriesHome(otherCategoriesHome);
        DocumentReference otherCategory = new DocumentReference("Topic & more", otherCategoriesSpace);
        data.createCategory(otherCategory, "Topic & more", serializeLocal(setup, otherCategoriesHome));

        // The posts are created by a regular user, who is the only one to see the unpublished ones.
        setup.createUserAndLogin(prefix + "Author", PASSWORD);
        DocumentReference defaultPost = new DocumentReference(WIKI, BLOG, prefix + " published");
        new BlogPostFixture(setup, defaultPost, "Published post")
            .withPublishDate(new GregorianCalendar(YEAR, 2, 10).getTime())
            .withCategory(serializeLocal(setup, defaultCategory)).create();
        new BlogPostFixture(setup, new DocumentReference(WIKI, BLOG, prefix + " unpublished"), "Unpublished post")
            .withPublished(false).withPublishDate(new GregorianCalendar(YEAR, 3, 10).getTime())
            .withCategory(serializeLocal(setup, defaultCategory)).create();
        DocumentReference otherPost = new DocumentReference("Old post", otherBlogSpace);
        new BlogPostFixture(setup, otherPost, "Old post").withPublishDate(new GregorianCalendar(YEAR, 4, 5).getTime())
            .withCategory(serializeLocal(setup, otherCategory)).create();
        new BlogPostFixture(setup, new DocumentReference("Recent post", otherBlogSpace), "Recent post")
            .withCategory(serializeLocal(setup, otherCategory)).create();
        new BlogPostFixture(setup, new DocumentReference("Draft post", otherBlogSpace), "Draft post")
            .withPublished(false).withCategory(serializeLocal(setup, otherCategory)).create();

        // The panels of the default blog link to the archive and the unpublished posts of the blog. The number of
        // posts of the year isn't checked since it depends on the posts created by other tests.
        DocumentReference defaultBlog = new DocumentReference(WIKI, BLOG, WEB_HOME);
        BlogPostListPage blogHome = BlogHomePage.gotoPage();
        String year = getYear(blogHome.getArchivePanel(), YEAR);
        assertEquals(getArchiveURL(setup, defaultBlog, YEAR), blogHome.getArchivePanel().getYearURL(year));
        assertUnpublishedPanelLink(setup, blogHome.getUnpublishedPanel(), defaultBlog);
        BlogPostViewPage postPage = BlogPostViewPage.gotoPage(defaultPost);
        year = getYear(postPage.getArchivePanel(), YEAR);
        assertEquals(getArchiveURL(setup, defaultBlog, YEAR), postPage.getArchivePanel().getYearURL(year));
        assertUnpublishedPanelLink(setup, postPage.getUnpublishedPanel(), defaultBlog);

        // The panels of a category link to the archive and the unpublished posts of the category.
        year = YEAR + " (2)";
        BlogPostListPage categoryPage = BlogPostListPage.gotoCategory(defaultCategory);
        assertEquals(Collections.singletonList(year), categoryPage.getArchivePanel().getYears());
        assertEquals(getArchiveURL(setup, defaultCategory, YEAR), categoryPage.getArchivePanel().getYearURL(year));
        assertUnpublishedPanelLink(setup, categoryPage.getUnpublishedPanel(), defaultCategory);

        // The archive panel lists the months of the current year.
        String currentYear = new GregorianCalendar().get(Calendar.YEAR) + " (2)";
        String currentMonth = new SimpleDateFormat("MMMM", Locale.ENGLISH).format(new Date()) + " (2)";
        blogHome = BlogHomePage.gotoPage(otherBlog);
        assertArchivePanelLinks(setup, blogHome.getArchivePanel(), otherBlog, currentYear, currentMonth);
        assertUnpublishedPanelLink(setup, blogHome.getUnpublishedPanel(), otherBlog);
        postPage = BlogPostViewPage.gotoPage(otherPost);
        assertArchivePanelLinks(setup, postPage.getArchivePanel(), otherBlog, currentYear, currentMonth);
        assertUnpublishedPanelLink(setup, postPage.getUnpublishedPanel(), otherBlog);
        categoryPage = BlogPostListPage.gotoCategory(otherCategory);
        assertArchivePanelLinks(setup, categoryPage.getArchivePanel(), otherCategory, currentYear, currentMonth);
        assertUnpublishedPanelLink(setup, categoryPage.getUnpublishedPanel(), otherCategory);

        // The categories panel links to the categories of the blog, which administrators can manage.
        setup.loginAsSuperAdmin();
        assertCategoriesPanelLinks(setup, BlogHomePage.gotoPage(otherBlog).getCategoriesPanel(), otherCategoriesHome);
        assertCategoriesPanelLinks(setup, BlogPostViewPage.gotoPage(otherPost).getCategoriesPanel(),
            otherCategoriesHome);
        assertCategoriesPanelLinks(setup, BlogPostListPage.gotoCategory(otherCategory).getCategoriesPanel(),
            otherCategoriesHome);

        // The all blogs panel lists the blogs of the wiki.
        AllBlogsPanel allBlogsPanel = BlogHomePage.gotoPage().getAllBlogsPanel();
        assertThat(allBlogsPanel.getBlogTitles(), hasItem(otherBlogTitle));
        assertEquals(BlogTestData.getViewURL(setup, otherBlog, ""), allBlogsPanel.getBlogURL(otherBlogTitle));
    }

    @Test
    @Order(2)
    void panelsOfABlogWithSpecialCharactersInItsName(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String prefix = getTestClassName(testReference);
        BlogTestData data = new BlogTestData(setup);

        // The names of the space of the blog and of the space of its categories contain the characters that end a
        // link, followed by wiki syntax, which are both expected to be part of the links of the panels.
        DocumentReference blog = new DocumentReference(WIKI, prefix + "]] **Bold** notes", WEB_HOME);
        SpaceReference categoriesSpace = new SpaceReference(WIKI, prefix + "]] **Bold** topics");
        data.createBlog(blog, "Special notes", "", serializeLocal(setup, categoriesSpace), false);
        data.enablePanels(blog.getLastSpaceReference());
        data.enablePanels(categoriesSpace);
        DocumentReference categoriesHome = new DocumentReference(WEB_HOME, categoriesSpace);
        data.createCategoriesHome(categoriesHome);
        DocumentReference category = new DocumentReference("Ideas", categoriesSpace);
        data.createCategory(category, "Ideas", serializeLocal(setup, categoriesHome));
        // The title of this blog contains wiki syntax, expected to be displayed as is.
        DocumentReference titledBlog = new DocumentReference(WIKI, prefix + " titled", WEB_HOME);
        data.createBlog(titledBlog, TITLE_WITH_WIKI_SYNTAX, "", "", false);

        setup.createUserAndLogin(prefix + "Writer", PASSWORD);
        new BlogPostFixture(setup, new DocumentReference("Published", blog.getLastSpaceReference()), "Published")
            .withPublishDate(new GregorianCalendar(SPECIAL_NAMES_YEAR, 5, 10).getTime())
            .withCategory(serializeLocal(setup, category)).create();
        new BlogPostFixture(setup, new DocumentReference("Unpublished", blog.getLastSpaceReference()), "Unpublished")
            .withPublished(false).withPublishDate(new GregorianCalendar(SPECIAL_NAMES_YEAR, 5, 20).getTime())
            .withCategory(serializeLocal(setup, category)).create();

        String year = SPECIAL_NAMES_YEAR + " (2)";
        BlogPostListPage blogHome = BlogHomePage.gotoPage(blog);
        assertArchivePanelLink(setup, blogHome.getArchivePanel(), blog, year);
        assertUnpublishedPanelLink(setup, blogHome.getUnpublishedPanel(), blog);
        BlogPostListPage categoryPage = BlogPostListPage.gotoCategory(category);
        assertArchivePanelLink(setup, categoryPage.getArchivePanel(), category, year);
        assertUnpublishedPanelLink(setup, categoryPage.getUnpublishedPanel(), category);

        setup.loginAsSuperAdmin();
        CategoriesPanel categoriesPanel = BlogHomePage.gotoPage(blog).getCategoriesPanel();
        assertCategoriesPanelLinks(setup, categoriesPanel, categoriesHome);
        assertEquals(Collections.emptyList(), categoriesPanel.getBoldTexts());
        categoriesPanel = BlogPostListPage.gotoCategory(category).getCategoriesPanel();
        assertCategoriesPanelLinks(setup, categoriesPanel, categoriesHome);
        assertEquals(Collections.emptyList(), categoriesPanel.getBoldTexts());

        AllBlogsPanel allBlogsPanel = BlogHomePage.gotoPage().getAllBlogsPanel();
        assertThat(allBlogsPanel.getBlogTitles(), hasItems(TITLE_WITH_WIKI_SYNTAX, "Special notes"));
        assertEquals(BlogTestData.getViewURL(setup, titledBlog, ""), allBlogsPanel.getBlogURL(TITLE_WITH_WIKI_SYNTAX));
        assertEquals(BlogTestData.getViewURL(setup, blog, ""), allBlogsPanel.getBlogURL("Special notes"));
        assertEquals(Collections.emptyList(), allBlogsPanel.getBoldTexts());
    }

    private static void assertArchivePanelLinks(TestUtils setup, ArchivePanel panel, DocumentReference page,
        String currentYear, String currentMonth)
    {
        String year = YEAR + " (1)";
        assertEquals(Arrays.asList(year, currentYear), panel.getYears());
        assertEquals(Collections.emptyList(), panel.getMonths(year));
        assertEquals(Collections.singletonList(currentMonth), panel.getMonths(currentYear));
        assertEquals(getArchiveURL(setup, page, YEAR), panel.getYearURL(year));
        int now = new GregorianCalendar().get(Calendar.YEAR);
        assertEquals(getArchiveURL(setup, page, now), panel.getYearURL(currentYear));
        assertEquals(BlogTestData.getViewURL(setup, page,
            ARCHIVE_QUERY_STRING + now + "&month=" + (new GregorianCalendar().get(Calendar.MONTH) + 1)),
            panel.getMonthURL(currentYear, currentMonth));
    }

    /**
     * @return the year listed by the archive panel, with its number of posts
     */
    private static String getYear(ArchivePanel panel, int year)
    {
        return panel.getYears().stream().filter(label -> label.startsWith(year + " (")).findFirst()
            .orElseThrow(() -> new AssertionError("No year [" + year + "] in " + panel.getYears()));
    }

    private static void assertArchivePanelLink(TestUtils setup, ArchivePanel panel, DocumentReference page,
        String year)
    {
        assertEquals(Collections.singletonList(year), panel.getYears());
        assertEquals(getArchiveURL(setup, page, SPECIAL_NAMES_YEAR), panel.getYearURL(year));
        assertEquals(Collections.emptyList(), panel.getBoldTexts());
    }

    private static void assertUnpublishedPanelLink(TestUtils setup, UnpublishedPanel panel, DocumentReference page)
    {
        assertEquals(VIEW_ALL, panel.getViewAllText());
        assertEquals(BlogTestData.getViewURL(setup, page, UNPUBLISHED_QUERY_STRING), panel.getViewAllURL());
        assertEquals(Collections.emptyList(), panel.getBoldTexts());
    }

    private static void assertCategoriesPanelLinks(TestUtils setup, CategoriesPanel panel,
        DocumentReference categoriesHome)
    {
        assertEquals(BlogTestData.getViewURL(setup, categoriesHome, FEED_QUERY_STRING), panel.getRSSURL());
        assertEquals(BlogTestData.getViewURL(setup, categoriesHome, ""), panel.getAllCategoriesURL());
        assertEquals(BlogTestData.getViewURL(setup, categoriesHome, MANAGE_QUERY_STRING),
            panel.getManageCategoriesURL());
    }

    private static String getArchiveURL(TestUtils setup, DocumentReference page, int year)
    {
        return BlogTestData.getViewURL(setup, page, ARCHIVE_QUERY_STRING + year);
    }

    /**
     * @return the reference serialized without the wiki, as the blog stores the references of its categories
     */
    private static String serializeLocal(TestUtils setup, EntityReference reference)
    {
        return setup.serializeReference(reference).substring(WIKI.length() + 1);
    }

    private static String getTestClassName(TestReference testReference)
    {
        return testReference.getLastSpaceReference().getParent().getName();
    }
}
