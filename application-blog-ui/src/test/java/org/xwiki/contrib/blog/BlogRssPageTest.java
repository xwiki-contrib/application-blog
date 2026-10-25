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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.jodatime.JodaTimePlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.xwiki.contrib.blog.FeedFixture.BLOG;
import static org.xwiki.contrib.blog.FeedFixture.WIKI;

/**
 * Page test for the document {@code Blog.BlogRss}, the feed of a blog. In a page test the translations are displayed
 * as their key followed by their parameters.
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
class BlogRssPageTest extends PageTest
{
    private static final DocumentReference BLOG_CLASS = new DocumentReference(WIKI, BLOG, "BlogClass");

    private static final String BLOG_RSS = "Blog.BlogRss";

    private static final String TITLE = "title";

    private static final String DESCRIPTION = "description";

    private static final String WEB_HOME = "WebHome";

    /**
     * A text made of characters that have a meaning in XML or in wiki syntax.
     */
    private static final String SPECIAL_TEXT = "Fish & <Chips> {{/html}}**bold**";

    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @MockComponent
    private MacroCategoryManager macroCategoryManager;

    private FeedFixture fixture;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.getPluginManager().addPlugin("jodatime", JodaTimePlugin.class.getName(), this.context);
        FeedFixture.registerBlogScriptService(this.componentManager);
        FeedFixture.registerEmptyQueryService(this.componentManager);

        loadPage(BLOG_CLASS);
        loadPage(new DocumentReference(WIKI, BLOG, "BlogParameters"));
        loadPage(new DocumentReference(WIKI, BLOG, "BlogCode"));
        loadPage(new DocumentReference(WIKI, BLOG, "RssCode"));
        loadPage(new DocumentReference(WIKI, BLOG, "BlogRss"));

        this.fixture = new FeedFixture(this.xwiki, this.context);
        this.request.put("xpage", "plain");
    }

    @Test
    void feedOfTheBlogPage() throws Exception
    {
        DocumentReference blogReference = new DocumentReference(WIKI, BLOG, WEB_HOME);
        createBlog(blogReference, "My Blog");

        Document feed = this.fixture.renderFeed(blogReference, BLOG_RSS);

        assertEquals("Blog/", FeedFixture.getChannelPage(feed));
        assertEquals("My Blog", FeedFixture.getChannelText(feed, TITLE));
        assertEquals(this.xwiki.getDocument(blogReference, this.context).getExternalURL("view", this.context),
            FeedFixture.getChannelText(feed, "link"));
        assertEquals("blog.code.description.space [Blog]", FeedFixture.getChannelText(feed, DESCRIPTION));
    }

    @Test
    void feedOfABlogInANestedSpaceGivenInTheRequest() throws Exception
    {
        DocumentReference blogReference = new DocumentReference(WIKI, Arrays.asList("Parent", "Child"), WEB_HOME);
        createBlog(blogReference, "Nested Blog");
        this.request.put("blog", "Parent.Child.WebHome");

        Document feed = this.fixture.renderFeed(new DocumentReference(WIKI, "Main", WEB_HOME), BLOG_RSS);

        assertEquals("Parent/Child/", FeedFixture.getChannelPage(feed));
        assertEquals("Nested Blog", FeedFixture.getChannelText(feed, TITLE));
        assertEquals("blog.code.description.space [Parent.Child]", FeedFixture.getChannelText(feed, DESCRIPTION));
    }

    @Test
    void spaceNameWithSpecialCharactersIsDisplayedVerbatim() throws Exception
    {
        DocumentReference blogReference = new DocumentReference(WIKI, SPECIAL_TEXT, WEB_HOME);
        createBlog(blogReference, "Special");

        Document feed = this.fixture.renderFeed(blogReference, BLOG_RSS);

        assertEquals("blog.code.description.space [" + SPECIAL_TEXT + "]",
            FeedFixture.getChannelText(feed, DESCRIPTION));
        assertFalse(FeedFixture.containsElement(feed, "strong"));
    }

    @Test
    void blogTitleWithSpecialCharactersIsDisplayedVerbatim() throws Exception
    {
        DocumentReference blogReference = new DocumentReference(WIKI, "Special", WEB_HOME);
        createBlog(blogReference, SPECIAL_TEXT);

        Document feed = this.fixture.renderFeed(blogReference, BLOG_RSS);

        assertEquals(SPECIAL_TEXT, FeedFixture.getChannelText(feed, TITLE));
        assertFalse(FeedFixture.containsElement(feed, "strong"));
    }

    private void createBlog(DocumentReference reference, String title) throws Exception
    {
        XWikiDocument blogDocument = this.fixture.getDocument(reference);
        blogDocument.setTitle(title);
        blogDocument.newXObject(BLOG_CLASS, this.context).setStringValue(TITLE, title);
        this.fixture.save(blogDocument);
    }
}
