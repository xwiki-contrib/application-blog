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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final DocumentReference CREATOR_REFERENCE = new DocumentReference("chocolate", "XWiki", "Bob");

    private static final String CREATOR = "chocolate:XWiki.Bob";

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
        when(this.serializer.serialize(CREATOR_REFERENCE)).thenReturn(CREATOR);
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
        mockSharing("", "");
    }

    private void mockSharing(String users, String groups)
    {
        when(this.blogPostObject.getStringValue("sharedUsers")).thenReturn(users);
        when(this.blogPostObject.getStringValue("sharedGroups")).thenReturn(groups);
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

    private BaseObject mockNewRightsObject(String user) throws Exception
    {
        return mockNewRightsObject(new String[] { user }, new String[0]);
    }

    /**
     * Mock the creation of a rights object. The mocked list properties hold their values joined with a comma.
     */
    private BaseObject mockNewRightsObject(String[] users, String[] groups) throws Exception
    {
        BaseObject newRights = mock(BaseObject.class);
        when(this.document.newXObject(RIGHTS_CLASS, this.context)).thenReturn(newRights);
        BaseClass rightsClass = mock(BaseClass.class);
        when(newRights.getXClass(this.context)).thenReturn(rightsClass);

        mockListProperty(rightsClass, "users", users);
        mockListProperty(rightsClass, "groups", groups);

        PropertyClass levelsClass = mock(PropertyClass.class);
        when(rightsClass.get("levels")).thenReturn(levelsClass);
        BaseProperty<?> levelsProperty = mock(BaseProperty.class);
        when(levelsClass.fromStringArray(new String[] { "view" })).thenReturn(levelsProperty);
        when(levelsProperty.getValue()).thenReturn(Arrays.asList("view"));

        return newRights;
    }

    private void mockListProperty(BaseClass rightsClass, String name, String[] values)
    {
        PropertyClass propertyClass = mock(PropertyClass.class);
        when(rightsClass.get(name)).thenReturn(propertyClass);
        BaseProperty<?> property = mock(BaseProperty.class);
        when(propertyClass.fromStringArray(values)).thenReturn(property);
        when(property.getValue()).thenReturn(String.join(",", values));
    }

    @Test
    void synchronizeHiddenMetadataRestrictsViewToCreatorAndSharedUsersAndGroups() throws Exception
    {
        mockBlogPost(false, false);
        // The creator is not repeated when the post is also shared with them.
        mockSharing("XWiki.Carol, XWiki.Dave|" + USER, "XWiki.Editors");
        BaseObject newRights =
            mockNewRightsObject(new String[] { USER, "XWiki.Carol", "XWiki.Dave" }, new String[] { "XWiki.Editors" });

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(true);
        verify(newRights).set("allow", 1, this.context);
        verify(newRights).set("users", USER + ",XWiki.Carol,XWiki.Dave", this.context);
        verify(newRights).set("groups", "XWiki.Editors", this.context);
        verify(newRights).set("levels", Arrays.asList("view"), this.context);
    }

    @Test
    void synchronizeHiddenMetadataKeepsRestrictionWhenSharingIsUnchanged() throws Exception
    {
        BaseObject restriction = rightsObject(1, USER + ",XWiki.Carol", "XWiki.Editors", "view");
        this.rightsObjects.add(restriction);
        mockBlogPost(false, false);
        mockSharing("XWiki.Carol", "XWiki.Editors");

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document, never()).newXObject(any(EntityReference.class), any(XWikiContext.class));
        assertRightsObjects(restriction);
    }

    @Test
    void synchronizeHiddenMetadataReplacesRestrictionWhenSharingChanges() throws Exception
    {
        // A single user view right, set by hand or by an older version for another editor, stays until publication.
        BaseObject deny = rightsObject(0, USER, "", "view");
        BaseObject otherUser = rightsObject(1, CREATOR, "", "view");
        this.rightsObjects.addAll(Arrays.asList(viewRestriction(), deny, otherUser));
        mockBlogPost(false, false);
        mockSharing("XWiki.Carol", "");
        BaseObject newRights = mockNewRightsObject(new String[] { USER, "XWiki.Carol" }, new String[0]);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(newRights).set("users", USER + ",XWiki.Carol", this.context);
        verify(newRights, never()).set(eq("groups"), any(), any(XWikiContext.class));
        assertRightsObjects(deny, otherUser);
    }

    @Test
    void isVisibilityRestrictionWithoutCreator()
    {
        assertTrue(DefaultBlogVisibilityUpdater.isVisibilityRestriction(rightsObject(1, CREATOR, "", "view"), null));
        assertFalse(DefaultBlogVisibilityUpdater.isVisibilityRestriction(
            rightsObject(1, CREATOR + ",XWiki.Carol", "", "view"), null));
        assertFalse(DefaultBlogVisibilityUpdater.isVisibilityRestriction(null, null));
    }

    @Test
    void synchronizeHiddenMetadataRestrictsViewToCurrentUserWhenNoCreator() throws Exception
    {
        mockBlogPost(false, false);
        BaseObject newRights = mockNewRightsObject(USER);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(true);
        verify(newRights).set("allow", 1, this.context);
        verify(newRights).set("users", USER, this.context);
        verify(newRights).set("levels", Arrays.asList("view"), this.context);
    }

    @Test
    void synchronizeHiddenMetadataRestrictsViewToCreator() throws Exception
    {
        when(this.document.getCreatorReference()).thenReturn(CREATOR_REFERENCE);
        mockBlogPost(false, false);
        BaseObject newRights = mockNewRightsObject(CREATOR);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(true);
        verify(newRights).set("allow", 1, this.context);
        verify(newRights).set("users", CREATOR, this.context);
        verify(newRights).set("levels", Arrays.asList("view"), this.context);
    }

    @Test
    void synchronizeHiddenMetadataDoesNotRestrictViewToOtherEditor() throws Exception
    {
        // Alice saves the draft that Bob created and restricted to himself (BLOG-172).
        when(this.document.getCreatorReference()).thenReturn(CREATOR_REFERENCE);
        BaseObject restriction = rightsObject(1, CREATOR, "", "view");
        this.rightsObjects.add(restriction);
        mockBlogPost(false, false);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(true);
        verify(this.document, never()).newXObject(any(EntityReference.class), any(XWikiContext.class));
        assertRightsObjects(restriction);
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
    void synchronizeHiddenMetadataRemovesRestrictionsOfAllEditorsWhenPublished()
    {
        // Alice is the creator. Only the rights that do not start with her and are not given to a single user stay.
        BaseObject deny = rightsObject(0, USER, "", "view");
        BaseObject otherLevel = rightsObject(1, USER, "", "edit");
        BaseObject withGroups = rightsObject(1, CREATOR, "XWiki.XWikiAdminGroup", "view");
        BaseObject severalUsers = rightsObject(1, CREATOR + ',' + USER, "", "view");
        BaseObject noUser = rightsObject(1, "", "", "view");
        BaseObject shared = rightsObject(1, USER + ",XWiki.Carol", "XWiki.Editors", "view");
        this.rightsObjects.addAll(Arrays.asList(rightsObject(1, CREATOR, "", "view"), null, viewRestriction(), deny,
            otherLevel, withGroups, severalUsers, noUser, shared, viewRestriction()));
        mockBlogPost(true, false);

        this.updater.synchronizeHiddenMetadata(this.document);

        verify(this.document).setHidden(false);
        verify(this.document, times(4)).removeXObject(any(BaseObject.class));
        assertRightsObjects(null, deny, otherLevel, withGroups, severalUsers, noUser);
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
        verify(this.document).removeXObject(restriction);
        assertEquals(1, this.logCapture.size());
        assertEquals("failed to remove visibility restriction for blog post [chocolate:Blog.HelloWorld]",
            this.logCapture.getMessage(0));
    }
}
