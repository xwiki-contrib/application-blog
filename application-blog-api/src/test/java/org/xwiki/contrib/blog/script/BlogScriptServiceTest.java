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
package org.xwiki.contrib.blog.script;

import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.blog.internal.CategoryLocationMigration;
import org.xwiki.rendering.internal.renderer.html5.HTML5BlockRenderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5Renderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5RendererFactory;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageTypeRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkTypeRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BlogScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
@ReferenceComponentList
@ComponentList({
    HTML5BlockRenderer.class,
    HTML5Renderer.class,
    HTML5RendererFactory.class,
    DefaultXHTMLLinkRenderer.class,
    DefaultXHTMLLinkTypeRenderer.class,
    DefaultXHTMLImageRenderer.class,
    DefaultXHTMLImageTypeRenderer.class
})
class BlogScriptServiceTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "TestBlog", "HelloWorld");

    private static final String HTML_MACRO_START = "{{html clean=\"false\" wiki=\"false\"}}";

    private static final String HTML_MACRO_END = "{{/html}}";

    private static final String ELLIPSIS =
        "<span class=\"wikiexternallink\"><a title=\"Read more\" href=\"wiki:TestBlog.HelloWorld\">…</a></span>";

    @InjectMockComponents
    private BlogScriptService blogScriptService;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private CategoryLocationMigration categoryLocationMigration;

    @MockComponent
    private EntityResourceActionLister entityResourceActionLister;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private XWikiContext xwikiContext;

    private Document blogDocument;

    private Object blogPostObject;

    private Property extractProperty;

    @BeforeEach
    void setUp() throws Exception
    {
        this.blogDocument = mock(Document.class);
        when(this.blogDocument.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        this.blogPostObject = mock(Object.class);
        this.extractProperty = mock(Property.class);
        when(this.blogPostObject.getProperty("extract")).thenReturn(this.extractProperty);
        this.xwikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.xwikiContext);
        Utils.setComponentManager(this.componentManager);
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        XWiki mockXWiki = mock(XWiki.class);
        when(this.xwikiContext.getWiki()).thenReturn(mockXWiki);
        when(mockXWiki.getWebAppPath(this.xwikiContext)).thenReturn("xwiki/");
        when(this.xwikiContext.getURL()).thenReturn(new URL("https://www.example.com/xwiki/bin/view/Test"));
        when(this.xwikiContext.getRequest()).thenReturn(mock(XWikiRequest.class));

        when(this.localizationManager.getTranslationPlain("blog.code.readpost")).thenReturn("Read more");
    }

    private void mockExtract(String extract)
    {
        when(this.extractProperty.getValue()).thenReturn(extract);
        when(this.blogDocument.display("extract", "view", this.blogPostObject))
            .thenReturn(HTML_MACRO_START + extract + HTML_MACRO_END);
    }

    private void mockContent(String content)
    {
        when(this.blogDocument.display("content", "view", this.blogPostObject))
            .thenReturn(HTML_MACRO_START + content + HTML_MACRO_END);
    }

    @Test
    void renderContentHTMLWithExtractAndRemoveEllipsis()
    {
        when(this.extractProperty.getValue()).thenReturn("Test Extract");
        when(this.blogDocument.display("extract", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Test Extract</p>{{/html}}");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, true, false);

        assertEquals("<p>Test Extract</p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    void renderContentHTMLWithoutExtractAndRemoveEllipsis()
    {
        when(this.blogPostObject.getProperty("extract")).thenReturn(null);
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Full Content</p>{{/html}}");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, true, false);

        assertEquals("<p>Full Content</p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    void renderContentHTMLWithExtractAndEllipsis()
    {
        when(this.extractProperty.getValue()).thenReturn("Test Extract");
        when(this.blogDocument.display("extract", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Test Extract</p>{{/html}}");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        assertEquals("<p>Test Extract " + ELLIPSIS + "</p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    void renderContentHTMLWithoutExtractAndEllipsis()
    {
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Full Content</p>{{/html}}");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, false, false, false);

        assertEquals("<p>Full Content</p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    void renderContentHTMLWithExternalURLs()
    {
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(this.xwikiContext.getURLFactory()).thenReturn(urlFactory);
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Full Content</p>{{/html}}");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, false, false, true);

        assertEquals("<p>Full Content</p>", result);
        verify(this.xwikiContext).setURLFactory(any(ExternalServletURLFactory.class));
        verify(this.xwikiContext).setURLFactory(urlFactory);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void renderContentHTMLWithBlankExtractUsesContent(String extract)
    {
        mockExtract(extract);
        mockContent("<p>Full Content</p>");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        // The ellipsis is only added to an extract, never to the full content.
        assertEquals("<p>Full Content</p>", result);
        verify(this.blogDocument, never()).display("extract", "view", this.blogPostObject);
    }

    @Test
    void renderContentHTMLWithNullExtractValueUsesContent()
    {
        when(this.extractProperty.getValue()).thenReturn(null);
        mockContent("<p>Full Content</p>");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        assertEquals("<p>Full Content</p>", result);
    }

    @Test
    void renderContentHTMLWithExtractAndEllipsisWhenExtractIsNotAParagraph()
    {
        mockExtract("<ul><li>Test Extract</li></ul>");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        assertEquals("<ul><li>Test Extract</li></ul><p>" + ELLIPSIS + "</p>", result);
    }

    @Test
    void renderContentHTMLWithExtractAndEllipsisWhenLastParagraphIsFollowedByWhitespace()
    {
        mockExtract("<p>First</p><p>Second</p>\n  ");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        assertEquals("<p>First</p><p>Second " + ELLIPSIS + "</p>", result);
    }

    @Test
    void renderContentHTMLWithExtractAndEllipsisWhenLastParagraphIsFollowedByContent()
    {
        mockExtract("<p>Test Extract</p><ul><li>Item</li></ul>");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        assertEquals("<p>Test Extract</p><ul><li>Item</li></ul><p>" + ELLIPSIS + "</p>", result);
    }

    @Test
    void renderContentHTMLWithExtractAndEllipsisWhenExtractIsPlainText()
    {
        mockExtract("Test Extract");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        assertEquals("Test Extract<p>" + ELLIPSIS + "</p>", result);
    }

    @Test
    void renderContentHTMLWithContentNotWrappedInHTMLMacro()
    {
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenReturn("<p>Full Content</p>");

        String result =
            this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, false, false, false);

        assertEquals("<p>Full Content</p>", result);
    }

    @Test
    void renderContentHTMLWithExternalURLsRestoresURLFactoryOnFailure()
    {
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(this.xwikiContext.getURLFactory()).thenReturn(urlFactory);
        RuntimeException exception = new RuntimeException("Display failed");
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, false, false,
                true));

        assertSame(exception, thrown);
        InOrder inOrder = inOrder(this.xwikiContext);
        inOrder.verify(this.xwikiContext).setURLFactory(any(ExternalServletURLFactory.class));
        inOrder.verify(this.xwikiContext).setURLFactory(urlFactory);
    }

    @Test
    void renderRSSDescription() throws Exception
    {
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(this.xwikiContext.getURLFactory()).thenReturn(urlFactory);
        when(this.blogDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(this.blogDocument.getRenderedContent("**Content**", "xwiki/2.1"))
            .thenReturn("<p><strong>Content</strong></p>");

        assertEquals("<p><strong>Content</strong></p>",
            this.blogScriptService.renderRSSDescription("**Content**", this.blogDocument));

        // The content is rendered with external URLs, then the original URL factory is restored.
        InOrder inOrder = inOrder(this.xwikiContext, this.blogDocument);
        inOrder.verify(this.xwikiContext).setURLFactory(any(ExternalServletURLFactory.class));
        inOrder.verify(this.blogDocument).getRenderedContent("**Content**", "xwiki/2.1");
        inOrder.verify(this.xwikiContext).setURLFactory(urlFactory);
    }

    @Test
    void renderRSSDescriptionRestoresURLFactoryOnFailure() throws Exception
    {
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(this.xwikiContext.getURLFactory()).thenReturn(urlFactory);
        when(this.blogDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        XWikiException exception = new XWikiException(0, 0, "Rendering failed");
        when(this.blogDocument.getRenderedContent("**Content**", "xwiki/2.1")).thenThrow(exception);

        XWikiException thrown = assertThrows(XWikiException.class,
            () -> this.blogScriptService.renderRSSDescription("**Content**", this.blogDocument));

        assertSame(exception, thrown);
        InOrder inOrder = inOrder(this.xwikiContext);
        inOrder.verify(this.xwikiContext).setURLFactory(any(ExternalServletURLFactory.class));
        inOrder.verify(this.xwikiContext).setURLFactory(urlFactory);
    }

    @Test
    void getExternalAttachmentURL()
    {
        XWikiDocument xwikiDocument = mock(XWikiDocument.class);
        when(this.blogDocument.getDocument()).thenReturn(xwikiDocument);
        when(xwikiDocument.getExternalAttachmentURL("image.png", "download", this.xwikiContext))
            .thenReturn("https://www.example.com/xwiki/bin/download/TestBlog/HelloWorld/image.png");

        assertEquals("https://www.example.com/xwiki/bin/download/TestBlog/HelloWorld/image.png",
            this.blogScriptService.getExternalAttachmentURL(this.blogDocument, "image.png"));
    }

    @Test
    void migrateCategoryLocation()
    {
        this.blogScriptService.migrateCategoryLocation("subwiki");

        verify(this.categoryLocationMigration).migrate("subwiki");
    }

    @Test
    void hasLegacyCategoryAssignments()
    {
        assertFalse(this.blogScriptService.hasLegacyCategoryAssignments());

        when(this.categoryLocationMigration.hasLegacyCategoryAssignments()).thenReturn(true);

        assertTrue(this.blogScriptService.hasLegacyCategoryAssignments());
    }
}
