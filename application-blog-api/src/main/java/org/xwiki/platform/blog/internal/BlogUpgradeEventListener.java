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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
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

import com.xpn.xwiki.XWikiContext;

/**
 * React to the upgrade of the blog application by starting the blog post visibility migration.
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.3
 * @since 7.4.6
 */
@Component
@Singleton
@Named(BlogUpgradeEventListener.NAME)
public class BlogUpgradeEventListener extends AbstractEventListener implements Initializable
{
    /**
     * Name of the listener.
     */
    protected static final String NAME = "Blog Upgrade Listener";

    /**
     * Current ID of the Blog Application.
     */
    private static final String EXTENSION_ID = "org.xwiki.contrib.blog:application-blog-ui";

    /**
     * TODO: Filter also ExtensionInstalledEvent by blog extension id after "XCOMMONS-2526: Provide constructors
     * for ExtensionInstalledEvent that does not take namespace" is fixed and blog starts depending on a
     * version of XWiki >= the version where is fixed.
     */
    private static final List<Event> EVENTS = Arrays.asList(new ExtensionUpgradedEvent(EXTENSION_ID),
        new ExtensionInstalledEvent());

    /**
     * Previous ID of the Blog Application.
     */
    private static final String PREVIOUS_EXTENSION_ID = "org.xwiki.platform:xwiki-platform-blog-ui";

    /**
     * The visibility is synchronized since 7.4.6, 8.4.3 and 9.0RC1, so we do the migration only if the previous version
     * was anterior, ie matches the following constraint.
     */
    private static final VersionConstraint VERSION_CONSTRAINT = new DefaultVersionConstraint("(,7.4.6),[8.0,8.4.3)");

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    private BlogVisibilityMigration blogVisibilityMigration;

    @Inject
    private CategoryLocationMigration categoryLocationMigration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Inject
    private ComponentManager componentManager;

    /**
     * Construct a BlogUpgradeEventListener.
     */
    public BlogUpgradeEventListener()
    {
        super(NAME, EVENTS);
    }

    /**
     * The migration should be done at ExtensionUpgradedEvent, but for avoiding "XCOMMONS-751: Getting wrong component
     * instance during JAR extension upgrade", it is done also at initialization step, since when an extension is
     * upgraded its listeners are initialized too. After the issue is fixed and blog starts depending on a version of
     * XWiki >= the version where is fixed, then only the migration from inside the event should be executed.
     */
    @Override
    public void initialize()
    {
        // Don't trigger the migration process at xwiki startup time.
        if (xcontextProvider.get() != null) {
            boolean hasNamespace = componentManager instanceof NamespacedComponentManager;
            getTargetWikis(
                hasNamespace ? ((NamespacedComponentManager) componentManager).getNamespace() : null).forEach(
                    targetWiki -> migrate(targetWiki, null));
        }
    }

    @Override
    public void onEvent(Event event, Object installedExtension, Object previousExtensions)
    {
        if (event instanceof ExtensionUpgradedEvent || isBlogInstallEvent(event)) {
            ExtensionUpgradedEvent extensionUpgradedEvent = (ExtensionUpgradedEvent) event;

            getTargetWikis(
                extensionUpgradedEvent.hasNamespace() ? extensionUpgradedEvent.getNamespace() : null).forEach(
                    targetWiki -> migrate(targetWiki, previousExtensions));
        }
    }

    private static boolean isBlogInstallEvent(Event event)
    {
        return event instanceof ExtensionInstalledEvent && EXTENSION_ID.equals(
            ((ExtensionEvent) event).getExtensionId().getId());
    }

    private Collection<String> getTargetWikis(String namespace)
    {
        // Checking also for null namespace since it could mean that the upgrade is done on farm level.
        if (namespace != null && namespace.startsWith("wiki:")) {
            return Collections.singleton(namespace.substring(5));
        } else {
            try {
                return this.wikiDescriptorManager.getAllIds();
            } catch (WikiManagerException e) {
                this.logger.error("Failed to get the list of wikis.", e);
                return Collections.emptySet();
            }
        }
    }

    private void migrate(String wikiId, Object previousExtensions)
    {
        Version previousVersion = getPreviousVersion(
            previousExtensions != null ? (Collection<InstalledExtension>) previousExtensions : Collections.emptyList());

        if (previousVersion != null && VERSION_CONSTRAINT.containsVersion(previousVersion)) {
            WikiReference wikiReference = new WikiReference(wikiId);
            try {
                blogVisibilityMigration.execute(wikiReference);
            } catch (Exception e) {
                logger.warn("Failed to updateBlogPosts the visibility of non published blog posts on the wiki [{}].",
                    wikiReference.getName(), e);
            }
        }
        this.categoryLocationMigration.migrate(wikiId);
    }

    private Version getPreviousVersion(Collection<InstalledExtension> previousExtensions)
    {
        for (InstalledExtension extension : previousExtensions) {
            if (extension.getId().getId().equals(PREVIOUS_EXTENSION_ID)) {
                return extension.getId().getVersion();
            }
        }
        // Should never happen
        return null;
    }
}
