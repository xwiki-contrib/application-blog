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
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogArchivePage;
import org.xwiki.blog.test.po.BlogPostFixture;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the blog archive: the tree of the years and months in which posts were published, and the list of the posts
 * of a year.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The blog archive uses the JodaTime plugin.
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
class BlogArchiveIT
{
    private static final String WIKI = "xwiki";

    private static final String BLOG = "Blog";

    /**
     * A year in which no other test publishes posts, so that the archive of its months is known.
     */
    private static final int YEAR = 2011;

    /**
     * Another year in which no other test publishes posts, used by the test about the names of the pages.
     */
    private static final int PAGE_NAMES_YEAR = 2012;

    private static final String ARCHIVE_SHEET = "sheet=Blog.ArchiveSheet&year=";

    private static final String MONTH = "&month=";

    private static final String RELEASE_TITLE = "Release 2.0 & more";

    private static final String DRAFT_TITLE = "Draft notes";

    private static final String CRAFTS_TITLE = "Arts & crafts fair";

    private static final String TEAM_TITLE = "Team meeting";

    /**
     * Another year in which no other test publishes posts, used by the test about the titles of the posts.
     */
    private static final int TITLES_YEAR = 2013;

    /**
     * A post title containing wiki syntax, expected to be displayed as is.
     */
    private static final String TITLE_WITH_WIKI_SYNTAX = "Notes]] with **bold** and {{info}}box{{/info}} [[syntax";

    private static final String SPECIAL_NAME_TITLE = "Post with a special page name";

    private static final String PASSWORD = "password";

    @Test
    @Order(1)
    void archiveListsTheYearsAndMonthsOfThePosts(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String prefix = getTestClassName(testReference);
        BlogTestData data = new BlogTestData(setup);

        // Posts of the default blog, whose page names contain spaces, dots and ampersands, one of them unpublished.
        DocumentReference releasePost = new DocumentReference(WIKI, BLOG, prefix + " release 2.0 & more");
        new BlogPostFixture(setup, releasePost, RELEASE_TITLE)
            .withPublishDate(new GregorianCalendar(YEAR, 1, 20).getTime()).create();
        DocumentReference draftPost = new DocumentReference(WIKI, BLOG, prefix + " draft v1.0");
        new BlogPostFixture(setup, draftPost, DRAFT_TITLE).withPublished(false)
            .withPublishDate(new GregorianCalendar(YEAR, 1, 10).getTime()).create();

        // A post of the default blog in a category whose name contains a space and an ampersand.
        DocumentReference category =
            new DocumentReference(WIKI, Arrays.asList(BLOG, "Categories"), prefix + " Arts & Crafts");
        data.createCategory(category, "Arts & Crafts", "Blog.Categories.WebHome");
        DocumentReference craftsPost = new DocumentReference(WIKI, BLOG, prefix + " crafts");
        new BlogPostFixture(setup, craftsPost, CRAFTS_TITLE)
            .withPublishDate(new GregorianCalendar(YEAR, 6, 5).getTime())
            .withCategory("Blog.Categories." + category.getName()).create();

        // A post of another blog, whose space name contains spaces, a dot and an ampersand.
        DocumentReference otherBlog = new DocumentReference(WIKI, prefix + " Team Notes 1.0 & Co", "WebHome");
        data.createBlog(otherBlog, "Team notes", "", "", false);
        new BlogPostFixture(setup, new DocumentReference(WIKI, otherBlog.getLastSpaceReference().getName(),
            "Meeting"), TEAM_TITLE).withPublishDate(new GregorianCalendar(YEAR, 2, 15).getTime()).create();

        // The archive page of the default blog lists the years and months of its posts, including the unpublished
        // post of the current user, and links to the archive of each year and month.
        DocumentReference archive = new DocumentReference(WIKI, BLOG, "Archive");
        BlogArchivePage archivePage = BlogArchivePage.gotoPage();
        String year = YEAR + " (3)";
        assertThat(archivePage.getYears(), hasItem(year));
        assertEquals(Arrays.asList("February (2)", "July (1)"), archivePage.getMonths(year));
        assertEquals(getArchiveURL(setup, archive, YEAR), archivePage.getYearURL(year));
        assertEquals(getArchiveURL(setup, archive, YEAR, 2), archivePage.getMonthURL(year, "February (2)"));
        assertEquals(getArchiveURL(setup, archive, YEAR, 7), archivePage.getMonthURL(year, "July (1)"));

        // The archive of another blog lists the years and months of the posts of that blog only.
        archivePage = BlogArchivePage.gotoArchiveOf(otherBlog);
        year = YEAR + " (1)";
        assertEquals(Collections.singletonList(year), archivePage.getYears());
        assertEquals(Collections.singletonList("March (1)"), archivePage.getMonths(year));
        assertEquals(getArchiveURL(setup, otherBlog, YEAR), archivePage.getYearURL(year));
        assertEquals(getArchiveURL(setup, otherBlog, YEAR, 3), archivePage.getMonthURL(year, "March (1)"));

        // The archive of a category lists the years and months of the posts of that category only.
        archivePage = BlogArchivePage.gotoArchiveOf(category);
        assertEquals(Collections.singletonList(year), archivePage.getYears());
        assertEquals(Collections.singletonList("July (1)"), archivePage.getMonths(year));
        assertEquals(getArchiveURL(setup, category, YEAR), archivePage.getYearURL(year));
        assertEquals(getArchiveURL(setup, category, YEAR, 7), archivePage.getMonthURL(year, "July (1)"));
    }

    @Test
    @Order(2)
    void archiveOfAYearListsThePostsByMonth(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        String prefix = getTestClassName(testReference);

        // Relies on the posts created by the previous test.
        DocumentReference archive = new DocumentReference(WIKI, BLOG, "Archive");
        BlogArchivePage archivePage = BlogArchivePage.gotoPage(YEAR);
        assertEquals(Arrays.asList("February (2)", "July (1)"), archivePage.getMonthHeadings());
        assertEquals(getArchiveURL(setup, archive, YEAR, 2), archivePage.getMonthHeadingURL("February (2)"));
        assertEquals(getArchiveURL(setup, archive, YEAR, 7), archivePage.getMonthHeadingURL("July (1)"));

        // The posts are listed by publication date, and the unpublished post of the current user is marked as such.
        assertEquals(Arrays.asList(DRAFT_TITLE + " (unpublished)", RELEASE_TITLE),
            archivePage.getPosts("February (2)"));
        assertEquals(Collections.singletonList(CRAFTS_TITLE), archivePage.getPosts("July (1)"));
        assertEquals(getPostURL(setup, prefix + " draft v1.0"), archivePage.getPostURL(DRAFT_TITLE));
        assertEquals(getPostURL(setup, prefix + " release 2.0 & more"), archivePage.getPostURL(RELEASE_TITLE));
        assertEquals(getPostURL(setup, prefix + " crafts"), archivePage.getPostURL(CRAFTS_TITLE));
    }

    @Test
    @Order(3)
    void archiveOfAPageWithSpecialCharactersInItsName(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        String prefix = getTestClassName(testReference);
        new BlogPostFixture(setup, new DocumentReference(WIKI, BLOG, prefix + " page names"), "Page names")
            .withPublishDate(new GregorianCalendar(PAGE_NAMES_YEAR, 4, 5).getTime()).create();

        // The name of the page contains the characters that end a link, followed by wiki syntax, which are both
        // expected to be part of the links to the archive of that page.
        DocumentReference page = new DocumentReference(WIKI, BLOG, prefix + "]] **Bold** archive");
        setup.createPage(page, "", "Archive of a page with special characters in its name");

        BlogArchivePage archivePage = BlogArchivePage.gotoArchiveOf(page);
        String year = PAGE_NAMES_YEAR + " (1)";
        assertThat(archivePage.getYears(), hasItem(year));
        assertEquals(getArchiveURL(setup, page, PAGE_NAMES_YEAR), archivePage.getYearURL(year));
        assertEquals(getArchiveURL(setup, page, PAGE_NAMES_YEAR, 5), archivePage.getMonthURL(year, "May (1)"));
        assertEquals(Collections.emptyList(), archivePage.getBoldTexts());

        archivePage = BlogArchivePage.gotoArchiveOf(page, PAGE_NAMES_YEAR);
        assertEquals(Collections.singletonList("May (1)"), archivePage.getMonthHeadings());
        assertEquals(getArchiveURL(setup, page, PAGE_NAMES_YEAR, 5), archivePage.getMonthHeadingURL("May (1)"));
        assertEquals(Collections.emptyList(), archivePage.getBoldTexts());
    }

    @Test
    @Order(4)
    void archiveOfAYearDisplaysTheTitlesOfThePostsVerbatim(TestUtils setup, TestReference testReference)
    {
        String prefix = getTestClassName(testReference);

        // The posts are created by a regular user.
        setup.loginAsSuperAdmin();
        setup.createUserAndLogin(prefix + "Author", PASSWORD);
        DocumentReference post = new DocumentReference(WIKI, BLOG, prefix + " wiki syntax title");
        new BlogPostFixture(setup, post, TITLE_WITH_WIKI_SYNTAX)
            .withPublishDate(new GregorianCalendar(TITLES_YEAR, 2, 10).getTime()).create();
        // The name of this post contains the characters that end a link, followed by wiki syntax.
        DocumentReference specialNamePost = new DocumentReference(WIKI, BLOG, prefix + "]] **Bold** post");
        new BlogPostFixture(setup, specialNamePost, SPECIAL_NAME_TITLE)
            .withPublishDate(new GregorianCalendar(TITLES_YEAR, 2, 20).getTime()).create();

        BlogArchivePage archivePage = BlogArchivePage.gotoPage(TITLES_YEAR);
        assertEquals(Arrays.asList(TITLE_WITH_WIKI_SYNTAX, SPECIAL_NAME_TITLE), archivePage.getPosts("March (2)"));
        assertEquals(BlogTestData.getViewURL(setup, post, ""), archivePage.getPostURL(TITLE_WITH_WIKI_SYNTAX));
        assertEquals(BlogTestData.getViewURL(setup, specialNamePost, ""),
            archivePage.getPostURL(SPECIAL_NAME_TITLE));
        assertEquals(Collections.emptyList(), archivePage.getBoldTexts());
    }

    private static String getArchiveURL(TestUtils setup, DocumentReference page, int year)
    {
        return BlogTestData.getViewURL(setup, page, ARCHIVE_SHEET + year);
    }

    private static String getArchiveURL(TestUtils setup, DocumentReference page, int year, int month)
    {
        return BlogTestData.getViewURL(setup, page, ARCHIVE_SHEET + year + MONTH + month);
    }

    private static String getPostURL(TestUtils setup, String name)
    {
        return BlogTestData.getViewURL(setup, new DocumentReference(WIKI, BLOG, name), "");
    }

    private static String getTestClassName(TestReference testReference)
    {
        return testReference.getLastSpaceReference().getParent().getName();
    }
}
