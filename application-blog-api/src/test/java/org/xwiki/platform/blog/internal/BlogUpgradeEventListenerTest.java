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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.blog.BlogVisibilityMigration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BlogUpgradeEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
class BlogUpgradeEventListenerTest
{
    private static final String EXTENSION_ID = "org.xwiki.contrib.blog:application-blog-ui";

    private static final String PREVIOUS_EXTENSION_ID = "org.xwiki.platform:xwiki-platform-blog-ui";

    private static final WikiReference WIKI = new WikiReference("chocolate");

    @InjectMockComponents
    private BlogUpgradeEventListener listener;

    @MockComponent
    private BlogVisibilityMigration blogVisibilityMigration;

    @MockComponent
    private BlogTitleMigration blogTitleMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private static InstalledExtension extension(String id, String version)
    {
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(new ExtensionId(id, version));
        return extension;
    }

    private static ExtensionUpgradedEvent event(String namespace)
    {
        return new ExtensionUpgradedEvent(new ExtensionId(EXTENSION_ID, "9.15.12"), namespace);
    }

    private void assertTitleMigrationFromContribVersion(String previousVersion, boolean executedExpected)
        throws Exception
    {
        InstalledExtension previous = mock(InstalledExtension.class);
        when(previous.getId())
            .thenReturn(new ExtensionId("org.xwiki.contrib.blog:application-blog-ui", previousVersion));

        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(
            new ExtensionId("org.xwiki.contrib.blog:application-blog-ui", "9.15.11"), "wiki:chocolate");

        this.listener.onEvent(event, null, Collections.singletonList(previous));

        if (executedExpected) {
            verify(this.blogTitleMigration).execute(new WikiReference("chocolate"));
        } else {
            verifyNoInteractions(this.blogTitleMigration);
        }
        // The visibility migration must never run for a contrib-to-contrib upgrade.
        verifyNoInteractions(this.blogVisibilityMigration);
    }

    @Test
    void titleMigrationFromPreviousVersion() throws Exception
    {
        assertTitleMigrationFromContribVersion("9.15.10", true);
    }

    @Test
    void titleMigrationFromSameVersion() throws Exception
    {
        assertTitleMigrationFromContribVersion("9.15.11", false);
    }

    private void assertVisibilityMigrationWithVersion(String version, boolean executedExpected) throws Exception
    {
        // Mocks
        InstalledExtension installedExtension1 = mock(InstalledExtension.class);
        InstalledExtension installedExtension2 = mock(InstalledExtension.class);
        InstalledExtension installedExtension3 = mock(InstalledExtension.class);

        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(
            new ExtensionId("org.xwiki.contrib.blog:application-blog-ui", "9.0"), "wiki:chocolate");

        when(installedExtension1.getId()).thenReturn(new ExtensionId("foo", "8"));
        when(installedExtension2.getId()).thenReturn(new ExtensionId("bar", "9.0"));
        when(installedExtension3.getId()).thenReturn(new ExtensionId("org.xwiki.platform:xwiki-platform-blog-ui",
            version));

        // Test
        this.listener.onEvent(event, null,
            Arrays.asList(installedExtension1, installedExtension2, installedExtension3));

        // Verify
        if (executedExpected) {
            verify(this.blogVisibilityMigration).execute(new WikiReference("chocolate"));
        } else {
            verifyNoInteractions(this.blogVisibilityMigration);
        }
    }

    @Test
    void onEventWithVersion82() throws Exception
    {
        assertVisibilityMigrationWithVersion("8.2", true);
    }

    @Test
    void onEventWithVersion51() throws Exception
    {
        assertVisibilityMigrationWithVersion("5.1", true);
    }

    @Test
    void onEventWithVersion746() throws Exception
    {
        assertVisibilityMigrationWithVersion("7.4.6", false);
    }

    @Test
    void onEventWithVersion843() throws Exception
    {
        assertVisibilityMigrationWithVersion("8.4.3", false);
    }

    @Test
    void onEventWithVersion90() throws Exception
    {
        assertVisibilityMigrationWithVersion("9.0", false);
    }

    @Test
    void onEventWithNoBlogInstalled()
    {
        // Mocks
        InstalledExtension installedExtension1 = mock(InstalledExtension.class);

        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(
            new ExtensionId("org.xwiki.contrib.blog:application-blog-ui", "9.0"), "wiki:chocolate");

        when(installedExtension1.getId()).thenReturn(new ExtensionId("foobar", "8"));

        // Test
        this.listener.onEvent(event, null, Collections.singletonList(installedExtension1));

        // Verify
        verifyNoInteractions(this.blogVisibilityMigration);
    }

    @Test
    void onEventWithNoNamespace() throws Exception
    {
        // Mocks
        InstalledExtension installedExtension1 = mock(InstalledExtension.class);

        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(
            new ExtensionId("org.xwiki.contrib.blog:application-blog-ui", "9.0"), null);

        when(installedExtension1.getId()).thenReturn(new ExtensionId("org.xwiki.platform:xwiki-platform-blog-ui",
            "2.3"));

        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("wiki1", "wiki2"));

        // Test
        this.listener.onEvent(event, null, Collections.singletonList(installedExtension1));

        // Verify
        verify(this.blogVisibilityMigration).execute(new WikiReference("wiki1"));
        verify(this.blogVisibilityMigration).execute(new WikiReference("wiki2"));
    }

    @Test
    void listenerNameAndEvents()
    {
        assertEquals(BlogUpgradeEventListener.NAME, this.listener.getName());
        assertEquals(1, this.listener.getEvents().size());
        assertTrue(this.listener.getEvents().get(0).matches(new ExtensionUpgradedEvent(EXTENSION_ID)));
    }

    @ParameterizedTest
    @CsvSource({
        "7.4.5, true",
        "7.4.6, false",
        "7.9, false",
        "8.0, true",
        "8.4.2, true",
        "8.4.3, false"
    })
    void visibilityMigrationVersionBounds(String previousVersion, boolean executedExpected) throws Exception
    {
        this.listener.onEvent(event("wiki:chocolate"), null,
            Collections.singletonList(extension(PREVIOUS_EXTENSION_ID, previousVersion)));

        if (executedExpected) {
            verify(this.blogVisibilityMigration).execute(WIKI);
        } else {
            verifyNoInteractions(this.blogVisibilityMigration);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "9.0, true",
        "9.15.10, true",
        "9.15.11, false",
        "9.15.12, false",
        "10.0, false"
    })
    void titleMigrationVersionBounds(String previousVersion, boolean executedExpected) throws Exception
    {
        this.listener.onEvent(event("wiki:chocolate"), null,
            Collections.singletonList(extension(EXTENSION_ID, previousVersion)));

        if (executedExpected) {
            verify(this.blogTitleMigration).execute(WIKI);
        } else {
            verifyNoInteractions(this.blogTitleMigration);
        }
    }

    @Test
    void bothMigrationsFromOldPlatformVersion() throws Exception
    {
        this.listener.onEvent(event("wiki:chocolate"), null,
            Collections.singletonList(extension(PREVIOUS_EXTENSION_ID, "8.2")));

        // The visibility migration runs first, then the title migration.
        InOrder inOrder = inOrder(this.blogVisibilityMigration, this.blogTitleMigration);
        inOrder.verify(this.blogVisibilityMigration).execute(WIKI);
        inOrder.verify(this.blogTitleMigration).execute(WIKI);
    }

    @Test
    void titleMigrationFromPlatformVersionWithoutVisibilityMigration() throws Exception
    {
        this.listener.onEvent(event("wiki:chocolate"), null,
            Collections.singletonList(extension(PREVIOUS_EXTENSION_ID, "9.0")));

        verify(this.blogTitleMigration).execute(WIKI);
        verifyNoInteractions(this.blogVisibilityMigration);
    }

    @Test
    void onEventWithNoPreviousExtension()
    {
        this.listener.onEvent(event("wiki:chocolate"), null, Collections.emptyList());

        verifyNoInteractions(this.blogVisibilityMigration, this.blogTitleMigration, this.wikiDescriptorManager);
    }

    @Test
    void onEventWithNonWikiNamespace()
    {
        this.listener.onEvent(event("user:chocolate:XWiki.Alice"), null,
            Collections.singletonList(extension(PREVIOUS_EXTENSION_ID, "8.2")));

        verifyNoInteractions(this.blogVisibilityMigration, this.blogTitleMigration, this.wikiDescriptorManager);
    }

    @Test
    void titleMigrationOnAllWikisWithNoNamespace() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("wiki1", "wiki2"));

        this.listener.onEvent(event(null), null, Collections.singletonList(extension(EXTENSION_ID, "9.15.10")));

        verify(this.blogTitleMigration).execute(new WikiReference("wiki1"));
        verify(this.blogTitleMigration).execute(new WikiReference("wiki2"));
        verifyNoInteractions(this.blogVisibilityMigration);
    }

    @Test
    void onEventWithNoNamespaceWhenWikisCannotBeListed() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenThrow(new WikiManagerException("Failed to list wikis"));

        this.listener.onEvent(event(null), null,
            Collections.singletonList(extension(PREVIOUS_EXTENSION_ID, "8.2")));

        verifyNoInteractions(this.blogVisibilityMigration, this.blogTitleMigration);
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to migrate the blogs.", this.logCapture.getMessage(0));
    }

    @Test
    void titleMigrationRunsWhenVisibilityMigrationFails() throws Exception
    {
        doThrow(new Exception("Visibility failure")).when(this.blogVisibilityMigration).execute(any());

        this.listener.onEvent(event("wiki:chocolate"), null,
            Collections.singletonList(extension(PREVIOUS_EXTENSION_ID, "8.2")));

        verify(this.blogTitleMigration).execute(WIKI);
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to migrate the visibility of non published blog posts on the wiki [chocolate].",
            this.logCapture.getMessage(0));
    }

    @Test
    void titleMigrationFailureIsLoggedAndOtherWikisAreMigrated() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("wiki1", "wiki2"));
        doThrow(new Exception("Title failure")).when(this.blogTitleMigration).execute(new WikiReference("wiki1"));

        this.listener.onEvent(event(null), null, Collections.singletonList(extension(EXTENSION_ID, "9.15.10")));

        verify(this.blogTitleMigration).execute(new WikiReference("wiki2"));
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to migrate the blog titles on the wiki [wiki1].", this.logCapture.getMessage(0));
    }
}
