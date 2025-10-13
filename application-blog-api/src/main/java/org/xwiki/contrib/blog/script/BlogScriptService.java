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

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.platform.blog.internal.CategoryLocationMigration;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.api.Property;
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

    private static final String EXTRACT_PROPERTY_NAME = "extract";

    private static final String PARAGRAPH_CLOSE_TAG = "</p>";

    private static final String VIEW_MODE = "view";

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    @Named("html/5.0")
    private BlockRenderer htmlRenderer;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private CategoryLocationMigration categoryLocationMigration;

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
     * @deprecated Since 9.14, use {@link #renderContentHTML(Document, Object, boolean, boolean, boolean)} instead.
     */
    @Deprecated
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

    /**
     * Renders the content of the specified blog post document to HTML, with options for using the extract if available,
     * adding ellipses, and generating external URLs.
     *
     * @param blogDocument the document representing the blog post
     * @param blogPostObject the object containing the blog post data
     * @param onlyExtract if true, renders the "extract" field of the blog post object (when present) instead of the
     * full content
     * @param removeEllipsis if true, no ellipsis ("...") will be appended to the extract
     * @param externalURLs if true, forces URLs included in the rendered content to be external
     * @return a string representing the rendered HTML content of the blog post
     * @since 9.14
     */
    public String renderContentHTML(Document blogDocument, Object blogPostObject, boolean onlyExtract,
        boolean removeEllipsis, boolean externalURLs)
    {
        XWikiContext context = this.xwikiContextProvider.get();
        XWikiURLFactory currentURLFactory = context.getURLFactory();
        try {
            if (externalURLs) {
                context.setURLFactory(new ExternalServletURLFactory(context));
            }

            boolean useExtract = onlyExtract && hasExtract(blogPostObject);

            String result;

            if (useExtract) {
                result = unwrapHTMLMacro(blogDocument.display(EXTRACT_PROPERTY_NAME, VIEW_MODE, blogPostObject));

                if (!removeEllipsis) {
                    String title = this.localizationManager.getTranslationPlain("blog.code.readpost");
                    String stringReference = this.referenceSerializer.serialize(blogDocument.getDocumentReference());
                    Block linkBlock = new LinkBlock(
                        Collections.singletonList(new WordBlock("…")),
                        new DocumentResourceReference(stringReference),
                        false,
                        Collections.singletonMap("title", title)
                    );
                    DefaultWikiPrinter printer = new DefaultWikiPrinter();
                    this.htmlRenderer.render(linkBlock, printer);

                    String ellipsis = printer.toString();

                    // Check if the (trimmed) content ends with a closing paragraph tag.
                    // If yes, add the ellipsis inside that last paragraph, else add it as a new paragraph.
                    if (StringUtils.lastIndexOf(result, PARAGRAPH_CLOSE_TAG) > 0
                        && StringUtils.isBlank(StringUtils.substringAfterLast(result, PARAGRAPH_CLOSE_TAG))) {
                        result = StringUtils.substringBeforeLast(result,
                            PARAGRAPH_CLOSE_TAG) + " " + ellipsis + PARAGRAPH_CLOSE_TAG;
                    } else {
                        result = result + "<p>" + ellipsis + PARAGRAPH_CLOSE_TAG;
                    }
                }
            } else {
                result = unwrapHTMLMacro(blogDocument.display("content", VIEW_MODE, blogPostObject));
            }

            return result;
        } finally {
            if (externalURLs) {
                context.setURLFactory(currentURLFactory);
            }
        }
    }

    private static String unwrapHTMLMacro(String input)
    {
        return StringUtils.removeEnd(StringUtils.removeStart(input, "{{html clean=\"false\" wiki=\"false\"}}"),
            "{{/html}}");
    }

    private static boolean hasExtract(Object blogPostObject)
    {
        Property extractProperty = blogPostObject.getProperty(EXTRACT_PROPERTY_NAME);
        return extractProperty != null && extractProperty.getValue() != null
            && StringUtils.isNotBlank(extractProperty.getValue().toString());
    }

    /**
     * Initiates the migration of category locations within the wiki farm.
     *
     * @param wikiId the id of the wiki where the blog categories are migrated
     * @since 9.15
     */
    @Unstable
    public void migrateCategoryLocation(String wikiId)
    {
        categoryLocationMigration.migrate(wikiId);
    }

    /**
     * Checks whether any blog posts are still associated with legacy categories located in the {@code Blog} space
     * (i.e., categories that have not yet been migrated to the new {@code Blog.Categories} location).
     *
     * @return {@code true} if one or more blog posts still reference legacy Blog categories; {@code false} otherwise or
     *     if the query fails
     * @since 9.15.3
     */
    @Unstable
    public boolean hasLegacyCategoryAssignments()
    {
        return categoryLocationMigration.hasLegacyCategoryAssignments();
    }
}
