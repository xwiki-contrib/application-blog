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
package org.xwiki.contrib.blog;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.macro.velocity.filter.NoneVelocityMacroFilter;
import org.xwiki.rendering.internal.util.XWikiSyntaxEscaper;
import org.xwiki.rendering.macro.MacroCategoryManager;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.security.SecurityConfiguration;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.script.SecurityAuthorizationScriptService;
import org.xwiki.security.authorization.script.internal.RightConverter;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.jodatime.JodaTimePlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.blog.FeedFixture.BLOG;
import static org.xwiki.contrib.blog.FeedFixture.WIKI;

/**
 * Page test for the document {@code Blog.CategoryRss}, the feed of a blog category. In a page test the translations
 * are displayed as their key followed by their parameters.
 *
 * @version $Id$
 */
@ComponentList({
    ModelScriptService.class,
    NoneVelocityMacroFilter.class,
    RenderingScriptService.class,
    RightConverter.class,
    SecurityAuthorizationScriptService.class,
    SecurityScriptService.class,
    TranslationMacro.class,
    XWikiSyntaxEscaper.class
})
@HTML50ComponentList
@XWikiSyntax21ComponentList
class CategoryRssPageTest extends PageTest
{
    private static final String CATEGORIES = "Categories";

    private static final DocumentReference CATEGORY_CLASS = new DocumentReference(WIKI, BLOG, "CategoryClass");

    private static final DocumentReference CATEGORIES_HOME =
        new DocumentReference(WIKI, Arrays.asList(BLOG, CATEGORIES), "WebHome");

    private static final DocumentReference NEWS = new DocumentReference(WIKI, Arrays.asList(BLOG, CATEGORIES), "News");

    private static final DocumentReference MAIN_PAGE = new DocumentReference(WIKI, "Main", "WebHome");

    private static final DocumentReference SHEET_BINDING_CLASS =
        new DocumentReference(WIKI, "XWiki", "DocumentSheetBinding");

    private static final String SHEET = "sheet";

    private static final String CATEGORY_RSS = "Blog.CategoryRss";

    private static final String CATEGORY = "category";

    private static final String TITLE = "title";

    private static final String NAME = "name";

    private static final String DESCRIPTION = "description";

    private static final String DEFAULT_CATEGORIES_PAGE = "Blog/Categories/";

    /**
     * A text made of characters that have a meaning in XML or in wiki syntax.
     */
    private static final String SPECIAL_TEXT = "Fish & <Chips> {{/html}}**bold**";

    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @MockComponent
    private MacroCategoryManager macroCategoryManager;

    @MockComponent
    private SecurityConfiguration securityConfiguration;

    private FeedFixture fixture;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.getPluginManager().addPlugin("jodatime", JodaTimePlugin.class.getName(), this.context);
        FeedFixture.registerBlogScriptService(this.componentManager);
        FeedFixture.registerEmptyQueryService(this.componentManager);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(Right.class),
            any(EntityReference.class))).thenReturn(true);

        loadPage(new DocumentReference(WIKI, BLOG, "BlogClass"));
        loadPage(CATEGORY_CLASS);
        loadPage(new DocumentReference(WIKI, BLOG, "BlogParameters"));
        loadPage(new DocumentReference(WIKI, BLOG, "BlogCode"));
        loadPage(new DocumentReference(WIKI, BLOG, "CategoriesCode"));
        loadPage(new DocumentReference(WIKI, BLOG, "RssCode"));
        loadPage(new DocumentReference(WIKI, BLOG, "CategoryRss"));
        this.fixture = new FeedFixture(this.xwiki, this.context);
        // The class of the sheet bindings, simplified: its sheet property is a text field.
        XWikiDocument sheetBindingClass = this.fixture.getDocument(SHEET_BINDING_CLASS);
        sheetBindingClass.getXClass().addTextField(SHEET, "Sheet", 30);
        this.fixture.save(sheetBindingClass);
        loadPage(CATEGORIES_HOME);

        createCategory(NEWS, "News");
        this.request.put("xpage", "plain");
    }

    @Test
    void feedOfACategory() throws Exception
    {
        Document feed = this.fixture.renderFeed(NEWS, CATEGORY_RSS);

        assertEquals("Blog/Categories/News", FeedFixture.getChannelPage(feed));
        assertEquals("News", FeedFixture.getChannelText(feed, TITLE));
        assertEquals("blog.code.description.category [News]", FeedFixture.getChannelText(feed, DESCRIPTION));
    }


    @Test
    void feedOfTheCategoriesHome() throws Exception
    {
        Document feed = this.fixture.renderFeed(CATEGORIES_HOME, CATEGORY_RSS);

        assertEquals(DEFAULT_CATEGORIES_PAGE, FeedFixture.getChannelPage(feed));
        // The default categories home has no sheet binding value, so the feed displays the name of its category, which
        // it doesn't have.
        assertEquals("", FeedFixture.getChannelText(feed, TITLE));
    }

    @Test
    void feedOfTheCategoryGivenInTheRequest() throws Exception
    {
        this.request.put(CATEGORY, "Blog.Categories.News");

        Document feed = this.fixture.renderFeed(MAIN_PAGE, CATEGORY_RSS);

        assertEquals("Blog/Categories/News", FeedFixture.getChannelPage(feed));
        assertEquals("News", FeedFixture.getChannelText(feed, TITLE));
    }

    @Test
    void feedOfTheCategoriesHomeGivenInTheRequest() throws Exception
    {
        createCategoriesHome("Other", "");
        this.request.put(CATEGORY, "Other.Categories.WebHome");

        Document feed = this.fixture.renderFeed(MAIN_PAGE, CATEGORY_RSS);

        assertEquals("Other/Categories/", FeedFixture.getChannelPage(feed));
        assertEquals("blog.categories.webhome.title", FeedFixture.getChannelText(feed, TITLE));
    }

    @Test
    void feedOfAPageThatIsNotACategory() throws Exception
    {
        Document feed = this.fixture.renderFeed(new DocumentReference(WIKI, BLOG, "Unknown"), CATEGORY_RSS);

        assertEquals(DEFAULT_CATEGORIES_PAGE, FeedFixture.getChannelPage(feed));
    }

    @Test
    void categoryNameWithSpecialCharactersIsDisplayedVerbatim() throws Exception
    {
        DocumentReference category = new DocumentReference(WIKI, Arrays.asList(BLOG, CATEGORIES), "Special");
        createCategory(category, SPECIAL_TEXT);

        Document feed = this.fixture.renderFeed(category, CATEGORY_RSS);

        assertEquals(SPECIAL_TEXT, FeedFixture.getChannelText(feed, TITLE));
        assertEquals("blog.code.description.category [" + SPECIAL_TEXT + "]",
            FeedFixture.getChannelText(feed, DESCRIPTION));
        assertFalse(FeedFixture.containsElement(feed, "strong"));
    }

    @Test
    void categoriesHomeTitleWithSpecialCharactersIsDisplayedVerbatim() throws Exception
    {
        createCategoriesHome("Special", SPECIAL_TEXT);
        this.request.put(CATEGORY, "Special.Categories.WebHome");

        Document feed = this.fixture.renderFeed(MAIN_PAGE, CATEGORY_RSS);

        assertEquals(SPECIAL_TEXT, FeedFixture.getChannelText(feed, TITLE));
        assertFalse(FeedFixture.containsElement(feed, "strong"));
    }

    @Test
    void feedOfARequestedPageThatIsNotACategoryIsTheFeedOfTheDefaultCategories() throws Exception
    {
        XWikiDocument page = this.fixture.getDocument(new DocumentReference(WIKI, "Main", "Other"));
        page.setTitle("Other page");
        this.fixture.save(page);
        this.request.put(CATEGORY, "Main.Other");

        Document feed = this.fixture.renderFeed(MAIN_PAGE, CATEGORY_RSS);

        assertEquals(DEFAULT_CATEGORIES_PAGE, FeedFixture.getChannelPage(feed));
        assertEquals("", FeedFixture.getChannelText(feed, TITLE));
    }

    @Test
    void feedOfARequestedCategoryThatCannotBeViewedIsTheFeedOfTheDefaultCategories() throws Exception
    {
        DocumentReference category = new DocumentReference(WIKI, Arrays.asList(BLOG, CATEGORIES), "Restricted");
        createCategory(category, "Restricted");
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(eq(Right.VIEW), eq(category)))
            .thenReturn(false);
        this.request.put(CATEGORY, "Blog.Categories.Restricted");

        Document feed = this.fixture.renderFeed(MAIN_PAGE, CATEGORY_RSS);

        assertEquals(DEFAULT_CATEGORIES_PAGE, FeedFixture.getChannelPage(feed));
        assertEquals("", FeedFixture.getChannelText(feed, TITLE));
    }

    private void createCategory(DocumentReference reference, String name) throws Exception
    {
        XWikiDocument category = this.fixture.getDocument(reference);
        category.newXObject(CATEGORY_CLASS, this.context).setStringValue(NAME, name);
        this.fixture.save(category);
    }

    /**
     * Creates the home page of a categories space, displayed with the categories sheet, as {@code Blog.Management}
     * does.
     *
     * @param space the space of the blog
     * @param title the title of the categories home
     */
    private void createCategoriesHome(String space, String title) throws Exception
    {
        DocumentReference reference = new DocumentReference(WIKI, Arrays.asList(space, CATEGORIES), "WebHome");
        XWikiDocument categoriesHome = this.fixture.getDocument(reference);
        categoriesHome.setTitle(title);
        categoriesHome.newXObject(SHEET_BINDING_CLASS, this.context).setStringValue(SHEET, "Blog.CategoriesSheet");
        this.fixture.save(categoriesHome);
    }
}
