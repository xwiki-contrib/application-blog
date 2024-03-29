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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Script APIs for the Blog Application.
 *
 * @version $Id$
 * @since 9.13.2
 */
@Component
@Named("blog")
@Singleton
@Unstable
public class BlogScriptService implements ScriptService
{
    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    /**
     * @param document the document containing the attachment
     * @param filename the document's attachment for which to generate an absolute URL for
     * @return the absolute URL to the attachment
     */
    public String getExternalAttachmentURL(Document document, String filename)
    {
        // TODO: Remove this method once the blog application has a minimal XWiki version that offers a non-PR method
        // to get an external attachment URL. See https://jira.xwiki.org/browse/XWIKI-20770
        XWikiContext context = this.xwikiContextProvider.get();
        return document.getDocument().getExternalAttachmentURL(filename, "download", context);
    }

    /**
     * Render the passed content to HTML so that it can be used in the {@code <description>} RSS tag. It ensures that
     * URLS are rendered as absolute URLs.
     *
     * @param contentToRender the content to render, written in the syntax of the passed document
     * @param blogDocument the document containing the content to render
     * @return the rendered content as HTML
     * @throws XWikiException if there's an error when rendering the content to HTML
     */
    public String renderRSSDescription(String contentToRender, Document blogDocument) throws XWikiException
    {
        XWikiContext context = this.xwikiContextProvider.get();
        XWikiURLFactory currentURLFactory = context.getURLFactory();
        try {
            context.setURLFactory(new ExternalServletURLFactory(context));
            return blogDocument.getRenderedContent(contentToRender, blogDocument.getSyntax().toIdString());
        } finally {
            context.setURLFactory(currentURLFactory);
        }
    }
}
