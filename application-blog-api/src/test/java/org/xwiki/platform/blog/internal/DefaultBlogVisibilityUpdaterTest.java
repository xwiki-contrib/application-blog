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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultBlogVisibilityUpdater}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultBlogVisibilityUpdaterTest
{
    private static final EntityReference RIGHTS_CLASS = new EntityReference("XWikiRights", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    private static final DocumentReference BLOG_POST_REFERENCE =
        new DocumentReference("chocolate", "Blog", "HelloWorld");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("chocolate", "XWiki", "Alice");

    private static final String USER = "chocolate:XWiki.Alice";

    @InjectMockComponents
    private DefaultBlogVisibilityUpdater updater;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext context;

    private XWikiDocument document;

    private BaseObject blogPostObject;

    private List<BaseObject> rightsObjects;

    @BeforeEach
    void setUp()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getUserReference()).thenReturn(USER_REFERENCE);
        when(this.serializer.serialize(USER_REFERENCE)).thenReturn(USER);
        when(this.serializer.serialize(BLOG_POST_REFERENCE)).thenReturn("chocolate:Blog.HelloWorld");

        this.document = mock(XWikiDocument.class);
        this.blogPostObject = mock(BaseObject.class);
        this.rightsObjects = new ArrayList<>();
        mockDocument(BLOG_POST_REFERENCE);
    }

    private void mockDocument(DocumentReference reference)
    {
        when(this.document.getDocumentReference()).thenReturn(reference);
        when(this.document.getXObject(new DocumentReference("chocolate", "Blog", "BlogPostClass")))
            .thenReturn(this.blogPostObject);
        when(this.document.getXObjects(RIGHTS_CLASS)).thenReturn(this.rightsObjects);
        // BaseObject#equals() requires a component manager, so the removal is based on identity.
        doAnswer(invocation -> this.rightsObjects.removeIf(object -> object == invocation.getArgument(0)))
            .when(this.document).removeXObject(any());
    }

    private void mockBlogPost(boolean published, boolean hidden)
    {
        when(this.blogPostObject.getIntValue("published")).thenReturn(published ? 1 : 0);
        when(this.blogPostObject.getIntValue("hidden")).thenReturn(hidden ? 1 : 0);
    }

    private BaseObject rightsObject(int allow, String users, String groups, String levels)
    {
        BaseObject rights = new BaseObject();
        rights.setIntValue("allow", allow);
        rights.setLargeStringValue("users", users);
        rights.setLargeStringValue("groups", groups);
        rights.setStringValue("levels", levels);
        return rights;
    }

    private void assertRightsObjects(BaseObject... expected)
    {
        assertEquals(expected.length, this.rightsObjects.size());
        for (int i = 0; i < expected.length; i++) {
            assertSame(expected[i], this.rightsObjects.get(i));
        }
    }

    private BaseObject viewRestriction()
    {
        return rightsObject(1, USER, "", "view");
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, true",
        "false, true, true",
        "true, false, false",
        "true, true, true"
    })
    void synchronizeHiddenMetadataSetsHiddenFlag(boolean published, boolean hidden, boolean expectedHidden)
        throws Exception
    {
        // Keep the restriction already present so that no new rights object needs to be created.
        this.rightsObjects.add(viewRestriction());
        mockBlogPost(published, hidden);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(expectedHidden);
    }

    @Test
    void synchronizeHiddenMetadataWhenNoBlogPostObject()
    {
        when(this.document.getXObject(new DocumentReference("chocolate", "Blog", "BlogPostClass")))
            .thenReturn(null);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document, never()).setHidden(anyBoolean());
        verify(this.document, never()).getXObjects(any(EntityReference.class));
    }

    @Test
    void synchronizeHiddenMetadataIgnoresBlogPostTemplate()
    {
        mockDocument(new DocumentReference("chocolate", "Blog", "BlogPostTemplate"));
        mockBlogPost(false, false);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document, never()).setHidden(anyBoolean());
        verify(this.document, never()).getXObjects(any(EntityReference.class));
    }

    @Test
    void synchronizeHiddenMetadataRestrictsViewToCurrentUser() throws Exception
    {
        mockBlogPost(false, false);

        BaseObject newRights = mock(BaseObject.class);
        when(this.document.newXObject(RIGHTS_CLASS, this.context)).thenReturn(newRights);
        BaseClass rightsClass = mock(BaseClass.class);
        when(newRights.getXClass(this.context)).thenReturn(rightsClass);

        PropertyClass usersClass = mock(PropertyClass.class);
        when(rightsClass.get("users")).thenReturn(usersClass);
        BaseProperty<?> usersProperty = mock(BaseProperty.class);
        when(usersClass.fromStringArray(new String[] { USER })).thenReturn(usersProperty);
        when(usersProperty.getValue()).thenReturn(USER);

        PropertyClass levelsClass = mock(PropertyClass.class);
        when(rightsClass.get("levels")).thenReturn(levelsClass);
        BaseProperty<?> levelsProperty = mock(BaseProperty.class);
        when(levelsClass.fromStringArray(new String[] { "view" })).thenReturn(levelsProperty);
        when(levelsProperty.getValue()).thenReturn(Arrays.asList("view"));

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(true);
        verify(newRights).set("allow", 1, this.context);
        verify(newRights).set("users", USER, this.context);
        verify(newRights).set("levels", Arrays.asList("view"), this.context);
    }

    @Test
    void synchronizeHiddenMetadataDoesNotDuplicateExistingRestriction() throws Exception
    {
        BaseObject restriction = viewRestriction();
        this.rightsObjects.add(restriction);
        mockBlogPost(true, true);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(true);
        verify(this.document, never()).newXObject(any(EntityReference.class), any(XWikiContext.class));
        assertRightsObjects(restriction);
    }

    @Test
    void synchronizeHiddenMetadataLogsWarningWhenRestrictionCannotBeCreated() throws Exception
    {
        mockBlogPost(false, false);
        when(this.document.newXObject(RIGHTS_CLASS, this.context))
            .thenThrow(new XWikiException(0, 0, "Failed to create object"));

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(true);
        assertEquals(1, this.logCapture.size());
        assertEquals("could not set/clear user rights on blog post [chocolate:Blog.HelloWorld]",
            this.logCapture.getMessage(0));
    }

    @Test
    void synchronizeHiddenMetadataRemovesRestrictionWhenPublished()
    {
        BaseObject otherUser = rightsObject(1, "chocolate:XWiki.Bob", "", "view");
        BaseObject deny = rightsObject(0, USER, "", "view");
        BaseObject otherLevel = rightsObject(1, USER, "", "edit");
        BaseObject withGroups = rightsObject(1, USER, "XWiki.XWikiAdminGroup", "view");
        this.rightsObjects.addAll(Arrays.asList(otherUser, null, viewRestriction(), deny, otherLevel, withGroups,
            viewRestriction()));
        mockBlogPost(true, false);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(false);
        verify(this.document, times(2)).removeXObject(any(BaseObject.class));
        assertRightsObjects(otherUser, null, deny, otherLevel, withGroups);
    }

    @Test
    void synchronizeHiddenMetadataWhenPublishedWithoutRightsObjects()
    {
        when(this.document.getXObjects(RIGHTS_CLASS)).thenReturn(null);
        mockBlogPost(true, false);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(false);
        verify(this.document, never()).removeXObject(any(BaseObject.class));
    }

    @Test
    void synchronizeHiddenMetadataLogsErrorWhenRestrictionCannotBeRemoved()
    {
        BaseObject restriction = viewRestriction();
        this.rightsObjects.add(restriction);
        doReturn(false).when(this.document).removeXObject(restriction);
        mockBlogPost(true, false);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(false);
        // The removal is retried a bounded number of times instead of looping forever.
        verify(this.document, times(5)).removeXObject(restriction);
        assertEquals(5, this.logCapture.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("failed to remove visibility restriction for blog post [chocolate:Blog.HelloWorld] "
                + "on publication", this.logCapture.getMessage(i));
        }
    }
}
