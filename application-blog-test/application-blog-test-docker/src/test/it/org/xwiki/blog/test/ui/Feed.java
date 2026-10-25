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
package org.xwiki.blog.test.ui;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A feed (RSS 1.0 or RSS 2.0) read as XML.
 *
 * @version $Id$
 */
final class Feed
{
    private static final String CHANNEL = "channel";

    private static final String ITEM = "item";

    private static final String TITLE = "title";

    private final Document document;

    /**
     * @param content the feed as sent by the wiki; the test fails when it's not well-formed XML
     */
    Feed(String content) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(null);
        try {
            this.document = builder.parse(new InputSource(new StringReader(content.trim())));
        } catch (SAXException e) {
            throw new AssertionError("The feed is not well-formed XML: " + e.getMessage() + "\n" + content, e);
        }
    }

    /**
     * @param name the local name of a child element of the channel, e.g. {@code title}
     * @return the text of that element
     */
    String getChannelText(String name)
    {
        return getChildText(getChannel(), name);
    }

    /**
     * @return the path, relative to the view action, of the page the channel is about (RSS 1.0)
     */
    String getChannelPage()
    {
        String about = getChannel().getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
        String view = "/view/";
        return about.substring(about.indexOf(view) + view.length());
    }

    /**
     * @return the titles of the items of the feed
     */
    List<String> getItemTitles()
    {
        return getItemTexts(TITLE);
    }

    /**
     * @return the links of the items of the feed
     */
    List<String> getItemLinks()
    {
        return getItemTexts("link");
    }

    /**
     * @param localName the local name of an element, e.g. {@code strong}
     * @return whether the feed contains such an element, in any namespace
     */
    boolean containsElement(String localName)
    {
        return this.document.getElementsByTagNameNS("*", localName).getLength() > 0;
    }

    private List<String> getItemTexts(String name)
    {
        List<String> texts = new ArrayList<>();
        NodeList items = this.document.getElementsByTagNameNS("*", ITEM);
        for (int i = 0; i < items.getLength(); i++) {
            texts.add(getChildText((Element) items.item(i), name));
        }
        return texts;
    }

    private Element getChannel()
    {
        NodeList channels = this.document.getElementsByTagNameNS("*", CHANNEL);
        if (channels.getLength() != 1) {
            fail("Expected one channel, found " + channels.getLength());
        }
        return (Element) channels.item(0);
    }

    private static String getChildText(Element parent, String name)
    {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(child.getLocalName())) {
                return child.getTextContent().trim();
            }
        }
        return fail("No " + name + " in " + parent.getLocalName());
    }
}
