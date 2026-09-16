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
package org.xwiki.platform.blog.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link BlogTitleMigration}.
 *
 * @version $Id$
 * @since 9.15.11
 */
@Component(roles = BlogTitleMigration.class)
@Singleton
public class BlogTitleMigration
{
    /**
     * The {@code Blog.BlogClass} reference, relative to the current wiki.
     */
    private static final LocalDocumentReference BLOG_CLASS = new LocalDocumentReference("Blog", "BlogClass");

    /**
     * The name of the evaluated title property.
     */
    private static final String TITLE_PROPERTY = "title";

    /**
     * The title stored by the default blog before BLOG-265. It used to be evaluated by the sheet to produce the
     * localized blog title; the sheet now renders the localized title itself when the stored title is empty.
     */
    private static final String DEFAULT_TITLE = "$services.localization.render('blog.code.title')";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Execute the migration on the given wiki.
     *
     * @param wikiReference reference of the wiki where the migration must be performed
     * @throws Exception if an error occurs
     */
    public void execute(WikiReference wikiReference) throws Exception
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();

            // Only touch blogs that still store the exact default expression, so that titles authored by users are
            // left untouched (they are already safe: the sheet no longer evaluates them).
            String xwql = "from doc.object(Blog.BlogClass) obj where obj.title = :defaultTitle";
            Query query = queryManager.createQuery(xwql, Query.XWQL)
                .bindValue("defaultTitle", DEFAULT_TITLE)
                .setWiki(wikiReference.getName());

            for (String docName : query.<String>execute()) {
                DocumentReference documentReference = referenceResolver.resolve(docName, wikiReference);
                XWikiDocument document = xwiki.getDocument(documentReference, context).clone();
                BaseObject blogObject = document.getXObject(BLOG_CLASS);
                if (blogObject != null && DEFAULT_TITLE.equals(blogObject.getStringValue(TITLE_PROPERTY))) {
                    // Blank the title so that Blog.BlogSheet renders the localized title from its fallback.
                    blogObject.setStringValue(TITLE_PROPERTY, "");
                    xwiki.saveDocument(document,
                        "Clear the default blog title so it is no longer evaluated (BLOG-265).", context);
                }
            }

            logger.info("Migration of the blog titles has been successfully executed on the wiki [{}].",
                wikiReference.getName());
        } catch (Exception e) {
            throw new Exception("Failed to migrate the blog titles.", e);
        }
    }
}
