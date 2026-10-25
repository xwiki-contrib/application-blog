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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.BlogPostsMigrationPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify {@code Blog.BlogPostsMigration}, which sets the default category to the blog posts that have no category.
 *
 * @version $Id$
 */
@UITest
class BlogPostsMigrationIT
{
    private static final String WIKI = "xwiki";

    private static final String SPACE = "BlogPostsMigrationIT";

    @Test
    @Order(1)
    void startSetsTheDefaultCategory(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference post = new DocumentReference(WIKI, SPACE, "Started");
        data.createPost(post, "Started", "");
        try {
            BlogPostsMigrationPage migrationPage = BlogPostsMigrationPage.gotoPage();
            assertThat(migrationPage.getListedPosts(), hasItem(SPACE + ".Started"));
            migrationPage = migrationPage.start();

            assertThat(migrationPage.getListedPosts(), hasItem(SPACE + ".Started"));
            assertEquals("Blog.Categories.WebHome", data.getPostCategory(post));
        } finally {
            setup.rest().delete(post);
        }
    }

    @Test
    @Order(2)
    void startRequiresAValidFormToken(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference post = new DocumentReference(WIKI, SPACE, "Unconfirmed");
        data.createPost(post, "Unconfirmed", "");
        try {
            // The page is displayed as when it's opened without parameters: the migration can be started.
            BlogPostsMigrationPage migrationPage = BlogPostsMigrationPage.gotoPage("confirm=true");
            assertTrue(migrationPage.canStart());

            assertEquals("", data.getPostCategory(post));
        } finally {
            setup.rest().delete(post);
        }
    }
}
