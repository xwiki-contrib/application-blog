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

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.blog.script.BlogScriptService;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.macro.velocity.filter.NoneVelocityMacroFilter;
import org.xwiki.rendering.internal.util.XWikiSyntaxEscaper;
import org.xwiki.rendering.macro.MacroCategoryManager;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.plugin.jodatime.JodaTimePlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.blog.BlogLocationFixture.WIKI;

/**
 * Page test for the document {@code Blog.CategoryRss}: which category a feed is about, in particular how a reference to
 * a category of the legacy {@code Blog} space is mapped to the migrated category in {@code Blog.Categories}.
 *
 * @version $Id$
 */
@ComponentList({
    ModelScriptService.class,
    NoneVelocityMacroFilter.class,
    RenderingScriptService.class,
    TranslationMacro.class,
    XWikiSyntaxEscaper.class
})
@HTML50ComponentList
@XWikiSyntax21ComponentList
class CategoryRssPageTest extends PageTest
{
    private static final DocumentReference CATEGORIES_HOME_A =
        new DocumentReference(WIKI, Arrays.asList("Blog", "Categories"), "WebHome");

    private static final DocumentReference MIGRATED_NEWS =
        new DocumentReference(WIKI, Arrays.asList("Blog", "Categories"), "News");

    private static final DocumentReference LEGACY_NEWS = new DocumentReference(WIKI, "Blog", "News");

    private static final DocumentReference CATEGORY_B =
        new DocumentReference(WIKI, Arrays.asList("SpaceB", "Categories"), "Sport");

    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @MockComponent
    private MacroCategoryManager macroCategoryManager;

    private BlogScriptService blogScriptService;

    private BlogLocationFixture fixture;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.getPluginManager().addPlugin("jodatime", JodaTimePlugin.class.getName(), this.context);
        this.blogScriptService = BlogLocationFixture.registerBlogScriptService(this.componentManager);
        BlogLocationFixture.registerEmptyQueryService(this.componentManager);

        loadPage(BlogLocationFixture.BLOG_CLASS);
        loadPage(BlogLocationFixture.CATEGORY_CLASS);
        loadPage(new DocumentReference(WIKI, "Blog", "BlogParameters"));
        loadPage(new DocumentReference(WIKI, "Blog", "BlogCode"));
        loadPage(new DocumentReference(WIKI, "Blog", "CategoriesCode"));
        loadPage(new DocumentReference(WIKI, "Blog", "RssCode"));
        loadPage(new DocumentReference(WIKI, "Blog", "CategoryRss"));
        loadPage(CATEGORIES_HOME_A);

        this.fixture = new BlogLocationFixture(this.xwiki, this.context);
        this.fixture.createBlogs();
        this.fixture.createCategory(MIGRATED_NEWS);
        this.fixture.createCategory(CATEGORY_B);

        this.request.put("xpage", "plain");
    }

    @Test
    void feedOfAMigratedLegacyCategory() throws Exception
    {
        // The legacy category page Blog.News doesn't exist anymore: the feed is about the migrated category.
        assertEquals("Blog/Categories/News", getFeedCategory(LEGACY_NEWS));
    }

    @Test
    void feedOfAMigratedLegacyCategoryRequestedThroughItsNestedPage() throws Exception
    {
        // A request for Blog/News resolves to Blog.News.WebHome when Blog.News doesn't exist.
        assertEquals("Blog/Categories/News",
            getFeedCategory(new DocumentReference(WIKI, Arrays.asList("Blog", "News"), "WebHome")));
    }

    @Test
    void feedOfALegacyCategoryWhenTheWikiStillHasLegacyCategoryAssignments() throws Exception
    {
        when(this.blogScriptService.hasLegacyCategoryAssignments()).thenReturn(true);
        this.fixture.createCategory(LEGACY_NEWS);

        assertEquals("Blog/News", getFeedCategory(LEGACY_NEWS));
    }

    @Test
    void feedOfACategoryOfAnotherBlog() throws Exception
    {
        assertEquals("SpaceB/Categories/Sport", getFeedCategory(CATEGORY_B));
    }

    @Test
    void feedOfTheCategoriesHome() throws Exception
    {
        assertEquals("Blog/Categories/", getFeedCategory(CATEGORIES_HOME_A));
    }

    @Test
    void feedOfTheCategoryGivenInTheRequest() throws Exception
    {
        this.request.put("category", "SpaceB.Categories.Sport");

        assertEquals("SpaceB/Categories/Sport", getFeedCategory(new DocumentReference(WIKI, "Main", "Page")));
    }

    @Test
    void feedOfAPageThatIsNotACategoryIsAboutTheDefaultCategoriesHome() throws Exception
    {
        // Also the case of a legacy category reference whose migrated category doesn't exist.
        assertEquals("Blog/Categories/", getFeedCategory(new DocumentReference(WIKI, "Blog", "Unknown")));
    }

    @Test
    void feedOfAPageThatIsNotACategoryWhenTheWikiStillHasLegacyCategoryAssignments() throws Exception
    {
        when(this.blogScriptService.hasLegacyCategoryAssignments()).thenReturn(true);

        assertEquals("Blog/Categories", getFeedCategory(new DocumentReference(WIKI, "Main", "Page")));
    }

    /**
     * @param page the page on which the category feed sheet is applied
     * @return the URL path, relative to the view action, of the category the feed is about
     */
    private String getFeedCategory(DocumentReference page) throws Exception
    {
        String feed = this.fixture.render(page, "{{include reference=\"Blog.CategoryRss\"/}}");
        String channel = Jsoup.parse(feed, "", Parser.xmlParser()).select("channel").attr("rdf:about");
        return channel.substring(channel.indexOf("/view/") + "/view/".length());
    }
}
