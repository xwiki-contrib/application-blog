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
@UITest
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
    class NestedBlogLocationIT extends BlogLocationIT
    {
    }

    @Nested
    class NestedBlogManagementIT extends BlogManagementIT
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
