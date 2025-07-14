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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

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

    private static final String CATEGORIES = "Categories";

    private static final String MIGRATING_CATEGORY_START_INFO = "[{}] - [{}] category migration.";

    private static final String CATEGORY_MIGRATION_END_INFO = "[{}] - [{}] category migration done";

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Logger logger;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("document")
    private QueryFilter documentQueryFilter;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private JobProgressManager progress;

    /**
     * Performs the migration on the categories from the specified wiki.
     */
    public void migrate()
    {
        boolean hasNamespace = componentManager instanceof NamespacedComponentManager;
        Collection<String> wikis =
            getTargetWikis(hasNamespace ? ((NamespacedComponentManager) componentManager).getNamespace() : null);
        progress.pushLevelProgress(wikis.size(), this);
        wikis.forEach(
            wikiId -> {
                progress.startStep(this);
                logger.info(String.format("Start migration of blog categories from [%s] wiki.", wikiId));
                try {
                    migrateDefaultCategories(wikiId);
                    migrateCustomCategories(wikiId);
                } catch (QueryException | XWikiException e) {
                    this.logger.error("Failed to get the list of categories to migrate.", e);
                }
                logger.info(String.format("End migration of blog categories from [%s] wiki.", wikiId));
                progress.endStep(this);
            });
        progress.popLevelProgress(this);
    }

    private void migrateCustomCategories(String wiki) throws QueryException, XWikiException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        String statement = "from doc.object(Blog.CategoryClass) as category where doc.space = 'Blog' and "
            + "doc.name <> 'CategoryTemplate'";
        Query query = this.queryManager.createQuery(statement, Query.XWQL);
        Collection<DocumentReference> oldCategoryRefs =
            query.setWiki(wiki).addFilter(documentQueryFilter).execute();
        for (DocumentReference oldCategoryRef : oldCategoryRefs) {
            logger.info(MIGRATING_CATEGORY_START_INFO, wiki, oldCategoryRef);
            DocumentReference newCategoryRef = getNewCategoryRef(oldCategoryRef);
            XWikiDocument oldCategoryDoc = xwiki.getDocument(oldCategoryRef, context);
            logger.info("[{}] - [{}] category migration. Renaming to new [{}].", wiki, oldCategoryRef, newCategoryRef);
            xwiki.renameDocument(oldCategoryRef, newCategoryRef, true, oldCategoryDoc.getBackLinkedReferences(context),
                oldCategoryDoc.getChildrenReferences(context), context);
            updateSubCategoriesParent(oldCategoryRef, newCategoryRef, xwiki, context, wiki);
            updateBlogPostsCategory(oldCategoryRef, newCategoryRef, xwiki, context, wiki);
            updateBackLinks(oldCategoryRef, newCategoryRef, context, xwiki, wiki);
            logger.info(CATEGORY_MIGRATION_END_INFO, wiki, oldCategoryRef);
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
                logger.info(MIGRATING_CATEGORY_START_INFO, wiki, oldCategoryRef);
                updateSubCategoriesParent(oldCategoryRef, newCategoryRef, xwiki, context, wiki);
                updateBlogPostsCategory(oldCategoryRef, newCategoryRef, xwiki, context, wiki);
                updateBackLinks(oldCategoryRef, newCategoryRef, context, xwiki, wiki);
                logger.info(CATEGORY_MIGRATION_END_INFO, wiki, category);
            }
        }
    }

    private void updateBackLinks(DocumentReference oldCategoryRef, DocumentReference newCategoryRef,
        XWikiContext context, XWiki xwiki, String wiki) throws XWikiException
    {
        XWikiDocument oldCategoryDoc = xwiki.getDocument(oldCategoryRef, context);
        Object refactoringTool = getRefactoringTool();

        Method renameMethod = getRenameMethod(refactoringTool);

        List<DocumentReference> backLinkedReferences = oldCategoryDoc.getBackLinkedReferences(context);
        if (!backLinkedReferences.isEmpty()) {
            logger.info("[{}] - [{}] category migration. Step 3: Updating backlinks.", wiki,
                oldCategoryRef);
        }
        for (DocumentReference backLinkedReference : backLinkedReferences) {
            try {
                logger.info("[{}] - [{}] category migration. Step 3: Updating backlinks of [{}] to new [{}].", wiki,
                    oldCategoryRef, backLinkedReference, newCategoryRef);
                renameMethod.invoke(refactoringTool, backLinkedReference, oldCategoryRef, newCategoryRef);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error(String.format("Error updating backlink: %s", backLinkedReference), e);
            }
        }
    }

    private Method getRenameMethod(Object refactoringTool)
    {
        Method renameMethod;
        try {
            // Try ReferenceUpdater#update() first (14.6+)
            renameMethod = refactoringTool.getClass().getMethod(
                "update",
                DocumentReference.class,
                EntityReference.class,
                EntityReference.class
            );
        } catch (NoSuchMethodException e) {
            try {
                // Fallback to LinkRefactoring#renameLinks() (pre-14.6)
                renameMethod = refactoringTool.getClass().getMethod(
                    "renameLinks",
                    DocumentReference.class,
                    DocumentReference.class,
                    DocumentReference.class
                );
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(
                    String.format("No compatible rename method found on %s", refactoringTool.getClass().getName()));
            }
        }
        return renameMethod;
    }

    private Object getRefactoringTool()
    {
        try {
            // Try ReferenceUpdater first (14.6+)
            return componentManager.getInstance(
                Class.forName("org.xwiki.refactoring.internal.ReferenceUpdater")
            );
        } catch (ComponentLookupException | ClassNotFoundException e1) {
            try {
                // Fallback to LinkRefactoring (pre-14.6)
                return componentManager.getInstance(
                    Class.forName("org.xwiki.refactoring.internal.LinkRefactoring")
                );
            } catch (ComponentLookupException | ClassNotFoundException e2) {
                throw new RuntimeException("No suitable refactoring tool found", e2);
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
        if (!subCategoryRefs.isEmpty()) {
            logger.info("[{}] - [{}] category migration. Step 1: Updating subcategories parent.", wiki,
                oldCategoryRef);
        }
        for (DocumentReference subCategoryRef : subCategoryRefs) {
            logger.info("[{}] - [{}] category migration. Step 1: Updating parent of [{}] to new [{}]", wiki,
                oldCategoryRef,
                subCategoryRef, newCategoryRef);
            updateSubCategoryParent(subCategoryRef, newCategoryRef, xwiki, context);
        }
    }

    private void updateSubCategoryParent(DocumentReference subCategoryRef, DocumentReference newCategoryRef,
        XWiki xwiki, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument subCategoryDoc = xwiki.getDocument(subCategoryRef, context);
        subCategoryDoc.setParentReference(newCategoryRef.getLocalDocumentReference());
        subCategoryDoc.setMetaDataDirty(false);
        subCategoryDoc.setContentDirty(false);
        xwiki.saveDocument(subCategoryDoc, context);
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
        if (!blogPostRefs.isEmpty()) {
            logger.info("[{}] - [{}] category migration. Step 2: Updating blog posts category.", wiki,
                oldCategoryRef);
        }
        for (DocumentReference blogPostRef : blogPostRefs) {
            logger.info("[{}] - [{}] category migration. Step 2: Updating blog posts category of [{}] to new [{}].",
                wiki,
                oldCategoryRef, blogPostRef, newCategoryRef);
            updateBlogPostCategory(blogPostRef, xwiki, context, oldCategory, newCategory);
        }
    }

    private void updateBlogPostCategory(DocumentReference blogPostRef, XWiki xwiki, XWikiContext context,
        String oldCategory, String newCategory) throws XWikiException
    {
        XWikiDocument blogPostDoc = xwiki.getDocument(blogPostRef, context);
        BaseObject blogPostObj = blogPostDoc.getXObject(
            new DocumentReference("xwiki", BLOG_SPACE, "BlogPostClass").getLocalDocumentReference());
        List<String> categories = blogPostObj.getListValue(CATEGORY);
        categories.remove(oldCategory);
        categories.add(newCategory);
        blogPostObj.setStringListValue(CATEGORY, categories);
        blogPostDoc.setMetaDataDirty(false);
        blogPostDoc.setContentDirty(false);
        xwiki.saveDocument(blogPostDoc, context);
    }

    private Collection<String> getTargetWikis(String namespace)
    {
        // Checking also for null namespace since it could mean that the upgrade is done on farm level.
        if (namespace != null && namespace.startsWith("wiki:")) {
            return Collections.singleton(namespace.substring(5));
        } else {
            try {
                return this.wikiDescriptorManager.getAllIds();
            } catch (WikiManagerException e) {
                this.logger.error("Failed to get the list of wikis.", e);
                return Collections.emptySet();
            }
        }
    }
}
