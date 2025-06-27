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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Migration component responsible for reorganizing the blog categories. It handles both custom-defined and default
 * categories, updating their locations, parent references, and associated blog posts accordingly.
 *
 * @version $Id$
 * @since 9.15
 */
@Component(roles = CategoryLocationMigration.class)
@Singleton
public class CategoryLocationMigration
{
    private static final String BLOG_CATEGORY_TEMPLATE = "Blog.CategoryTemplate";

    private static final List<String> DEFAULT_CATEGORIES =
        Arrays.asList("Blog.Categories", "Blog.News", "Blog.Other", "Blog.Personal", BLOG_CATEGORY_TEMPLATE);

    private static final String BLOG_SPACE = "Blog";

    private static final String CATEGORY = "category";

    private static final LocalDocumentReference BLOG_POST_CLASS =
        new LocalDocumentReference(BLOG_SPACE, "BlogPostClass");

    private static final String CATEGORIES = "Categories";

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("document")
    private QueryFilter documentQueryFilter;

    /**
     * Performs the migration on the categories from the specified wiki.
     *
     * @param wiki the wiki where to run the migration
     */
    public void migrate(String wiki)
    {
        try {
            migrateCustomCategories(wiki);
            migrateDefaultCategories(wiki);
        } catch (QueryException | XWikiException e) {
            this.logger.error("Failed to get the list of categories to migrate.", e);
        }
    }

    private void migrateCustomCategories(String wiki) throws QueryException, XWikiException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        String statement = "from doc.object(Blog.CategoryClass) as category where doc.fullName not in "
            + "(:defaultCategories) and doc.space = 'Blog'";
        Query query = this.queryManager.createQuery(statement, Query.XWQL);
        Collection<DocumentReference> oldCategoryRefs =
            query.setWiki(wiki).bindValue("defaultCategories", DEFAULT_CATEGORIES).addFilter(documentQueryFilter)
                .execute();
        for (DocumentReference oldCategoryRef : oldCategoryRefs) {
            DocumentReference newCategoryRef = getNewCategoryRef(oldCategoryRef);
            XWikiDocument oldCategoryDoc = xwiki.getDocument(oldCategoryRef, context);
            oldCategoryDoc.rename(newCategoryRef, Collections.emptyList(), Collections.emptyList(), context);
            // Update the parent of a newly migrated category if its parent was a default category.
            updateSubCategoryParentIfDefault(oldCategoryDoc, newCategoryRef, xwiki, context);
            updateSubCategoriesParent(oldCategoryRef, newCategoryRef, xwiki, context, wiki);
            updateBlogPostsCategory(oldCategoryRef, newCategoryRef, xwiki, context, wiki);
        }
    }

    private void migrateDefaultCategories(String wiki) throws QueryException, XWikiException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        for (String category : DEFAULT_CATEGORIES) {
            if (!category.equals(BLOG_CATEGORY_TEMPLATE)) {
                DocumentReference oldCategoryRef = resolver.resolve(category);
                DocumentReference newCategoryRef = getNewCategoryRef(oldCategoryRef);
                updateBlogPostsCategory(oldCategoryRef, newCategoryRef, xwiki, context, wiki);
            }
        }
    }

    private DocumentReference getNewCategoryRef(DocumentReference oldCategoryRef)
    {
        String newCategoryName = oldCategoryRef.getName();
        if (newCategoryName.equals(CATEGORIES)) {
            newCategoryName = "WebHome";
        }
        return new DocumentReference(newCategoryName,
            new SpaceReference(CATEGORIES, oldCategoryRef.getLastSpaceReference()));
    }

    private void updateSubCategoriesParent(DocumentReference oldCategoryRef, DocumentReference newCategoryRef,
        XWiki xwiki, XWikiContext context, String wiki) throws QueryException, XWikiException
    {
        String oldCategory = compactWikiSerializer.serialize(oldCategoryRef);
        String statement = "from doc.object(Blog.CategoryClass) as category where doc.parent = :category";
        Query query = queryManager.createQuery(statement, Query.XWQL);
        List<DocumentReference> subCategoryRefs =
            query.setWiki(wiki).bindValue(CATEGORY, oldCategory).addFilter(documentQueryFilter).execute();
        for (DocumentReference subCategoryRef : subCategoryRefs) {
            updateSubCategoryParent(subCategoryRef, newCategoryRef, xwiki, context);
        }
    }

    private void updateSubCategoryParent(DocumentReference subCategoryRef, DocumentReference newCategoryRef,
        XWiki xwiki, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument subCategoryDoc = xwiki.getDocument(subCategoryRef, context);
        subCategoryDoc.setParentReference(newCategoryRef);
        subCategoryDoc.setMetaDataDirty(false);
        subCategoryDoc.setContentDirty(false);
        xwiki.saveDocument(subCategoryDoc, context);
    }

    private void updateSubCategoryParentIfDefault(XWikiDocument oldCategoryDoc, DocumentReference subCategoryRef,
        XWiki xwiki,
        XWikiContext context) throws XWikiException
    {
        DocumentReference oldParentRef = oldCategoryDoc.getParentReference();
        if (DEFAULT_CATEGORIES.contains(compactWikiSerializer.serialize(oldParentRef))) {
            DocumentReference newParentRef = getNewCategoryRef(oldParentRef);

            updateSubCategoryParent(subCategoryRef, newParentRef, xwiki, context);
        }
    }

    private void updateBlogPostsCategory(DocumentReference oldCategoryRef, DocumentReference newCategoryRef,
        XWiki xwiki, XWikiContext context, String wiki) throws QueryException, XWikiException
    {
        String oldCategory = compactWikiSerializer.serialize(oldCategoryRef);
        String newCategory = compactWikiSerializer.serialize(newCategoryRef);
        String statement =
            "from doc.object(Blog.BlogPostClass) as blogPost where :category member of blogPost.category";
        Query query = queryManager.createQuery(statement, Query.XWQL);
        List<DocumentReference> blogPostRefs =
            query.setWiki(wiki).bindValue(CATEGORY, oldCategory).addFilter(documentQueryFilter).execute();
        for (DocumentReference blogPostRef : blogPostRefs) {
            updateBlogPostCategory(blogPostRef, xwiki, context, oldCategory, newCategory);
        }
    }

    private void updateBlogPostCategory(DocumentReference blogPostRef, XWiki xwiki, XWikiContext context,
        String oldCategory, String newCategory) throws XWikiException
    {
        XWikiDocument blogPostDoc = xwiki.getDocument(blogPostRef, context);
        BaseObject blogPostObj = blogPostDoc.getXObject(BLOG_POST_CLASS);
        List<String> categories = blogPostObj.getListValue(CATEGORY);
        categories.remove(oldCategory);
        categories.add(newCategory);
        blogPostObj.setStringListValue(CATEGORY, categories);
        blogPostDoc.setMetaDataDirty(false);
        blogPostDoc.setContentDirty(false);
        xwiki.saveDocument(blogPostDoc, context);
    }
}
