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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.event.DocumentRenamingEvent;
import org.xwiki.security.authorization.Right;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.xwiki.platform.blog.internal.InMemoryBlogQueries.BLOG_POST_CLASS;
import static org.xwiki.platform.blog.internal.InMemoryBlogQueries.CATEGORY;
import static org.xwiki.platform.blog.internal.InMemoryBlogQueries.CATEGORY_CLASS;

/**
 * Unit tests for {@link CategoryRenameListener}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class CategoryRenameListenerTest
{
    private static final List<String> CATEGORIES_SPACE = Arrays.asList("Blog", "Categories");

    private static final DocumentReference OLD_CATEGORY = new DocumentReference("xwiki", CATEGORIES_SPACE, "Old");

    private static final DocumentReference NEW_CATEGORY = new DocumentReference("xwiki", CATEGORIES_SPACE, "New");

    private static final String PARENT_CATEGORY = "Blog.Categories.Parent";

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "Renamer");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private CategoryRenameListener listener;

    @MockComponent
    private QueryManager queryManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWiki xwiki;

    private XWikiContext context;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki = this.oldcore.getSpyXWiki();
        this.context = this.oldcore.getXWikiContext();
        new InMemoryBlogQueries(this.oldcore).answer(this.queryManager);
        this.context.setUserReference(USER);
    }

    private void allowEdit(DocumentReference reference)
    {
        when(this.oldcore.getMockAuthorizationManager().hasAccess(Right.EDIT, USER, reference)).thenReturn(true);
    }

    private XWikiDocument saveDocument(DocumentReference reference, String parent, LocalDocumentReference xclass)
        throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(reference);
        document.setParent(parent);
        if (xclass != null) {
            BaseObject object = new BaseObject();
            object.setXClassReference(xclass);
            document.addXObject(object);
        }
        this.xwiki.saveDocument(document, this.context);
        return document;
    }

    private void saveBlogPost(DocumentReference reference, String... categories) throws XWikiException
    {
        XWikiDocument post = saveDocument(reference, "Blog.WebHome", BLOG_POST_CLASS);
        post.getXObject(BLOG_POST_CLASS).setStringListValue(CATEGORY, Arrays.asList(categories));
        this.xwiki.saveDocument(post, this.context);
    }

    private List<?> getCategories(DocumentReference blogPost) throws XWikiException
    {
        return this.xwiki.getDocument(blogPost, this.context).getXObject(BLOG_POST_CLASS).getListValue(CATEGORY);
    }

    private String getParent(DocumentReference reference) throws XWikiException
    {
        return this.xwiki.getDocument(reference, this.context).getParent();
    }

    /**
     * Fire the events of a rename and move the document in between, the way a rename does, leaving it with the given
     * parent.
     */
    private void rename(DocumentReference source, DocumentReference target, String parentAfterRename)
        throws XWikiException
    {
        this.listener.onEvent(new DocumentRenamingEvent(source, target), null, null);

        XWikiDocument sourceDocument = this.xwiki.getDocument(source, this.context);
        XWikiDocument targetDocument = sourceDocument.copyDocument(target, this.context);
        targetDocument.setParent(parentAfterRename);
        this.xwiki.saveDocument(targetDocument, this.context);
        this.xwiki.deleteDocument(sourceDocument, this.context);

        this.listener.onEvent(new DocumentRenamedEvent(source, target), null, null);
    }

    @Test
    void listensToTheRenameOfAnyDocument()
    {
        List<Event> events = this.listener.getEvents();

        assertEquals("CategoryRenameListener", this.listener.getName());
        assertEquals(2, events.size());
        assertTrue(events.get(0).matches(new DocumentRenamedEvent(OLD_CATEGORY, NEW_CATEGORY)));
        assertTrue(events.get(1).matches(new DocumentRenamingEvent(OLD_CATEGORY, NEW_CATEGORY)));
    }

    @Test
    void renamingACategoryKeepsItsParent() throws Exception
    {
        saveDocument(OLD_CATEGORY, PARENT_CATEGORY, CATEGORY_CLASS);

        rename(OLD_CATEGORY, NEW_CATEGORY, "Blog.Categories.WebHome");

        XWikiDocument newCategory = this.xwiki.getDocument(NEW_CATEGORY, this.context);
        assertEquals(new DocumentReference("xwiki", CATEGORIES_SPACE, "Parent"), newCategory.getParentReference());
        // The parent is stored with its wiki, so the queries looking for the subcategories of the parent by its local
        // name (when renaming or deleting it) miss this category. It should be stored as it was before the rename
        // (Blog.Categories.Parent): BLOG-274 "Renaming a blog category stores its parent with the wiki prefix".
        assertEquals("xwiki:" + PARENT_CATEGORY, newCategory.getParent());
    }

    @Test
    void renamingACategoryUpdatesTheBlogPostsInIt() throws Exception
    {
        saveDocument(OLD_CATEGORY, PARENT_CATEGORY, CATEGORY_CLASS);
        DocumentReference postInCategory = new DocumentReference("xwiki", "Blog", "PostInCategory");
        saveBlogPost(postInCategory, "Blog.Categories.First", "Blog.Categories.Old", "Blog.Categories.Last");
        DocumentReference otherPost = new DocumentReference("xwiki", "Blog", "OtherPost");
        saveBlogPost(otherPost, "Blog.Categories.First");
        allowEdit(postInCategory);
        allowEdit(otherPost);

        rename(OLD_CATEGORY, NEW_CATEGORY, PARENT_CATEGORY);

        // The renamed category moves to the end of the post's categories. It should keep its position: BLOG-273
        // "Renaming or migrating a blog category moves it to the end of the categories of its blog posts".
        assertEquals(Arrays.asList("Blog.Categories.First", "Blog.Categories.Last", "Blog.Categories.New"),
            getCategories(postInCategory));
        assertEquals(Arrays.asList("Blog.Categories.First"), getCategories(otherPost));
    }

    @Test
    void renamingACategoryDoesNotUpdateThePostsTheUserCannotEdit() throws Exception
    {
        saveDocument(OLD_CATEGORY, PARENT_CATEGORY, CATEGORY_CLASS);
        DocumentReference editablePost = new DocumentReference("xwiki", "Blog", "EditablePost");
        saveBlogPost(editablePost, "Blog.Categories.Old");
        DocumentReference readOnlyPost = new DocumentReference("xwiki", "Blog", "ReadOnlyPost");
        saveBlogPost(readOnlyPost, "Blog.Categories.First", "Blog.Categories.Old");
        allowEdit(editablePost);

        rename(OLD_CATEGORY, NEW_CATEGORY, PARENT_CATEGORY);

        assertEquals(Arrays.asList("Blog.Categories.New"), getCategories(editablePost));
        assertEquals(Arrays.asList("Blog.Categories.First", "Blog.Categories.Old"), getCategories(readOnlyPost));
        assertEquals(1, this.logCapture.size());
        assertEquals("The category of the blog post [xwiki:Blog.ReadOnlyPost] was not updated after the rename of "
            + "[xwiki:Blog.Categories.Old] to [xwiki:Blog.Categories.New] because the current user is not allowed to "
            + "edit it.", this.logCapture.getMessage(0));
    }

    @Test
    void renamingAPageThatIsNotACategoryChangesNothing() throws Exception
    {
        DocumentReference oldPage = new DocumentReference("xwiki", "Main", "Old");
        DocumentReference newPage = new DocumentReference("xwiki", "Main", "New");
        saveDocument(oldPage, "Main.Parent", null);
        // A blog post can't be in a page that is not a category, but if it was, it would be left as is.
        DocumentReference post = new DocumentReference("xwiki", "Blog", "Post");
        saveBlogPost(post, "Main.Old");

        rename(oldPage, newPage, "Main.WebHome");

        assertEquals("Main.WebHome", getParent(newPage));
        assertEquals(Arrays.asList("Main.Old"), getCategories(post));
    }

    @Test
    void renamingACategoryThatCannotBeLoaded() throws Exception
    {
        saveDocument(OLD_CATEGORY, PARENT_CATEGORY, CATEGORY_CLASS);
        doThrow(new XWikiException()).when(this.xwiki).getDocument(OLD_CATEGORY, this.context);

        this.listener.onEvent(new DocumentRenamingEvent(OLD_CATEGORY, NEW_CATEGORY), null, null);

        assertEquals("Failed to get the source category document", this.logCapture.getMessage(0));
    }

    @Test
    void renamingACategoryWhenTheBlogPostsCannotBeFound() throws Exception
    {
        saveDocument(OLD_CATEGORY, PARENT_CATEGORY, CATEGORY_CLASS);
        when(this.queryManager.createQuery(anyString(), any())).thenThrow(new QueryException("error", null, null));

        rename(OLD_CATEGORY, NEW_CATEGORY, "Blog.Categories.WebHome");

        assertEquals("Failed to update categories", this.logCapture.getMessage(0));
        // The parent is restored before looking for the blog posts.
        assertEquals(new DocumentReference("xwiki", CATEGORIES_SPACE, "Parent"),
            this.xwiki.getDocument(NEW_CATEGORY, this.context).getParentReference());
    }
}
