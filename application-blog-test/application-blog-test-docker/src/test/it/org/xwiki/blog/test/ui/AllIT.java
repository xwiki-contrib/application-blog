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
package org.xwiki.blog.test.ui;

import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Blog application, run as nested classes so that XWiki is started only once.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The feed of all the blog posts of the wiki uses the Feed plugin.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.feed.FeedPlugin"
    },
    extraJARs = {
        // A plugin listed in xwiki.cfg is loaded at startup, before extensions get installed, so its JAR has to be
        // in WEB-INF/lib.
        "org.xwiki.platform:xwiki-platform-feed-api"
    },
    // Resolve the version of the extra JAR from the dependencies of the POM (it's not the version of this module).
    resolveExtraJARs = true
)
class AllIT
{
    @Nested
    class NestedBlogArchiveAndCategoriesIT extends BlogArchiveAndCategoriesIT
    {
    }

    @Nested
    class NestedBlogArchiveIT extends BlogArchiveIT
    {
    }

    @Nested
    class NestedBlogCalendarDisplayIT extends BlogCalendarDisplayIT
    {
    }

    @Nested
    class NestedBlogCategoriesIT extends BlogCategoriesIT
    {
    }

    @Nested
    class NestedBlogCategoriesLocationMigrationIT extends BlogCategoriesLocationMigrationIT
    {
    }

    @Nested
    class NestedBlogFeedIT extends BlogFeedIT
    {
    }

    @Nested
    class NestedBlogLocationIT extends BlogLocationIT
    {
    }

    @Nested
    class NestedBlogManagementIT extends BlogManagementIT
    {
    }

    @Nested
    class NestedBlogMigrationIT extends BlogMigrationIT
    {
    }

    @Nested
    class NestedBlogPanelsConfigurationIT extends BlogPanelsConfigurationIT
    {
    }

    @Nested
    class NestedBlogPanelsIT extends BlogPanelsIT
    {
    }

    @Nested
    class NestedBlogPostIT extends BlogPostIT
    {
    }

    @Nested
    class NestedBlogPostListMacroIT extends BlogPostListMacroIT
    {
    }

    @Nested
    class NestedBlogPostVisibilityIT extends BlogPostVisibilityIT
    {
    }

    @Nested
    class NestedBlogTitleSecurityIT extends BlogTitleSecurityIT
    {
    }
}
