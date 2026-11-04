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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.blog.BlogVisibilityUpdater;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BlogVisibilityRestrictionMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class BlogVisibilityRestrictionMigrationTest
{
    private static final EntityReference RIGHTS_CLASS = new EntityReference("XWikiRights", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    private static final WikiReference WIKI = new WikiReference("chocolate");

    private static final DocumentReference BLOG_POST = new DocumentReference("chocolate", "Blog", "HelloWorld");

    private static final DocumentReference CREATOR_REFERENCE = new DocumentReference("chocolate", "XWiki", "Alice");

    private static final String CREATOR = "chocolate:XWiki.Alice";

    private static final String SAVE_COMMENT = "Remove the view restrictions left on the published blog post "
        + "(BLOG-172).";

    private static final String SUCCESS_MESSAGE =
        "Migration of the blog posts' view restrictions has been successfully executed on the wiki [chocolate].";

    @InjectMockComponents
    private BlogVisibilityRestrictionMigration migration;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private BlogVisibilityUpdater blogVisibilityUpdater;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private Query query;

    private XWiki xwiki;

    private XWikiContext context;

    private XWikiDocument loadedDocument;

    private XWikiDocument document;

    @BeforeEach
    void setUp() throws Exception
    {
        this.context = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);

        this.query = mock(Query.class);
        when(this.queryManager.createQuery(any(String.class), eq(Query.XWQL))).thenReturn(this.query);
        when(this.query.setWiki("chocolate")).thenReturn(this.query);

        when(this.referenceResolver.resolve("Blog.HelloWorld", WIKI)).thenReturn(BLOG_POST);

        // The migration clones the loaded document before modifying it, so the clone is what gets saved.
        this.loadedDocument = mock(XWikiDocument.class);
        this.document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(BLOG_POST, this.context)).thenReturn(this.loadedDocument);
        when(this.loadedDocument.clone()).thenReturn(this.document);
        when(this.document.getCreatorReference()).thenReturn(CREATOR_REFERENCE);
        when(this.serializer.serialize(CREATOR_REFERENCE)).thenReturn(CREATOR);
    }

    private static BaseObject rightsObject(String users, String groups)
    {
        BaseObject rights = new BaseObject();
        rights.setIntValue("allow", 1);
        rights.setLargeStringValue("users", users);
        rights.setLargeStringValue("groups", groups);
        rights.setStringValue("levels", "view");
        return rights;
    }

    @Test
    void executeRemovesTheRestrictionsLeftOnPublishedPosts() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.singletonList("Blog.HelloWorld"));
        when(this.document.getXObjects(RIGHTS_CLASS))
            .thenReturn(Arrays.asList(null, rightsObject("chocolate:XWiki.Bob", "")));

        this.migration.execute(WIKI);

        verify(this.blogVisibilityUpdater).synchronizeHiddenMetadata(this.document);
        verify(this.xwiki).saveDocument(this.document, SAVE_COMMENT, this.context);
        // The document from the cache is never modified directly.
        verify(this.loadedDocument, never()).getXObjects(any(EntityReference.class));
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeLeavesTheRightsSetByUsersUntouched() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.singletonList("Blog.HelloWorld"));
        // Neither lists the creator first, nor is given to a single user and no group.
        when(this.document.getXObjects(RIGHTS_CLASS)).thenReturn(Arrays.asList(
            rightsObject("chocolate:XWiki.Bob," + CREATOR, ""),
            rightsObject("chocolate:XWiki.Bob", "chocolate:XWiki.XWikiAllGroup"),
            rightsObject("", "chocolate:XWiki.XWikiAllGroup")));

        this.migration.execute(WIKI);

        verifyNoInteractions(this.blogVisibilityUpdater);
        verify(this.xwiki, never())
            .saveDocument(any(XWikiDocument.class), any(String.class), any(XWikiContext.class));
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeQueriesThePublishedPostsWithAllowRightsOnTheGivenWiki() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.emptyList());

        this.migration.execute(WIKI);

        verify(this.queryManager).createQuery("select distinct doc.fullName from Document doc, "
            + "doc.object(Blog.BlogPostClass) post, doc.object(XWiki.XWikiRights) rights "
            + "where post.published = 1 and post.hidden = 0 and rights.allow = 1", Query.XWQL);
        verify(this.query).setWiki("chocolate");
        verifyNoInteractions(this.xwiki, this.blogVisibilityUpdater);
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeWhenQueryFails() throws Exception
    {
        QueryException cause = new QueryException("Query failure", this.query, null);
        when(this.query.<String>execute()).thenThrow(cause);

        Exception exception = assertThrows(Exception.class, () -> this.migration.execute(WIKI));

        assertEquals("Failed to remove the view restrictions left on published blog posts.", exception.getMessage());
        assertSame(cause, exception.getCause());
        verifyNoInteractions(this.xwiki);
        assertEquals(0, this.logCapture.size());
    }
}
