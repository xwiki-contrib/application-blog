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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
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
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BlogTitleMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class BlogTitleMigrationTest
{
    private static final String DEFAULT_TITLE = "$services.localization.render('blog.code.title')";

    private static final String TITLE_PROPERTY = "title";

    private static final WikiReference WIKI = new WikiReference("chocolate");

    private static final LocalDocumentReference BLOG_CLASS = new LocalDocumentReference("Blog", "BlogClass");

    private static final DocumentReference BLOG_HOME = new DocumentReference("chocolate", "Blog", "WebHome");

    private static final String SAVE_COMMENT = "Clear the default blog title so it is no longer evaluated (BLOG-265).";

    private static final String SUCCESS_MESSAGE =
        "Migration of the blog titles has been successfully executed on the wiki [chocolate].";

    @InjectMockComponents
    private BlogTitleMigration migration;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private Query query;

    private XWiki xwiki;

    private XWikiContext context;

    private XWikiDocument loadedDocument;

    private XWikiDocument document;

    private BaseObject blogObject;

    @BeforeEach
    void setUp() throws Exception
    {
        this.context = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);

        this.query = mock(Query.class);
        when(this.queryManager.createQuery(any(String.class), eq(Query.XWQL))).thenReturn(this.query);
        when(this.query.bindValue(eq("defaultTitle"), any())).thenReturn(this.query);
        when(this.query.setWiki("chocolate")).thenReturn(this.query);

        when(this.referenceResolver.resolve("Blog.WebHome", WIKI)).thenReturn(BLOG_HOME);

        // The migration clones the loaded document before modifying it, so the clone is what gets saved.
        this.loadedDocument = mock(XWikiDocument.class);
        this.document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(BLOG_HOME, this.context)).thenReturn(this.loadedDocument);
        when(this.loadedDocument.clone()).thenReturn(this.document);
        this.blogObject = mock(BaseObject.class);
        when(this.document.getXObject(BLOG_CLASS)).thenReturn(this.blogObject);
    }

    @Test
    void executeClearsTheDefaultTitle() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.singletonList("Blog.WebHome"));
        when(this.blogObject.getStringValue(TITLE_PROPERTY)).thenReturn(DEFAULT_TITLE);

        this.migration.execute(WIKI);

        verify(this.blogObject).setStringValue(TITLE_PROPERTY, "");
        verify(this.xwiki).saveDocument(eq(this.document), any(String.class), eq(this.context));
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeLeavesUserTitlesUntouched() throws Exception
    {
        // The query is bound to the default title, but we still double-check the stored value defensively.
        when(this.query.<String>execute()).thenReturn(Collections.singletonList("Blog.WebHome"));
        when(this.blogObject.getStringValue(TITLE_PROPERTY)).thenReturn("My custom blog");

        this.migration.execute(WIKI);

        verify(this.blogObject, never()).setStringValue(eq(TITLE_PROPERTY), any());
        verify(this.xwiki, never())
            .saveDocument(any(XWikiDocument.class), any(String.class), any(XWikiContext.class));
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeWithNoMatchingBlog() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.emptyList());

        this.migration.execute(WIKI);

        verify(this.xwiki, never())
            .saveDocument(any(XWikiDocument.class), any(String.class), any(XWikiContext.class));
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    private BaseObject mockBlog(DocumentReference reference, String title) throws Exception
    {
        XWikiDocument loaded = mock(XWikiDocument.class);
        XWikiDocument clone = mock(XWikiDocument.class);
        BaseObject object = mock(BaseObject.class);
        when(this.referenceResolver.resolve(reference.getLastSpaceReference().getName() + '.' + reference.getName(),
            WIKI)).thenReturn(reference);
        when(this.xwiki.getDocument(reference, this.context)).thenReturn(loaded);
        when(loaded.clone()).thenReturn(clone);
        when(clone.getXObject(BLOG_CLASS)).thenReturn(object);
        when(object.getStringValue(TITLE_PROPERTY)).thenReturn(title);
        when(object.getOwnerDocument()).thenReturn(clone);
        return object;
    }

    @Test
    void executeQueriesTheDefaultTitleOnTheGivenWiki() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.emptyList());

        this.migration.execute(WIKI);

        verify(this.queryManager).createQuery("from doc.object(Blog.BlogClass) obj where obj.title = :defaultTitle",
            Query.XWQL);
        verify(this.query).bindValue("defaultTitle", DEFAULT_TITLE);
        verify(this.query).setWiki("chocolate");
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeSavesTheClonedDocumentWithAComment() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.singletonList("Blog.WebHome"));
        when(this.blogObject.getStringValue(TITLE_PROPERTY)).thenReturn(DEFAULT_TITLE);

        this.migration.execute(WIKI);

        verify(this.xwiki).saveDocument(this.document, SAVE_COMMENT, this.context);
        // The document from the cache is never modified directly.
        verify(this.loadedDocument, never()).getXObject(any(LocalDocumentReference.class));
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeSkipsDocumentWithoutBlogObject() throws Exception
    {
        when(this.query.<String>execute()).thenReturn(Collections.singletonList("Blog.WebHome"));
        when(this.document.getXObject(BLOG_CLASS)).thenReturn(null);

        this.migration.execute(WIKI);

        verify(this.xwiki, never())
            .saveDocument(any(XWikiDocument.class), any(String.class), any(XWikiContext.class));
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeMigratesOnlyTheBlogsStoringTheDefaultTitle() throws Exception
    {
        DocumentReference news = new DocumentReference("chocolate", "News", "WebHome");
        DocumentReference team = new DocumentReference("chocolate", "Team", "WebHome");
        BaseObject newsObject = mockBlog(news, DEFAULT_TITLE);
        BaseObject teamObject = mockBlog(team, "Team blog");
        when(this.query.<String>execute()).thenReturn(Arrays.asList("News.WebHome", "Team.WebHome"));

        this.migration.execute(WIKI);

        verify(newsObject).setStringValue(TITLE_PROPERTY, "");
        verify(this.xwiki).saveDocument(newsObject.getOwnerDocument(), SAVE_COMMENT, this.context);
        verify(teamObject, never()).setStringValue(eq(TITLE_PROPERTY), any());
        verify(this.xwiki, never()).saveDocument(teamObject.getOwnerDocument(), SAVE_COMMENT, this.context);
        assertEquals(SUCCESS_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void executeWhenQueryFails() throws Exception
    {
        QueryException cause = new QueryException("Query failure", this.query, null);
        when(this.query.<String>execute()).thenThrow(cause);

        Exception exception = assertThrows(Exception.class, () -> this.migration.execute(WIKI));

        assertEquals("Failed to migrate the blog titles.", exception.getMessage());
        assertSame(cause, exception.getCause());
        verifyNoInteractions(this.xwiki);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void executeStopsAtFirstSaveFailure() throws Exception
    {
        DocumentReference news = new DocumentReference("chocolate", "News", "WebHome");
        mockBlog(news, DEFAULT_TITLE);
        when(this.blogObject.getStringValue(TITLE_PROPERTY)).thenReturn(DEFAULT_TITLE);
        when(this.query.<String>execute()).thenReturn(Arrays.asList("Blog.WebHome", "News.WebHome"));
        XWikiException cause = new XWikiException(0, 0, "Save failure");
        doThrow(cause).when(this.xwiki).saveDocument(this.document, SAVE_COMMENT, this.context);

        Exception exception = assertThrows(Exception.class, () -> this.migration.execute(WIKI));

        assertEquals("Failed to migrate the blog titles.", exception.getMessage());
        assertSame(cause, exception.getCause());
        verify(this.xwiki, never()).getDocument(news, this.context);
        assertEquals(0, this.logCapture.size());
    }
}
