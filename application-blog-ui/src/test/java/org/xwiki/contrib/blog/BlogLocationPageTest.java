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
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.contrib.blog.script.BlogScriptService;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.util.XWikiSyntaxEscaper;
import org.xwiki.rendering.macro.MacroCategoryManager;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.plugin.jodatime.JodaTimePlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.blog.BlogLocationFixture.BLOG_B;
import static org.xwiki.contrib.blog.BlogLocationFixture.BLOG_C_CATEGORIES_LOCATION;
import static org.xwiki.contrib.blog.BlogLocationFixture.BLOG_C_POSTS_LOCATION;
import static org.xwiki.contrib.blog.BlogLocationFixture.BLOG_E;
import static org.xwiki.contrib.blog.BlogLocationFixture.BLOG_E_SPACE;
import static org.xwiki.contrib.blog.BlogLocationFixture.WIKI;

/**
 * Page test for the macros of {@code Blog.BlogCode} that decide which blog a page belongs to and where the posts and
 * the categories of a blog live. Only the rules that don't depend on a query result are covered here: in a page test
 * every query returns no result.
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
class BlogLocationPageTest extends PageTest
{
    /**
     * The {@code #hierarchy} macro of the platform templates, which displays the blog of a post, replaced by the
     * reference it is called with.
     */
    private static final String HIERARCHY_STUB = "#macro(hierarchy $reference $options)"
        + "<span class=\"hierarchy\">$services.model.serialize($reference, 'local')</span>#end\n";

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
        loadPage(BlogLocationFixture.BLOG_POST_CLASS);
        loadPage(BlogLocationFixture.ENABLE_PANELS_CLASS);
        loadPage(new DocumentReference(WIKI, "Blog", "BlogParameters"));
        loadPage(new DocumentReference(WIKI, "Blog", "BlogCode"));

        this.fixture = new BlogLocationFixture(this.xwiki, this.context);
        this.fixture.createBlogs();
    }

    @Test
    void getBlogDocumentReturnsTheBlogConfiguredInTheWebPreferences() throws Exception
    {
        this.fixture.configurePanelsBlog(new DocumentReference(WIKI, "Elsewhere", "WebPreferences"),
            "SpaceB.WebHome");

        assertEquals("SpaceB.WebHome", getBlogDocument(new DocumentReference(WIKI, "Elsewhere", "Page")));
    }

    @Test
    void getBlogDocumentPrefersTheBlogConfiguredInTheWebPreferencesToTheBlogOfTheSpace() throws Exception
    {
        this.fixture.configurePanelsBlog(new DocumentReference(WIKI, "SpaceD", "WebPreferences"), "SpaceB.WebHome");

        assertEquals("SpaceB.WebHome", getBlogDocument(new DocumentReference(WIKI, "SpaceD", "Post")));
    }

    static Stream<Arguments> blogDocuments()
    {
        return Stream.of(
            // A: a whole-space blog, found through its WebHome.
            Arguments.of(new DocumentReference(WIKI, "Blog", "Post"), "Blog.WebHome"),
            // B
            Arguments.of(new DocumentReference(WIKI, "SpaceB", "Post"), "SpaceB.WebHome"),
            // The blog document itself.
            Arguments.of(BLOG_B, "SpaceB.WebHome"),
            // D: an in-space blog, found through the Blog page of the space.
            Arguments.of(new DocumentReference(WIKI, "SpaceD", "Post"), "SpaceD.Blog"),
            // E: a nested blog with special characters in its space name.
            Arguments.of(new DocumentReference("Post", BLOG_E.getLastSpaceReference()),
                BLOG_E_SPACE + ".WebHome"),
            // A page outside of any blog falls back to the default blog.
            Arguments.of(new DocumentReference(WIKI, "Main", "Page"), "Blog.WebHome"),
            // So does a page of a categories space, including the ones of another blog.
            Arguments.of(new DocumentReference(WIKI, Arrays.asList("Blog", "Categories"), "News"), "Blog.WebHome"),
            Arguments.of(new DocumentReference(WIKI, Arrays.asList("SpaceB", "Categories"), "Sport"), "Blog.WebHome")
        );
    }

    @ParameterizedTest
    @MethodSource("blogDocuments")
    void getBlogDocument(DocumentReference page, String expectedBlog) throws Exception
    {
        assertEquals(expectedBlog, getBlogDocument(page));
    }

    @Test
    void getBlogDocumentPrefersTheWebHomeToTheBlogPageOfTheSpace() throws Exception
    {
        this.fixture.createBlog(new DocumentReference(WIKI, "SpaceF", "Blog"), "", "");
        this.fixture.createBlog(new DocumentReference(WIKI, "SpaceF", "WebHome"), "", "");

        assertEquals("SpaceF.WebHome", getBlogDocument(new DocumentReference(WIKI, "SpaceF", "Post")));
    }

    static Stream<Arguments> postsLocations()
    {
        return Stream.of(
            // A: no stored location, the posts are in the space of the blog.
            Arguments.of(new DocumentReference(WIKI, "Blog", "Post"), "Blog"),
            // B: stored location.
            Arguments.of(new DocumentReference(WIKI, "SpaceB", "Post"), "SpaceB"),
            // C: stored location, outside of the blog space.
            Arguments.of(new DocumentReference(WIKI, "SpaceC", "Page"), BLOG_C_POSTS_LOCATION),
            // D: no stored location, the posts are in the space of the Blog page.
            Arguments.of(new DocumentReference(WIKI, "SpaceD", "Post"), "SpaceD"),
            // E
            Arguments.of(new DocumentReference("Post", BLOG_E.getLastSpaceReference()), BLOG_E_SPACE),
            // A page outside of any blog gets the location of the default blog.
            Arguments.of(new DocumentReference(WIKI, "Main", "Page"), "Blog")
        );
    }

    @ParameterizedTest
    @MethodSource("postsLocations")
    void getBlogPostsLocation(DocumentReference page, String expectedLocation) throws Exception
    {
        assertEquals(expectedLocation,
            evaluate(page, "#getBlogPostsLocation($doc.space $location)#print($location)"));
    }

    static Stream<Arguments> categoriesLocations()
    {
        DocumentReference postA = new DocumentReference(WIKI, "Blog", "Post");
        DocumentReference postB = new DocumentReference(WIKI, "SpaceB", "Post");
        DocumentReference pageC = new DocumentReference(WIKI, "SpaceC", "Page");
        DocumentReference postD = new DocumentReference(WIKI, "SpaceD", "Post");
        DocumentReference postE = new DocumentReference("Post", BLOG_E.getLastSpaceReference());
        return Stream.of(
            // A, once the categories have been migrated to Blog.Categories (or on a fresh install).
            Arguments.of(postA, false, "Blog.Categories"),
            // A, while blog posts are still assigned to categories of the legacy Blog space.
            Arguments.of(postA, true, "Blog"),
            // B, C: the stored location wins, whatever the legacy state.
            Arguments.of(postB, false, "SpaceB.Categories"),
            Arguments.of(postB, true, "SpaceB.Categories"),
            Arguments.of(pageC, false, BLOG_C_CATEGORIES_LOCATION),
            // D, E: without a stored location, a blog shares the categories of the default blog.
            Arguments.of(postD, false, "Blog.Categories"),
            Arguments.of(postD, true, "Blog"),
            Arguments.of(postE, false, "Blog.Categories")
        );
    }

    @ParameterizedTest
    @MethodSource("categoriesLocations")
    void getBlogCategoriesLocation(DocumentReference page, boolean legacyCategories, String expectedLocation)
        throws Exception
    {
        when(this.blogScriptService.hasLegacyCategoryAssignments()).thenReturn(legacyCategories);

        assertEquals(expectedLocation,
            evaluate(page, "#getBlogCategoriesLocation($doc.space $location)#print($location)"));
    }

    @Test
    void getBlogCategoriesLocationClearsThePostsLocationOfTheCaller() throws Exception
    {
        String result = evaluate(new DocumentReference(WIKI, "SpaceC", "Page"),
            "#getBlogPostsLocation($doc.space $postsLocation)"
                + "#getBlogCategoriesLocation($doc.space $categoriesLocation)"
                + "#print(\"$!postsLocation|$!categoriesLocation\")");

        // This is a bug: #getBlogCategoriesLocation sets $postsLocation to null instead of only setting its own
        // output parameter, so a caller that keeps the posts location in $postsLocation loses it. It should be
        // "PostsC|CategoriesC". See BLOG-278: Getting the categories location of a blog clears the posts location
        // computed by the calling script.
        assertEquals("|" + BLOG_C_CATEGORIES_LOCATION, result);
    }

    @Test
    void displayEntryBlogLocationLinksToTheDefaultBlog() throws Exception
    {
        this.fixture.createPost(new DocumentReference(WIKI, "Blog", "Post"));

        Elements blogLinks = displayEntryBlogLocation(new DocumentReference(WIKI, "Blog", "Post"));

        assertEquals(1, blogLinks.size(), blogLinks.outerHtml());
        assertEquals("Blog.WebHome", blogLinks.select(".hierarchy").text());
    }

    @Test
    void displayEntryBlogLocationLinksToAWholeSpaceBlog() throws Exception
    {
        this.fixture.createPost(new DocumentReference(WIKI, "SpaceB", "Post"));

        Elements blogLinks = displayEntryBlogLocation(new DocumentReference(WIKI, "SpaceB", "Post"));

        assertEquals(1, blogLinks.size(), blogLinks.outerHtml());
        assertEquals("SpaceB.WebHome", blogLinks.select(".hierarchy").text());
    }

    @Test
    void displayEntryBlogLocationDisplaysNothingForAnInSpaceBlog() throws Exception
    {
        this.fixture.createPost(new DocumentReference(WIKI, "SpaceD", "Post"));

        Elements blogLinks = displayEntryBlogLocation(new DocumentReference(WIKI, "SpaceD", "Post"));

        // This is a bug: the link is built from the posts location ("SpaceD.WebHome") instead of the blog document,
        // and since SpaceD.WebHome is not a blog nothing is displayed. It should say the post is in SpaceD.Blog.
        // See BLOG-279: The "Posted in" blog link is missing for the posts of a blog whose page is not the WebHome of
        // its space.
        assertEquals(0, blogLinks.size(), blogLinks.outerHtml());
    }

    @Test
    void displayEntryBlogLocationDisplaysNothingOnTheBlogOfThePost() throws Exception
    {
        this.fixture.createPost(new DocumentReference(WIKI, "SpaceB", "Post"));

        Elements blogLinks = displayEntryBlogLocation(BLOG_B, new DocumentReference(WIKI, "SpaceB", "Post"));

        assertEquals(0, blogLinks.size(), blogLinks.outerHtml());
    }

    private Elements displayEntryBlogLocation(DocumentReference post) throws Exception
    {
        return displayEntryBlogLocation(new DocumentReference(WIKI, "Main", "Page"), post);
    }

    private Elements displayEntryBlogLocation(DocumentReference page, DocumentReference post) throws Exception
    {
        String html = this.fixture.render(page, "{{include reference=\"Blog.BlogCode\"/}}\n\n"
            + "{{velocity}}" + HIERARCHY_STUB
            + "#set ($entryDoc = $xwiki.getDocument('" + post.getLastSpaceReference().getName() + '.' + post.getName()
            + "'))"
            + "#displayEntryBlogLocation($entryDoc $entryDoc.getObject('Blog.BlogPostClass')){{/velocity}}");
        return Jsoup.parse(html).select(".blogrefpath");
    }

    private String getBlogDocument(DocumentReference page) throws Exception
    {
        return evaluate(page, "#getBlogDocument($doc.space $blogDoc)#print($blogDoc.fullName)");
    }

    /**
     * Renders the given Velocity code, after including {@code Blog.BlogCode}, as the content of the given page.
     *
     * @param page the page whose content is rendered, created if needed
     * @param velocity the Velocity code to execute; {@code #print($value)} outputs a value as is
     * @return the plain text result
     */
    private String evaluate(DocumentReference page, String velocity) throws Exception
    {
        setOutputSyntax(Syntax.PLAIN_1_0);
        return this.fixture.render(page, "{{include reference=\"Blog.BlogCode\"/}}\n\n"
            + "{{velocity}}#macro(print $value)$services.rendering.escape(\"$!value\", 'xwiki/2.1')#end\n"
            + velocity + "{{/velocity}}").trim();
    }
}
