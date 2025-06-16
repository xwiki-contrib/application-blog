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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Event listener to listen to category rename events and update the category field in blog posts.
 *
 * @version $Id$
 * @since 9.15
 */
@Component
@Named(CategoryRenameListener.NAME)
@Singleton
public class CategoryRenameListener implements EventListener
{
    protected static final String NAME = "CategoryRenameListener";

    private static final String CATEGORY = "category";

    private static final String BLOG_SPACE = "Blog";

    private static final List<Event> EVENTS = Collections.singletonList(new DocumentRenamedEvent());

    private static final LocalDocumentReference BLOG_CATEGORY_CLASS =
        new LocalDocumentReference(BLOG_SPACE, "CategoryClass");

    private static final LocalDocumentReference BLOG_POST_CLASS =
        new LocalDocumentReference(BLOG_SPACE, "BlogPostClass");

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("document")
    private QueryFilter documentQueryFilter;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        DocumentRenamedEvent renamedEvent = (DocumentRenamedEvent) event;
        DocumentReference newCategoryReference = renamedEvent.getTargetReference();

        try {
            XWikiDocument newCategoryDocument = xwiki.getDocument(newCategoryReference, context);
            if (newCategoryDocument.getXObject(BLOG_CATEGORY_CLASS) != null) {
                String oldCategory = compactWikiSerializer.serialize(renamedEvent.getSourceReference());
                String statement =
                    "from doc.object(Blog.BlogPostClass) as blogPost where :category member of blogPost.category";
                Query query = queryManager.createQuery(statement, Query.XWQL);
                List<DocumentReference> blogPostReferences =
                    query.bindValue(CATEGORY, oldCategory).addFilter(documentQueryFilter).execute();
                for (DocumentReference blogPostReference : blogPostReferences) {
                    XWikiDocument blogPostDoc = xwiki.getDocument(blogPostReference, context);
                    BaseObject blogPostObj = blogPostDoc.getXObject(BLOG_POST_CLASS);
                    List<String> categories = blogPostObj.getListValue(CATEGORY);
                    categories.remove(oldCategory);
                    categories.add(compactWikiSerializer.serialize(newCategoryReference));
                    blogPostObj.setStringListValue(CATEGORY, categories);
                    xwiki.saveDocument(blogPostDoc, "Update blog post after category refactoring.", context);
                }
            }
        } catch (XWikiException | QueryException e) {
            logger.error("Failed to update categories", e);
        }
    }
}
