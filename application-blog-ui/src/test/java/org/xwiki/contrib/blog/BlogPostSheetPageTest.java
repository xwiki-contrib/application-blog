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

import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.contrib.blog.script.BlogScriptService;
import org.xwiki.edit.EditConfiguration;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorManager;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.blog.BlogLocationFixture.WIKI;

/**
 * Page test for the edit mode of the document {@code Blog.BlogPostSheet}: the category a new post gets when no category
 * is selected, and the parent of a category created from the post editor when no parent is selected. The categories
 * listed in the editor come from a query and are not covered here.
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
class BlogPostSheetPageTest extends PageTest
{
    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @MockComponent
    private MacroCategoryManager macroCategoryManager;

    @MockComponent
    private EditConfiguration editConfiguration;

    @MockComponent
    private EditorManager editorManager;

    private BlogScriptService blogScriptService;

    private BlogLocationFixture fixture;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.getPluginManager().addPlugin("jodatime", JodaTimePlugin.class.getName(), this.context);
        this.blogScriptService = BlogLocationFixture.registerBlogScriptService(this.componentManager);
        BlogLocationFixture.registerEmptyQueryService(this.componentManager);
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("edit"), any(), any(), any())).thenReturn(true);
        // The text areas of the post (content, summary) are displayed with an editor that is not part of the test.
        Editor<?> editor = mock(Editor.class);
        doReturn(editor).when(this.editorManager).getDefaultEditor(any(), any());
        when(editor.render(any(), any())).thenReturn("");

        loadPage(BlogLocationFixture.BLOG_CLASS);
        loadPage(BlogLocationFixture.BLOG_POST_CLASS);
        loadPage(BlogLocationFixture.CATEGORY_CLASS);
        loadPage(new DocumentReference(WIKI, "Blog", "BlogParameters"));
        loadPage(new DocumentReference(WIKI, "Blog", "BlogCode"));
        loadPage(new DocumentReference(WIKI, "Blog", "CategoriesCode"));
        loadPage(new DocumentReference(WIKI, "Blog", "BlogPostSheet"));

        this.fixture = new BlogLocationFixture(this.xwiki, this.context);
        this.fixture.createBlogs();

        this.context.setAction("edit");
        // Ask for the form to add a category, displayed below the categories of the post.
        this.request.put("xaction", "showAddCategory");
    }

    static Stream<Arguments> defaultCategories()
    {
        return Stream.of(
            // A, once the categories have been migrated to Blog.Categories (or on a fresh install).
            Arguments.of("Blog", false, "Blog.Categories.WebHome", "Blog.Categories.WebHome"),
            // A, while blog posts are still assigned to categories of the legacy Blog space: the legacy categories
            // root is the Blog.Categories page.
            Arguments.of("Blog", true, "Blog.Categories", "Blog.Categories"),
            // B: the root of its own categories space.
            Arguments.of("SpaceB", false, "SpaceB.Categories.WebHome", "SpaceB.Categories.WebHome"),
            Arguments.of("SpaceB", true, "SpaceB.Categories.WebHome", "SpaceB.Categories.WebHome"),
            // C: its categories are elsewhere.
            Arguments.of("SpaceC", false, "CategoriesC.WebHome", "CategoriesC.WebHome"),
            // D: it shares the categories of the default blog.
            Arguments.of("SpaceD", false, "Blog.Categories.WebHome", "Blog.Categories.WebHome")
        );
    }

    @ParameterizedTest
    @MethodSource("defaultCategories")
    void defaultCategoryAndDefaultCategoryParent(String space, boolean legacyCategories,
        String expectedDefaultPostCategory, String expectedDefaultCategoryParent) throws Exception
    {
        when(this.blogScriptService.hasLegacyCategoryAssignments()).thenReturn(legacyCategories);
        DocumentReference post = new DocumentReference(WIKI, space, "Post");
        this.fixture.createPost(post);

        Document editor = Jsoup.parse(this.fixture.render(post, "{{include reference=\"Blog.BlogPostSheet\"/}}"));

        // The category the post gets when no category is selected.
        assertEquals(expectedDefaultPostCategory, editor.select("#blogCategoriesWebHome").val());
        // The parent of a new category when no parent is selected ("None").
        assertEquals(expectedDefaultCategoryParent,
            editor.select("#blog_category_selectBox option[selected]").val(), editor.html());
    }
}
