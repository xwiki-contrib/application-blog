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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.blog.BlogVisibilityUpdater;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Remove the visibility restrictions left on published blog posts. Before BLOG-172, each user who saved a post while
 * it was hidden got a "view" right on it, and publishing removed only the one of the publisher, so the published post
 * stayed restricted to the other users.
 *
 * @version $Id$
 * @since 9.15.16
 */
@Component(roles = BlogVisibilityRestrictionMigration.class)
@Singleton
public class BlogVisibilityRestrictionMigration
{
    /**
     * The reference to the xwiki rights, relative to the current wiki.
     */
    private static final EntityReference RIGHTS_CLASS = new EntityReference("XWikiRights", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private BlogVisibilityUpdater blogVisibilityUpdater;

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

            String xwql = "select distinct doc.fullName from Document doc, doc.object(Blog.BlogPostClass) post, "
                + "doc.object(XWiki.XWikiRights) rights "
                + "where post.published = 1 and post.hidden = 0 and rights.allow = 1";
            Query query = queryManager.createQuery(xwql, Query.XWQL).setWiki(wikiReference.getName());

            for (String docName : query.<String>execute()) {
                DocumentReference documentReference = referenceResolver.resolve(docName, wikiReference);
                XWikiDocument document = xwiki.getDocument(documentReference, context).clone();
                // The query also matches the "allow" rights that users set themselves, so check the exact shape.
                String creator = serializer.serialize(document.getCreatorReference());
                if (document.getXObjects(RIGHTS_CLASS).stream()
                    .anyMatch(rights -> DefaultBlogVisibilityUpdater.isVisibilityRestriction(rights, creator))) {
                    blogVisibilityUpdater.synchronizeHiddenMetadata(document);
                    xwiki.saveDocument(document,
                        "Remove the view restrictions left on the published blog post (BLOG-172).", context);
                }
            }

            logger.info("Migration of the blog posts' view restrictions has been successfully executed on the wiki "
                + "[{}].", wikiReference.getName());
        } catch (Exception e) {
            throw new Exception("Failed to remove the view restrictions left on published blog posts.", e);
        }
    }
}
