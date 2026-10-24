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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.jodatime.JodaTimePlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Page test for the document {@code Blog.BlogCode}.
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
class BlogCodePageTest extends PageTest
{
    private static final DocumentReference BLOG_CLASS_REFERENCE = new DocumentReference("xwiki", "Blog", "BlogClass");

    private static final String BLOG_PAGE_NAME = "Blog]] [[Other]] [[X";

    private static final String WIKI = "xwiki";

    private static final String BLOG = "Blog";

    private static final String BLOG_POST_CLASS = "Blog.BlogPostClass";

    private static final DocumentReference POST_REFERENCE = new DocumentReference(WIKI, BLOG, "Post");

    /**
     * A text made of characters that have a meaning in HTML or in wiki syntax, including the end of an HTML macro.
     */
    private static final String SPECIAL_TEXT = "Fish & \"Chips\" l'été <b>bold</b> **x** {{/html}}";

    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @MockComponent
    private MacroCategoryManager macroCategoryManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.getPluginManager().addPlugin("jodatime", JodaTimePlugin.class.getName(), this.context);

        loadPage(BLOG_CLASS_REFERENCE);
        loadPage(new DocumentReference("xwiki", "Blog", "BlogParameters"));
        loadPage(new DocumentReference("xwiki", "Blog", "BlogCode"));
        loadPage(new DocumentReference(WIKI, BLOG, "BlogPostClass"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "weekly", "monthly" })
    void displayNavigationLinksWithSpecialCharactersInPageName(String displayType) throws Exception
    {
        DocumentReference blogReference = new DocumentReference("xwiki", "Sandbox", BLOG_PAGE_NAME);
        XWikiDocument blogDocument = new XWikiDocument(blogReference);
        blogDocument.setSyntax(Syntax.XWIKI_2_1);
        blogDocument.setContent("{{include reference=\"Blog.BlogCode\"/}}\n\n"
            + "{{velocity}}#displayNavigationLinks($doc){{/velocity}}");
        BaseObject blogObject = blogDocument.newXObject(BLOG_CLASS_REFERENCE, this.context);
        blogObject.setStringValue("displayType", displayType);
        this.xwiki.saveDocument(blogDocument, this.context);

        this.context.setDoc(blogDocument);
        Document document = renderHTMLPage(blogDocument);

        Elements links = document.select(".pagingLinks a");
        assertEquals(2, links.size(), document.html());
        for (Element link : links) {
            assertEquals(BLOG_PAGE_NAME, link.attr("href"));
        }
    }

    @ParameterizedTest
    @CsvSource({ "true, true", "true, false", "false, true", "false, false" })
    void displayEntryTitleWithSpecialCharacters(boolean insideHTML, boolean listed) throws Exception
    {
        XWikiDocument postDocument = createPost();

        XWikiDocument document = postDocument;
        if (listed) {
            document = new XWikiDocument(new DocumentReference(WIKI, BLOG, "Listing"));
        }
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent(getContent(insideHTML,
            "#set ($postDoc = $xwiki.getDocument('Blog.Post'))#displayEntryTitle($postDoc $postDoc.getObject('"
                + BLOG_POST_CLASS + "'))"));
        this.xwiki.saveDocument(document, this.context);

        this.context.setDoc(document);
        Document html = renderHTMLPage(document);

        Elements titles = html.select(".entry-title");
        assertEquals(1, titles.size(), html.html());
        Element title = titles.first();
        assertTrue(title.select("b, strong").isEmpty(), title.html());
        if (listed) {
            Elements links = title.select("a");
            assertEquals(1, links.size(), title.html());
            assertEquals(SPECIAL_TEXT, links.first().text());
            assertEquals(this.xwiki.getURL(POST_REFERENCE, "view", this.context), links.first().attr("href"));
        } else {
            assertEquals(SPECIAL_TEXT, title.text());
        }
    }

    private XWikiDocument createPost() throws Exception
    {
        XWikiDocument postDocument = new XWikiDocument(POST_REFERENCE);
        BaseObject postObject =
            postDocument.newXObject(new DocumentReference(WIKI, BLOG, "BlogPostClass"), this.context);
        postObject.setStringValue("title", SPECIAL_TEXT);
        this.xwiki.saveDocument(postDocument, this.context);
        return this.xwiki.getDocument(POST_REFERENCE, this.context);
    }

    /**
     * @param insideHTML whether the macro is called inside an HTML macro that renders wiki syntax, as the image
     *     layout and the weekly and monthly displays of a blog do
     * @param macroCall the call of the Velocity macro to test
     * @return the content of a page calling the given Velocity macro
     */
    private String getContent(boolean insideHTML, String macroCall)
    {
        String call = macroCall;
        if (insideHTML) {
            call = "{{html clean=\"false\" wiki=\"true\"}}\n" + macroCall + "\n{{/html}}";
        }
        return "{{include reference=\"Blog.BlogCode\"/}}\n\n{{velocity}}\n" + call + "\n{{/velocity}}";
    }
}
