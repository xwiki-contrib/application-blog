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

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogMigrationPage;
import org.xwiki.blog.test.po.BlogMigrationPage.ReportEntry;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the migration of the articles of the old blog application ({@code XWiki.ArticleClass} objects) to blog
 * posts, done by {@code Blog.Migration}, and its report.
 *
 * @version $Id$
 */
@UITest
class BlogMigrationIT
{
    private static final String WIKI = "xwiki";

    private static final String ARTICLE_CLASS = "XWiki.ArticleClass";

    private static final String PASSWORD = "password";

    private static final String TITLE = "title";

    private static final String EDIT = "edit";

    private static final String WEB_HOME = "WebHome";

    private static final String UPDATED = "Updated ";

    private static final String SKIPPED = "Skipping protected page ";

    /**
     * Characters that have a meaning in wiki syntax, which the report must display as they are.
     */
    private static final String SPECIAL_CHARACTERS = "]] **strong** [[Main.WebHome]]";

    @Test
    @Order(1)
    void migratedAndProtectedArticlesAreListedInTheReport(TestUtils setup, TestReference testReference)
        throws Exception
    {
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        String user = getUser(testClassName);
        SpaceReference space = new SpaceReference(WIKI, testClassName + " ordinary articles");
        DocumentReference article = new DocumentReference("Ordinary article 1.0 & more", space);
        String title = "Ordinary article title";

        setup.loginAsSuperAdmin();
        createArticleClass(setup);
        setup.createUser(user, PASSWORD, null);
        createSpaceHome(setup, space);
        createArticle(setup, article, title);
        denyEdit(setup, article, user);

        // A user who can't edit the article is told that it's skipped.
        setup.login(user, PASSWORD);
        List<ReportEntry> entries = BlogMigrationPage.gotoPage().migrate().getReportEntries();
        ReportEntry skipped = getEntry(entries, SKIPPED + article.getName());
        assertEquals(List.of(article.getName()), skipped.getLinkLabels());
        assertEquals(List.of(BlogTestData.getViewURL(setup, article, "")), skipped.getLinkURLs());

        // An administrator migrates it.
        setup.loginAsSuperAdmin();
        entries = BlogMigrationPage.gotoPage().migrate().getReportEntries();
        ReportEntry updated = getEntry(entries, UPDATED + title + " (in space " + space.getName() + ")");
        assertEquals(List.of(title, space.getName()), updated.getLinkLabels());
        assertEquals(List.of(BlogTestData.getViewURL(setup, article, ""),
            BlogTestData.getViewURL(setup, new DocumentReference(WEB_HOME, space), "")), updated.getLinkURLs());
    }

    @Test
    @Order(2)
    void specialCharactersInPageNamesAndTitlesAreDisplayedVerbatimInTheReport(TestUtils setup,
        TestReference testReference) throws Exception
    {
        String testClassName = testReference.getLastSpaceReference().getParent().getName();
        String user = getUser(testClassName);
        SpaceReference space = new SpaceReference(WIKI, testClassName + SPECIAL_CHARACTERS);
        DocumentReference ownArticle = new DocumentReference("Own article " + SPECIAL_CHARACTERS, space);
        String title = "Own article title " + SPECIAL_CHARACTERS;
        DocumentReference protectedArticle = new DocumentReference("Protected article " + SPECIAL_CHARACTERS, space);

        setup.loginAsSuperAdmin();
        createSpaceHome(setup, space);
        createArticle(setup, protectedArticle, "Protected article title");
        denyEdit(setup, protectedArticle, user);

        // The user creates an article and migrates it.
        setup.login(user, PASSWORD);
        createArticle(setup, ownArticle, title);
        List<ReportEntry> entries = BlogMigrationPage.gotoPage().migrate().getReportEntries();

        // The space is displayed as a reference, in which the dots of the space name are escaped.
        String spaceLabel = space.getName().replace(".", "\\.");
        ReportEntry updated = getEntry(entries, UPDATED + title + " (in space " + spaceLabel + ")");
        assertEquals(List.of(title, spaceLabel), updated.getLinkLabels());
        assertEquals(List.of(BlogTestData.getViewURL(setup, ownArticle, ""),
            BlogTestData.getViewURL(setup, new DocumentReference(WEB_HOME, space), "")), updated.getLinkURLs());

        ReportEntry skipped = getEntry(entries, SKIPPED + protectedArticle.getName());
        assertEquals(List.of(protectedArticle.getName()), skipped.getLinkLabels());
        assertEquals(List.of(BlogTestData.getViewURL(setup, protectedArticle, "")), skipped.getLinkURLs());
    }

    private static String getUser(String testClassName)
    {
        return testClassName + "User";
    }

    /**
     * The class of the articles of the old blog application isn't provided by the wiki anymore: create the properties
     * that the migration copies.
     */
    private static void createArticleClass(TestUtils setup) throws Exception
    {
        DocumentReference articleClass = new DocumentReference(WIKI, "XWiki", "ArticleClass");
        if (!setup.rest().exists(articleClass)) {
            setup.addClassProperty(articleClass, TITLE, "String");
            setup.addClassProperty(articleClass, "content", "TextArea");
        }
    }

    private static void createSpaceHome(TestUtils setup, SpaceReference space)
    {
        setup.createPage(new DocumentReference(WEB_HOME, space), "", space.getName());
    }

    /**
     * Creates an article of the old blog application, as the current user.
     */
    private static void createArticle(TestUtils setup, DocumentReference reference, String title)
    {
        setup.createPage(reference, "", reference.getName());
        setup.addObject(reference, ARTICLE_CLASS, TITLE, title, "content", "Content of " + title);
    }

    /**
     * Denies the edit right on a page to a user. The rights are set from the browser because the REST client of the
     * tests can't build a URL with square brackets.
     */
    private static void denyEdit(TestUtils setup, DocumentReference reference, String user)
    {
        setup.addObject(reference, "XWiki.XWikiRights", "users", "XWiki." + user, "levels", EDIT, "allow", 0);
    }

    private static ReportEntry getEntry(List<ReportEntry> entries, String text)
    {
        List<String> texts = entries.stream().map(ReportEntry::getText).collect(Collectors.toList());
        assertThat(texts, hasItem(text));
        return entries.stream().filter(entry -> text.equals(entry.getText())).findFirst().orElseThrow();
    }
}
