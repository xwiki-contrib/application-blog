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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.link.LinkStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.platform.blog.internal.InMemoryBlogQueries.BLOG_POST_CLASS;
import static org.xwiki.platform.blog.internal.InMemoryBlogQueries.CATEGORY;
import static org.xwiki.platform.blog.internal.InMemoryBlogQueries.CATEGORY_CLASS;

/**
 * Unit tests for {@link CategoryLocationMigration}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class CategoryLocationMigrationTest
{
    private static final String XWIKI = "xwiki";

    private static final String SUBWIKI = "subwiki";

    private static final String DESCRIPTION = "description";

    private static final List<String> CATEGORIES_SPACE = Arrays.asList("Blog", "Categories");

    private static final DocumentReference LEGACY_TRAVEL = new DocumentReference(XWIKI, "Blog", "Travel");

    private static final DocumentReference TRAVEL = new DocumentReference(XWIKI, CATEGORIES_SPACE, "Travel");

    private static final DocumentReference LEGACY_EUROPE = new DocumentReference(XWIKI, "Blog", "Europe");

    private static final DocumentReference EUROPE = new DocumentReference(XWIKI, CATEGORIES_SPACE, "Europe");

    private static final DocumentReference POST = new DocumentReference(XWIKI, "Blog", "Post");

    private static final DocumentReference LINKING_PAGE = new DocumentReference(XWIKI, "Main", "Links");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private CategoryLocationMigration migration;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private LinkStore linkStore;

    // Used to find the children of the renamed documents.
    @MockComponent
    @Named("hidden")
    private QueryFilter hiddenFilter;

    @MockComponent
    private ReferenceUpdater referenceUpdater;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private XWiki xwiki;

    private XWikiContext context;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki = this.oldcore.getSpyXWiki();
        this.context = this.oldcore.getXWikiContext();
        new InMemoryBlogQueries(this.oldcore).answer(this.queryManager);
        // Used to find the children of the renamed documents.
        when(this.oldcore.getMockStore().getQueryManager()).thenReturn(this.queryManager);

        // Move the document, the way XWiki#renameDocument() does it (except for the backlinks and children, that are
        // checked on the call).
        doAnswer(invocation -> {
            XWikiDocument source = this.xwiki.getDocument((DocumentReference) invocation.getArgument(0), this.context);
            DocumentReference targetReference = invocation.getArgument(1);
            boolean overwrite = invocation.getArgument(2);
            if (source.isNew() || !(overwrite || this.xwiki.getDocument(targetReference, this.context).isNew())) {
                return false;
            }
            this.xwiki.saveDocument(source.copyDocument(targetReference, this.context), this.context);
            this.xwiki.deleteDocument(source, this.context);
            return true;
        }).when(this.xwiki).renameDocument(any(DocumentReference.class), any(DocumentReference.class), anyBoolean(),
            anyList(), anyList(), any(XWikiContext.class));
    }

    private XWikiDocument saveCategory(DocumentReference reference, String parent, String description)
        throws XWikiException
    {
        XWikiDocument category = new XWikiDocument(reference);
        category.setParent(parent);
        BaseObject categoryObject = new BaseObject();
        categoryObject.setXClassReference(CATEGORY_CLASS);
        categoryObject.setLargeStringValue(DESCRIPTION, description);
        category.addXObject(categoryObject);
        this.xwiki.saveDocument(category, this.context);
        return category;
    }

    private void saveBlogPost(DocumentReference reference, String... categories) throws XWikiException
    {
        XWikiDocument post = new XWikiDocument(reference);
        BaseObject postObject = new BaseObject();
        postObject.setXClassReference(BLOG_POST_CLASS);
        postObject.setStringListValue(CATEGORY, Arrays.asList(categories));
        post.addXObject(postObject);
        this.xwiki.saveDocument(post, this.context);
    }

    private XWikiDocument getDocument(DocumentReference reference) throws XWikiException
    {
        return this.xwiki.getDocument(reference, this.context);
    }

    private List<?> getCategories(DocumentReference blogPost) throws XWikiException
    {
        return getDocument(blogPost).getXObject(BLOG_POST_CLASS).getListValue(CATEGORY);
    }

    private String getDescription(DocumentReference category) throws XWikiException
    {
        return getDocument(category).getXObject(CATEGORY_CLASS).getLargeStringValue(DESCRIPTION);
    }

    private void backlinks(EntityReference target, EntityReference... backlinks) throws Exception
    {
        when(this.linkStore.resolveBackLinkedEntities(target)).thenReturn(new HashSet<>(Arrays.asList(backlinks)));
    }

    private void migrate(String wiki)
    {
        this.migration.migrate(wiki);

        // The migration logs its progress, which is not what is tested here.
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void hasLegacyCategoryAssignmentsWhenABlogPostIsInACategoryOfTheBlogSpace() throws Exception
    {
        saveCategory(LEGACY_TRAVEL, "Blog.Categories", "");
        saveBlogPost(POST, "Blog.Categories.News", "Blog.Travel");

        assertTrue(this.migration.hasLegacyCategoryAssignments());
    }

    @Test
    void hasNoLegacyCategoryAssignmentsWhenTheBlogPostsAreInMigratedCategories() throws Exception
    {
        saveCategory(LEGACY_TRAVEL, "Blog.Categories", "");
        saveCategory(TRAVEL, "Blog.Categories.WebHome", "");
        saveBlogPost(POST, "Blog.Categories.Travel");

        assertFalse(this.migration.hasLegacyCategoryAssignments());
    }

    @Test
    void hasNoLegacyCategoryAssignmentsWhenTheQueryFails() throws Exception
    {
        when(this.queryManager.createQuery(anyString(), any())).thenThrow(new QueryException("error", null, null));

        assertFalse(this.migration.hasLegacyCategoryAssignments());
        assertEquals("Failed to get the list of old categories.", this.logCapture.getMessage(0));
    }

    @Test
    void migrateMovesTheLegacyCategoriesToTheCategoriesSpace() throws Exception
    {
        saveCategory(LEGACY_TRAVEL, "Blog.Categories", "Trips");
        saveCategory(LEGACY_EUROPE, "Blog.Travel", "Trips in Europe");
        saveBlogPost(POST, "Blog.Travel", "Blog.Categories.Personal");
        DocumentReference otherPost = new DocumentReference(XWIKI, "Blog", "OtherPost");
        saveBlogPost(otherPost, "Blog.Europe");
        backlinks(LEGACY_TRAVEL, LINKING_PAGE);

        migrate(XWIKI);

        assertTrue(getDocument(LEGACY_TRAVEL).isNew());
        assertTrue(getDocument(LEGACY_EUROPE).isNew());
        assertEquals("Trips", getDescription(TRAVEL));
        assertEquals("Trips in Europe", getDescription(EUROPE));
        // The parent of a top level category is the home page of the categories.
        assertEquals("Blog.Categories.WebHome", getDocument(TRAVEL).getParent());
        assertEquals("Blog.Categories.Travel", getDocument(EUROPE).getParent());
        // The migrated category moves to the end of the post's categories. It should keep its position: BLOG-273
        // "Renaming or migrating a blog category moves it to the end of the categories of its blog posts".
        assertEquals(Arrays.asList("Blog.Categories.Personal", "Blog.Categories.Travel"), getCategories(POST));
        assertEquals(Arrays.asList("Blog.Categories.Europe"), getCategories(otherPost));
        // The links to the legacy category are updated by the rename.
        verify(this.xwiki).renameDocument(LEGACY_TRAVEL, TRAVEL, false, Arrays.asList(LINKING_PAGE),
            Collections.emptyList(), this.context);
    }

    @Test
    void migrateUpdatesWhatPointsToTheDefaultLegacyCategories() throws Exception
    {
        DocumentReference subcategory = new DocumentReference(XWIKI, CATEGORIES_SPACE, "Sub");
        saveCategory(subcategory, "Blog.News", "");
        saveBlogPost(POST, "Blog.Other", "Blog.Categories.Sub");
        DocumentReference legacyNews = new DocumentReference(XWIKI, "Blog", "News");
        // The legacy page is shipped by the extension, even when the new one is missing.
        saveCategory(legacyNews, "Blog.Categories", "");
        backlinks(legacyNews, LINKING_PAGE);

        migrate(XWIKI);

        assertEquals("Blog.Categories.News", getDocument(subcategory).getParent());
        assertEquals(Arrays.asList("Blog.Categories.Sub", "Blog.Categories.Other"), getCategories(POST));
        verify(this.referenceUpdater).update(LINKING_PAGE, legacyNews,
            new DocumentReference(XWIKI, CATEGORIES_SPACE, "News"));
        // The default legacy categories are not moved: there is a new one shipped for each of them.
        verify(this.xwiki, never()).renameDocument(eq(legacyNews), any(DocumentReference.class), anyBoolean(),
            anyList(), anyList(), any(XWikiContext.class));
    }

    @Test
    void migrateKeepsTheShippedCategoriesAndTheShippedLegacyOnes() throws Exception
    {
        // The pages of the default categories, shipped by the extension both at the legacy and at the new location.
        DocumentReference legacyNews = new DocumentReference(XWIKI, "Blog", "News");
        DocumentReference news = new DocumentReference(XWIKI, CATEGORIES_SPACE, "News");
        DocumentReference legacyCategoriesHome = new DocumentReference(XWIKI, "Blog", "Categories");
        DocumentReference categoriesHome = new DocumentReference(XWIKI, CATEGORIES_SPACE, "WebHome");
        saveCategory(legacyNews, "Blog.Categories", "legacy");
        saveCategory(news, "Blog.Categories.WebHome", "new");
        saveCategory(legacyCategoriesHome, "Blog.WebHome", "legacy");
        XWikiDocument categoriesHomeDocument = new XWikiDocument(categoriesHome);
        categoriesHomeDocument.setParent("Blog.WebHome");
        this.xwiki.saveDocument(categoriesHomeDocument, this.context);

        migrate(XWIKI);

        // BLOG-275: the shipped pages are left untouched, at both locations.
        assertEquals("legacy", getDescription(legacyNews));
        assertEquals("new", getDescription(news));
        assertEquals("legacy", getDescription(legacyCategoriesHome));
        assertNull(getDocument(categoriesHome).getXObject(CATEGORY_CLASS));
        verify(this.xwiki, never()).renameDocument(any(DocumentReference.class), any(DocumentReference.class),
            anyBoolean(), anyList(), anyList(), any(XWikiContext.class));
    }

    @Test
    void migrateKeepsTheExistingCategoryOfTheSameName() throws Exception
    {
        saveCategory(LEGACY_TRAVEL, "Blog.Categories", "legacy");
        saveCategory(TRAVEL, "Blog.Categories.WebHome", "new");
        saveCategory(LEGACY_EUROPE, "Blog.Travel", "");
        saveBlogPost(POST, "Blog.Travel");
        backlinks(LEGACY_TRAVEL, LINKING_PAGE);

        migrate(XWIKI);

        // The existing category is not overwritten and the legacy one stays where it is.
        assertEquals("new", getDescription(TRAVEL));
        assertEquals("legacy", getDescription(LEGACY_TRAVEL));
        verify(this.xwiki, never()).renameDocument(eq(LEGACY_TRAVEL), any(DocumentReference.class), anyBoolean(),
            anyList(), anyList(), any(XWikiContext.class));
        // What pointed to the legacy category now points to the existing one.
        assertEquals(Arrays.asList("Blog.Categories.Travel"), getCategories(POST));
        assertEquals("Blog.Categories.Travel", getDocument(EUROPE).getParent());
        verify(this.referenceUpdater).update(LINKING_PAGE, LEGACY_TRAVEL, TRAVEL);
    }

    @Test
    void migrateAnotherWikiUsesTheReferencesOfTheCurrentWiki() throws Exception
    {
        DocumentReference subwikiPost = new DocumentReference(SUBWIKI, "Blog", "Post");
        DocumentReference subwikiLegacyTravel = new DocumentReference(SUBWIKI, "Blog", "Travel");
        DocumentReference subwikiTravel = new DocumentReference(SUBWIKI, CATEGORIES_SPACE, "Travel");
        DocumentReference subwikiLinkingPage = new DocumentReference(SUBWIKI, "Main", "Links");
        saveCategory(subwikiLegacyTravel, "Blog.Categories", "");
        saveBlogPost(subwikiPost, "Blog.News", "Blog.Travel");
        DocumentReference legacyNews = new DocumentReference(XWIKI, "Blog", "News");
        backlinks(legacyNews, LINKING_PAGE);
        backlinks(new DocumentReference(SUBWIKI, "Blog", "News"), subwikiLinkingPage);

        // Migrate the subwiki from the main wiki.
        migrate(SUBWIKI);

        assertTrue(getDocument(subwikiLegacyTravel).isNew());
        assertNotNull(getDocument(subwikiTravel).getXObject(CATEGORY_CLASS));
        // The default categories are resolved in the current wiki, so the links of the current wiki are updated
        // instead of those of the migrated wiki, and the blog posts of the migrated wiki are not updated for the
        // migrated custom categories. All the references should be those of the migrated wiki: BLOG-276 "Migrating
        // the blog categories of another wiki updates the links of the current wiki and misses its blog
        // posts".
        verify(this.referenceUpdater).update(LINKING_PAGE, legacyNews,
            new DocumentReference(XWIKI, CATEGORIES_SPACE, "News"));
        verify(this.referenceUpdater, never()).update(eq(subwikiLinkingPage), any(), any());
        assertEquals(Arrays.asList("Blog.Travel", "Blog.Categories.News"), getCategories(subwikiPost));
    }

    @Test
    void migrateStopsWhenTheCategoriesCannotBeFound() throws Exception
    {
        when(this.queryManager.createQuery(anyString(), any())).thenThrow(new QueryException("error", null, null));

        this.migration.migrate(XWIKI);

        assertTrue(IntStream.range(0, this.logCapture.size()).anyMatch(
            index -> "Failed to get the list of categories to migrate.".equals(this.logCapture.getMessage(index))));
        this.logCapture.ignoreAllMessages();
    }
}
