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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BlogMoveListener}.
 *
 * @version $Id$
 */
@ComponentTest
class BlogMoveListenerTest
{
    private static final DocumentReference OLD_BLOG_REFERENCE = new DocumentReference("wiki", "MyBlog", "WebHome");

    private static final DocumentReference NEW_BLOG_REFERENCE =
        new DocumentReference("wiki", Arrays.asList("TestPage", "MyBlog"), "WebHome");

    private static final String POSTS_LOCATION = "postsLocation";

    private static final String CATEGORIES_LOCATION = "categoriesLocation";

    @InjectMockComponents
    private BlogMoveListener listener;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    private XWiki xwiki;

    private XWikiDocument blogDoc;

    private BaseObject blogObj;

    @BeforeEach
    void setUp() throws Exception
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        this.xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        this.blogDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(NEW_BLOG_REFERENCE, this.context)).thenReturn(this.blogDoc);
        this.blogObj = mock(BaseObject.class);

        when(this.localSerializer.serialize(new SpaceReference("wiki", "MyBlog"))).thenReturn("MyBlog");
        when(this.localSerializer.serialize(new SpaceReference("wiki", "TestPage", "MyBlog")))
            .thenReturn("TestPage.MyBlog");
    }

    private void moveBlog()
    {
        this.listener.onEvent(new DocumentRenamedEvent(OLD_BLOG_REFERENCE, NEW_BLOG_REFERENCE), null, null);
    }

    @Test
    void onEventUpdatesTheLocationsUnderTheMovedBlog() throws Exception
    {
        when(this.blogDoc.getXObject(new LocalDocumentReference("Blog", "BlogClass"))).thenReturn(this.blogObj);
        when(this.blogObj.getStringValue(POSTS_LOCATION)).thenReturn("MyBlog");
        when(this.blogObj.getStringValue(CATEGORIES_LOCATION)).thenReturn("MyBlog.Categories");

        moveBlog();

        verify(this.blogObj).setStringValue(POSTS_LOCATION, "TestPage.MyBlog");
        verify(this.blogObj).setStringValue(CATEGORIES_LOCATION, "TestPage.MyBlog.Categories");
        verify(this.xwiki).saveDocument(this.blogDoc, this.context);
    }

    @Test
    void onEventKeepsTheLocationsOutsideTheMovedBlog() throws Exception
    {
        when(this.blogDoc.getXObject(new LocalDocumentReference("Blog", "BlogClass"))).thenReturn(this.blogObj);
        when(this.blogObj.getStringValue(POSTS_LOCATION)).thenReturn("");
        when(this.blogObj.getStringValue(CATEGORIES_LOCATION)).thenReturn("MyBlogCategories");

        moveBlog();

        verify(this.blogObj, never()).setStringValue(anyString(), anyString());
        verify(this.xwiki, never()).saveDocument(any(), any());
    }

    @Test
    void onEventIgnoresPagesThatAreNotBlogs() throws Exception
    {
        moveBlog();

        verify(this.xwiki, never()).saveDocument(any(), any());
    }
}
