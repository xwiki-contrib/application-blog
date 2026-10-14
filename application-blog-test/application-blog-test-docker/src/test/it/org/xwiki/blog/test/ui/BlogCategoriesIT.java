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
import org.xwiki.blog.test.po.ManageCategoriesPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Blog categories. Tested features: add, rename, delete.
 *
 * @version $Id$
 */
@UITest
class BlogCategoriesIT
{
    /**
     * We make sure to have spaces and special chars to ensure categories can be named with any char.
     */
    private static final String CATEGORY = "The \"Do\"s & Don'ts";

    private static final String CATEGORY_RENAME = "New \"categor'y\"";

    private static final String CATEGORY_RENAME_2 =
        "A category with [[//wiki// syntax]] # || onclick=\"alert('fail');return false;\"";

    @Test
    @Order(1)
    void categoryAddRenameRemove(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        categoryAdd(CATEGORY);
        categoryRename(CATEGORY, CATEGORY_RENAME);
        categoryRename(CATEGORY_RENAME, CATEGORY_RENAME_2);
        categoryRemove(CATEGORY_RENAME_2);
    }

    /**
     * Adds a new category and checks for success.
     *
     * @param name the name of the category to add
     */
    private void categoryAdd(String name)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        assertFalse(categoriesPage.isCategoryPresent(name));

        categoriesPage.clickAddCategory();
        categoriesPage.addCategory(name);
        assertTrue(categoriesPage.isCategoryPresent(name));
    }

    /**
     * Renames a category and checks for success.
     *
     * @param fromName source name, must exist
     * @param toName target name, must not exist
     */
    private void categoryRename(String fromName, String toName)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        assertTrue(categoriesPage.isCategoryPresent(fromName));
        assertFalse(categoriesPage.isCategoryPresent(toName));

        categoriesPage.renameCategory(fromName, toName);
        assertFalse(categoriesPage.isCategoryPresent(fromName));
        assertTrue(categoriesPage.isCategoryPresent(toName));
    }

    /**
     * Removes a category and checks for success.
     *
     * @param name category name, must exist
     */
    private void categoryRemove(String name)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        assertTrue(categoriesPage.isCategoryPresent(name));

        categoriesPage.deleteCategory(name);
        assertFalse(categoriesPage.isCategoryPresent(name));
    }
}
