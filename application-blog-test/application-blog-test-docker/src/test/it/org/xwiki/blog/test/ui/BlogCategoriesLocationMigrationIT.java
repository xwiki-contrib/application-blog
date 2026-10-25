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

import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.CategoriesLocationMigratorPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify {@code Blog.Code.CategoriesLocationMigrator}, which starts the migration of the categories of the
 * {@code Blog} space to the {@code Blog.Categories} space.
 * <p>
 * The migration is run by the {@code job} macro, which needs the legacy rendering modules shipped by XWiki Standard but
 * not by the minimal instance used by these tests: the tests check how the page is displayed, not the migration itself
 * (see {@code BlogScriptServiceTest} for the rights required by the migration).
 *
 * @version $Id$
 */
@UITest
class BlogCategoriesLocationMigrationIT
{
    private static final String WIKI = "xwiki";

    private static final String BLOG = "Blog";

    private static final String SPACE = "BlogCategoriesLocationMigrationIT";

    @Test
    @Order(1)
    void startDisplaysTheStartedMigration(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference[] fixture = createFixture(data, "Started");
        try {
            CategoriesLocationMigratorPage migratorPage = CategoriesLocationMigratorPage.gotoPage();
            assertTrue(migratorPage.canStart());

            // The page displays the started migration, which can't be started again.
            assertFalse(migratorPage.start().canStart());
        } finally {
            delete(setup, data, fixture);
        }
    }

    @Test
    @Order(2)
    void startRequiresAValidFormToken(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        DocumentReference[] fixture = createFixture(data, "Unconfirmed");
        try {
            // The page is displayed as when it's opened without parameters: the migration can be started.
            assertTrue(CategoriesLocationMigratorPage.gotoPage("confirm=true&jobId=" + SPACE).canStart());
        } finally {
            delete(setup, data, fixture);
        }
    }

    /**
     * Creates a category of the {@code Blog} space and a blog post in it, so that the page proposes to migrate the
     * categories.
     *
     * @return the category and the post
     */
    private static DocumentReference[] createFixture(BlogTestData data, String name) throws Exception
    {
        String categoryName = SPACE + name;
        DocumentReference category = new DocumentReference(WIKI, BLOG, categoryName);
        DocumentReference post = new DocumentReference(WIKI, SPACE, name + " post");
        data.createCategory(category, categoryName, "Blog.Categories");
        data.createPost(post, name + " post", BLOG + '.' + categoryName);
        return new DocumentReference[] {post, category};
    }

    /**
     * Deletes the fixture, so that no blog post of the wiki is left in a category of the {@code Blog} space.
     */
    private static void delete(TestUtils setup, BlogTestData data, DocumentReference[] fixture) throws Exception
    {
        for (DocumentReference page : Arrays.asList(fixture)) {
            if (data.exists(page)) {
                setup.rest().delete(page);
            }
        }
    }
}
