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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.renderer.html5.HTML5BlockRenderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5Renderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5RendererFactory;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageTypeRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkTypeRenderer;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BlogScriptService}.
 *
 * @version $Id$
 */
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
public class BlogScriptServiceTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "TestBlog", "HelloWorld");

    @Rule
    public MockitoComponentMockingRule<BlogScriptService> mocker =
        new MockitoComponentMockingRule<>(BlogScriptService.class);

    BlogScriptService blogScriptService;

    private ContextualLocalizationManager localizationManager;

    private XWikiContext xwikiContext;

    private Document blogDocument;

    private Object blogPostObject;

    private Property extractProperty;

    @Before
    public void setUp() throws Exception
    {
        this.blogDocument = mock(Document.class);
        when(this.blogDocument.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        this.blogPostObject = mock(Object.class);
        this.extractProperty = mock(Property.class);
        when(this.blogPostObject.getProperty("extract")).thenReturn(this.extractProperty);
        Provider<XWikiContext> contextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        this.blogScriptService = this.mocker.getComponentUnderTest();
        this.localizationManager = this.mocker.getInstance(ContextualLocalizationManager.class);
        this.xwikiContext = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(this.xwikiContext);
        Utils.setComponentManager(this.mocker);
        this.mocker.registerMockComponent(EntityResourceActionLister.class);
        this.mocker.registerComponent(ComponentManager.class, "context", this.mocker);

        XWiki mockXWiki = mock(XWiki.class);
        when(this.xwikiContext.getWiki()).thenReturn(mockXWiki);
        when(mockXWiki.getWebAppPath(this.xwikiContext)).thenReturn("xwiki/");
        when(this.xwikiContext.getURL()).thenReturn(new URL("https://www.example.com/xwiki/bin/view/Test"));
        when(this.xwikiContext.getRequest()).thenReturn(mock(XWikiRequest.class));
    }

    @Test
    public void testRenderContentHTMLWithExtractAndRemoveEllipsis()
    {
        when(this.extractProperty.getValue()).thenReturn("Test Extract");
        when(this.blogDocument.display("extract", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Test Extract</p>{{/html}}");

        String result = this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, true, false);

        assertEquals("<p>Test Extract</p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    public void testRenderContentHTMLWithoutExtractAndRemoveEllipsis()
    {
        when(this.blogPostObject.getProperty("extract")).thenReturn(null);
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Full Content</p>{{/html}}");

        String result = this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, true, false);

        assertEquals("<p>Full Content</p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    public void testRenderContentHTMLWithExtractAndEllipsis()
    {
        when(this.extractProperty.getValue()).thenReturn("Test Extract");
        when(this.blogDocument.display("extract", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Test Extract</p>{{/html}}");
        when(this.localizationManager.getTranslationPlain("blog.code.readpost")).thenReturn("Read more");

        String result = this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, true, false, false);

        assertEquals(
            "<p>Test Extract <span class=\"wikiexternallink\">"
                + "<a title=\"Read more\" href=\"wiki:TestBlog.HelloWorld\">…</a></span></p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    public void testRenderContentHTMLWithoutExtractAndEllipsis()
    {
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Full Content</p>{{/html}}");

        String result = this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, false, false, false);

        assertEquals("<p>Full Content</p>", result);
        verify(this.xwikiContext, never()).setURLFactory(any());
    }

    @Test
    public void testRenderContentHTMLWithExternalURLs()
    {
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(this.xwikiContext.getURLFactory()).thenReturn(urlFactory);
        when(this.blogDocument.display("content", "view", this.blogPostObject)).thenReturn(
            "{{html clean=\"false\" wiki=\"false\"}}<p>Full Content</p>{{/html}}");

        String result = this.blogScriptService.renderContentHTML(this.blogDocument, this.blogPostObject, false, false, true);

        assertEquals("<p>Full Content</p>", result);
        verify(this.xwikiContext).setURLFactory(any(ExternalServletURLFactory.class));
        verify(this.xwikiContext).setURLFactory(urlFactory);
    }
}
