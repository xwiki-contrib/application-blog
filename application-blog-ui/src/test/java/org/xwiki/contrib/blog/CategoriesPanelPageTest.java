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
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.blog.script.BlogScriptService;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.blog.BlogLocationFixture.WIKI;

/**
 * Page test for the panel {@code Blog.CategoriesPanel}: which categories space it is about, as shown by its RSS link,
 * its link to all the categories and its link to manage them. The listed categories come from a query and are not
 * covered here.
 *
 * @version $Id$
 */
@ComponentList({
    ModelScriptService.class,
    RenderingScriptService.class,
    TranslationMacro.class,
    XWikiSyntaxEscaper.class
})
@HTML50ComponentList
@XWikiSyntax21ComponentList
class CategoriesPanelPageTest extends PageTest
{
    private static final DocumentReference PANEL_CLASS = new DocumentReference(WIKI, "Panels", "PanelClass");

    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @MockComponent
    private MacroCategoryManager macroCategoryManager;

    private BlogScriptService blogScriptService;

    private BlogLocationFixture fixture;

    private String panelContent;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.getPluginManager().addPlugin("jodatime", JodaTimePlugin.class.getName(), this.context);
        this.blogScriptService = BlogLocationFixture.registerBlogScriptService(this.componentManager);
        BlogLocationFixture.registerEmptyQueryService(this.componentManager);
        when(this.oldcore.getMockRightService().hasAdminRights(any())).thenReturn(true);

        loadPage(BlogLocationFixture.BLOG_CLASS);
        loadPage(BlogLocationFixture.CATEGORY_CLASS);
        loadPage(new DocumentReference(WIKI, "Blog", "BlogParameters"));
        loadPage(new DocumentReference(WIKI, "Blog", "BlogCode"));
        loadPage(new DocumentReference(WIKI, "Blog", "CategoriesCode"));
        this.panelContent = loadPage(new DocumentReference(WIKI, "Blog", "CategoriesPanel")).getXObject(PANEL_CLASS)
            .getLargeStringValue("content");

        this.fixture = new BlogLocationFixture(this.xwiki, this.context);
        this.fixture.createBlogs();
    }

    @Test
    void onAPostOfTheDefaultBlog() throws Exception
    {
        Document panel = renderPanel(new DocumentReference(WIKI, "Blog", "Post"));

        // The categories of the default blog are in Blog.Categories, so the panel treats them as the categories space
        // of any other blog.
        assertEquals("Blog.Categories.WebHome?xpage=plain&sheet=Blog.CategoryRss", getRSSLink(panel));
        assertEquals("/xwiki/bin/view/Blog/Categories/", getAllCategoriesLink(panel));
        assertEquals("Blog.Categories.WebHome?action=manage", getManageLink(panel));
    }

    @Test
    void onAPostOfTheDefaultBlogWhenTheWikiStillHasLegacyCategoryAssignments() throws Exception
    {
        when(this.blogScriptService.hasLegacyCategoryAssignments()).thenReturn(true);

        Document panel = renderPanel(new DocumentReference(WIKI, "Blog", "Post"));

        assertEquals("Blog.WebHome?xpage=plain&sheet=Blog.BlogRss", getRSSLink(panel));
        assertEquals("/xwiki/bin/view/Blog/", getAllCategoriesLink(panel));
        assertEquals("Blog.ManageCategories", getManageLink(panel));
    }

    @Test
    void onAPageOutsideOfAnyBlog() throws Exception
    {
        Document panel = renderPanel(new DocumentReference(WIKI, "Main", "Page"));

        assertEquals("Blog.Categories.WebHome?xpage=plain&sheet=Blog.CategoryRss", getRSSLink(panel));
        assertEquals("Blog.Categories.WebHome?action=manage", getManageLink(panel));
    }

    @Test
    void onAPostOfAnotherBlog() throws Exception
    {
        Document panel = renderPanel(new DocumentReference(WIKI, "SpaceB", "Post"));

        assertEquals("SpaceB.Categories.WebHome?xpage=plain&sheet=Blog.CategoryRss", getRSSLink(panel));
        assertEquals("/xwiki/bin/view/SpaceB/Categories/", getAllCategoriesLink(panel));
        assertEquals("SpaceB.Categories.WebHome?action=manage", getManageLink(panel));
    }

    @Test
    void onAPostOfABlogWithItsCategoriesElsewhere() throws Exception
    {
        Document panel = renderPanel(new DocumentReference(WIKI, "SpaceC", "Page"));

        assertEquals("CategoriesC.WebHome?xpage=plain&sheet=Blog.CategoryRss", getRSSLink(panel));
        assertEquals("CategoriesC.WebHome?action=manage", getManageLink(panel));
    }

    @Test
    void onACategoryPage() throws Exception
    {
        DocumentReference category = new DocumentReference(WIKI, Arrays.asList("SpaceB", "Categories"), "Sport");
        this.fixture.createCategory(category);

        Document panel = renderPanel(category);

        // The panel is about the space of the category, without looking for the blog.
        assertEquals("SpaceB.Categories.WebHome?xpage=plain&sheet=Blog.CategoryRss", getRSSLink(panel));
        assertEquals("SpaceB.Categories.WebHome?action=manage", getManageLink(panel));
    }

    @Test
    void onALegacyCategoryPage() throws Exception
    {
        DocumentReference category = new DocumentReference(WIKI, "Blog", "News");
        this.fixture.createCategory(category);

        Document panel = renderPanel(category);

        assertEquals("Blog.WebHome?xpage=plain&sheet=Blog.BlogRss", getRSSLink(panel));
        assertEquals("Blog.ManageCategories", getManageLink(panel));
    }

    private Document renderPanel(DocumentReference page) throws Exception
    {
        return Jsoup.parse(this.fixture.render(page, this.panelContent));
    }

    private String getRSSLink(Document panel)
    {
        return getWikiLinkTarget(panel.select("a[title=RSS]"));
    }

    private String getAllCategoriesLink(Document panel)
    {
        return panel.select(".blog-category-level .wikilink a").last().attr("href");
    }

    private String getManageLink(Document panel)
    {
        return getWikiLinkTarget(panel.select(".edit-categories a"));
    }

    /**
     * @return the target of a wiki link, as the link reference followed by its query string: the links to the pages
     *     that don't exist in the test wiki are not rendered as URLs
     */
    private String getWikiLinkTarget(Elements link)
    {
        String queryString = link.attr("data-xwiki-translated-attribute-querystring");
        return link.attr("href") + (queryString.isEmpty() ? "" : '?' + queryString);
    }
}
