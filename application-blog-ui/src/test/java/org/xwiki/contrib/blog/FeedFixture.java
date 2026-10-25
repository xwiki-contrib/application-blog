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

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.mockito.Answers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xwiki.contrib.blog.script.BlogScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Renders, in the in-memory wiki of a page test, the feeds displayed with {@code Blog.RssCode} and reads them as XML.
 * The queries of a page test return no result, so the feeds have no items.
 *
 * @version $Id$
 */
final class FeedFixture
{
    static final String WIKI = "xwiki";

    static final String BLOG = "Blog";

    private static final String RSS_NAMESPACE = "http://purl.org/rss/1.0/";

    private static final String CHANNEL = "channel";

    private final XWiki xwiki;

    private final XWikiContext context;

    FeedFixture(XWiki xwiki, XWikiContext context)
    {
        this.xwiki = xwiki;
        this.context = context;
    }

    /**
     * Registers a mock of the blog script service ({@code $services.blog}), which answers that the wiki has no blog
     * post assigned to a legacy category.
     *
     * @param componentManager the component manager of the test
     * @throws Exception in case of errors
     */
    static void registerBlogScriptService(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ScriptService.class, "blog", mock(BlogScriptService.class));
    }

    /**
     * Registers a query script service ({@code $services.query}) whose queries can be built and executed, and return no
     * result.
     *
     * @param componentManager the component manager of the test
     * @throws Exception in case of errors
     */
    static void registerEmptyQueryService(MockitoComponentManager componentManager) throws Exception
    {
        QueryManagerScriptService queryService = mock(QueryManagerScriptService.class);
        when(queryService.hql(any())).thenAnswer(invocation -> mock(ScriptQuery.class, Answers.RETURNS_SELF));
        when(queryService.xwql(any())).thenAnswer(invocation -> mock(ScriptQuery.class, Answers.RETURNS_SELF));
        when(queryService.createQuery(any(), any()))
            .thenAnswer(invocation -> mock(ScriptQuery.class, Answers.RETURNS_SELF));
        componentManager.registerComponent(ScriptService.class, "query", queryService);
    }

    /**
     * @param reference the reference of the page
     * @return the page, not saved yet, with its objects if it exists already
     * @throws Exception in case of errors
     */
    XWikiDocument getDocument(DocumentReference reference) throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(reference, this.context).clone();
        document.setSyntax(Syntax.XWIKI_2_1);
        return document;
    }

    /**
     * @param document the page to save
     * @throws Exception in case of errors
     */
    void save(XWikiDocument document) throws Exception
    {
        this.xwiki.saveDocument(document, this.context);
    }

    /**
     * Displays a page with the given feed sheet, as {@code ?xpage=plain&sheet=<sheet>} does, and reads the result as
     * XML.
     *
     * @param reference the page on which the feed sheet is applied, whose objects are kept if it exists already
     * @param sheet the feed sheet, e.g. {@code Blog.BlogRss}
     * @return the feed
     * @throws Exception in case of errors
     */
    Document renderFeed(DocumentReference reference, String sheet) throws Exception
    {
        XWikiDocument document = getDocument(reference);
        document.setContent("{{include reference=\"" + sheet + "\"/}}");
        save(document);
        this.context.setDoc(document);
        String output = document.getRenderedContent(this.context).trim();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(null);
        try {
            return builder.parse(new InputSource(new StringReader(output)));
        } catch (SAXException e) {
            return fail("The feed is not well-formed XML: " + e.getMessage() + "\n" + output);
        }
    }

    /**
     * @param feed a feed
     * @return the channel of the feed
     */
    static Element getChannel(Document feed)
    {
        NodeList channels = feed.getElementsByTagNameNS(RSS_NAMESPACE, CHANNEL);
        if (channels.getLength() != 1) {
            fail("Expected one channel, found " + channels.getLength());
        }
        return (Element) channels.item(0);
    }

    /**
     * @param feed a feed
     * @param name the local name of a child element of the channel, e.g. {@code title}
     * @return the text of that element
     */
    static String getChannelText(Document feed, String name)
    {
        Element channel = getChannel(feed);
        for (Node child = channel.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(child.getLocalName())) {
                return child.getTextContent();
            }
        }
        return fail("The channel has no " + name);
    }

    /**
     * @param feed a feed
     * @return the path, relative to the view action, of the page the channel is about
     */
    static String getChannelPage(Document feed)
    {
        String about = getChannel(feed).getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
        String view = "/view/";
        return about.substring(about.indexOf(view) + view.length());
    }

    /**
     * @param feed a feed
     * @param localName the local name of an element, e.g. {@code strong}
     * @return whether the feed contains such an element, in any namespace
     */
    static boolean containsElement(Document feed, String localName)
    {
        return feed.getElementsByTagNameNS("*", localName).getLength() > 0;
    }
}
