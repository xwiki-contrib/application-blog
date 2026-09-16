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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BlogTitleMigration}.
 *
 * @version $Id$
 */
public class BlogTitleMigrationTest
{
    private static final String DEFAULT_TITLE = "$services.localization.render('blog.code.title')";

    private static final String TITLE_PROPERTY = "title";

    private static final WikiReference WIKI = new WikiReference("chocolate");

    private static final LocalDocumentReference BLOG_CLASS = new LocalDocumentReference("Blog", "BlogClass");

    private static final DocumentReference BLOG_HOME = new DocumentReference("chocolate", "Blog", "WebHome");

    @Rule
    public MockitoComponentMockingRule<BlogTitleMigration> mocker =
        new MockitoComponentMockingRule<>(BlogTitleMigration.class);

    private QueryManager queryManager;

    private DocumentReferenceResolver<String> referenceResolver;

    private Query query;

    private XWiki xwiki;

    private XWikiContext context;

    private XWikiDocument document;

    private BaseObject blogObject;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        referenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        Provider<XWikiContext> contextProvider =
            mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));

        context = mock(XWikiContext.class);
        xwiki = mock(XWiki.class);
        when(contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xwiki);

        query = mock(Query.class);
        when(queryManager.createQuery(any(String.class), eq(Query.XWQL))).thenReturn(query);
        when(query.bindValue(eq("defaultTitle"), any())).thenReturn(query);
        when(query.setWiki("chocolate")).thenReturn(query);

        when(referenceResolver.resolve("Blog.WebHome", WIKI)).thenReturn(BLOG_HOME);

        // The migration clones the loaded document before modifying it, so the clone is what gets saved.
        XWikiDocument loadedDocument = mock(XWikiDocument.class);
        document = mock(XWikiDocument.class);
        when(xwiki.getDocument(BLOG_HOME, context)).thenReturn(loadedDocument);
        when(loadedDocument.clone()).thenReturn(document);
        blogObject = mock(BaseObject.class);
        when(document.getXObject(BLOG_CLASS)).thenReturn(blogObject);
    }

    @Test
    public void executeClearsTheDefaultTitle() throws Exception
    {
        when(query.<String>execute()).thenReturn(Collections.singletonList("Blog.WebHome"));
        when(blogObject.getStringValue(TITLE_PROPERTY)).thenReturn(DEFAULT_TITLE);

        mocker.getComponentUnderTest().execute(WIKI);

        verify(blogObject).setStringValue(TITLE_PROPERTY, "");
        verify(xwiki).saveDocument(eq(document), any(String.class), eq(context));
    }

    @Test
    public void executeLeavesUserTitlesUntouched() throws Exception
    {
        // The query is bound to the default title, but we still double-check the stored value defensively.
        when(query.<String>execute()).thenReturn(Collections.singletonList("Blog.WebHome"));
        when(blogObject.getStringValue(TITLE_PROPERTY)).thenReturn("My custom blog");

        mocker.getComponentUnderTest().execute(WIKI);

        verify(blogObject, never()).setStringValue(eq(TITLE_PROPERTY), any());
        verify(xwiki, never()).saveDocument(any(XWikiDocument.class), any(String.class), any(XWikiContext.class));
    }

    @Test
    public void executeWithNoMatchingBlog() throws Exception
    {
        when(query.<String>execute()).thenReturn(Collections.emptyList());

        mocker.getComponentUnderTest().execute(WIKI);

        verify(xwiki, never()).saveDocument(any(XWikiDocument.class), any(String.class), any(XWikiContext.class));
    }
}
