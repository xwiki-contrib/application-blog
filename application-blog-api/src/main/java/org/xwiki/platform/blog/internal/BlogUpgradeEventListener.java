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

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.platform.blog.BlogVisibilityMigration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * React to the upgrade of the blog application by starting the blog post visibility migration.
 *
 * @version $Id$
 *
 * @since 9.0RC1
 * @since 8.4.3
 * @since 7.4.6
 */
@Component
@Singleton
@Named(BlogUpgradeEventListener.NAME)
public class BlogUpgradeEventListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "Blog Upgrade Listener";

    /**
     * Current ID of the Blog Application.
     */
    private static final String EXTENSION_ID = "org.xwiki.contrib.blog:application-blog-ui";

    /**
     * Previous ID of the Blog Application.
     */
    private static final String PREVIOUS_EXTENSION_ID = "org.xwiki.platform:xwiki-platform-blog-ui";

    /**
     * The visibility is synchronized since 7.4.6, 8.4.3 and 9.0RC1, so we do the migration only if the previous
     * version was anterior, ie matches the following constraint.
     */
    private static final VersionConstraint VERSION_CONSTRAINT = new DefaultVersionConstraint("(,7.4.6),[8.0,8.4.3)");

    /**
     * The blog sheet stopped evaluating the stored blog title in 9.15.11 (BLOG-265), so blogs upgraded from an
     * earlier version may still store the default title expression and need the title migration.
     */
    private static final VersionConstraint TITLE_VERSION_CONSTRAINT = new DefaultVersionConstraint("(,9.15.11)");

    @Inject
    private BlogVisibilityMigration blogVisibilityMigration;

    @Inject
    private BlogTitleMigration blogTitleMigration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    /**
     * Construct a BlogUpgradeEventListener.
     */
    public BlogUpgradeEventListener()
    {
        super(NAME, new ExtensionUpgradedEvent(EXTENSION_ID));
    }

    @Override
    public void onEvent(Event event, Object installedExtension, Object previousExtensions)
    {
        ExtensionUpgradedEvent extensionUpgradedEvent = (ExtensionUpgradedEvent) event;

        Collection<InstalledExtension> previous = (Collection<InstalledExtension>) previousExtensions;

        boolean visibilityMigrationNeeded = isConstraintMatched(previous, PREVIOUS_EXTENSION_ID, VERSION_CONSTRAINT);
        // The blog title existed under both the current and the previous (platform) extension id, so check both.
        boolean titleMigrationNeeded = isConstraintMatched(previous, EXTENSION_ID, TITLE_VERSION_CONSTRAINT)
            || isConstraintMatched(previous, PREVIOUS_EXTENSION_ID, TITLE_VERSION_CONSTRAINT);

        if (visibilityMigrationNeeded || titleMigrationNeeded) {
            String namespace = extensionUpgradedEvent.getNamespace();
            if (namespace == null) {
                // When the namespace is null, it means the application is installed on the root namespace, ie. on
                // the farm.
                migrateAllWikis(visibilityMigrationNeeded, titleMigrationNeeded);
            } else if (namespace.startsWith("wiki:")) {
                migrateWiki(new WikiReference(namespace.substring(5)), visibilityMigrationNeeded, titleMigrationNeeded);
            }
        }
    }

    private void migrateAllWikis(boolean visibilityMigrationNeeded, boolean titleMigrationNeeded)
    {
        try {
            for (String wikiId : wikiDescriptorManager.getAllIds()) {
                migrateWiki(new WikiReference(wikiId), visibilityMigrationNeeded, titleMigrationNeeded);
            }
        } catch (WikiManagerException e) {
            logger.warn("Failed to migrate the blogs.", e);
        }
    }

    private void migrateWiki(WikiReference wikiReference, boolean visibilityMigrationNeeded,
        boolean titleMigrationNeeded)
    {
        if (visibilityMigrationNeeded) {
            try {
                blogVisibilityMigration.execute(wikiReference);
            } catch (Exception e) {
                logger.warn("Failed to migrate the visibility of non published blog posts on the wiki [{}].",
                    wikiReference.getName(), e);
            }
        }
        if (titleMigrationNeeded) {
            try {
                blogTitleMigration.execute(wikiReference);
            } catch (Exception e) {
                logger.warn("Failed to migrate the blog titles on the wiki [{}].", wikiReference.getName(), e);
            }
        }
    }

    private boolean isConstraintMatched(Collection<InstalledExtension> previousExtensions, String extensionId,
        VersionConstraint constraint)
    {
        Version previousVersion = getPreviousVersion(previousExtensions, extensionId);
        return previousVersion != null && constraint.containsVersion(previousVersion);
    }

    private Version getPreviousVersion(Collection<InstalledExtension> previousExtensions, String extensionId)
    {
        for (InstalledExtension extension : previousExtensions) {
            if (extension.getId().getId().equals(extensionId)) {
                return extension.getId().getVersion();
            }
        }
        return null;
    }
}
