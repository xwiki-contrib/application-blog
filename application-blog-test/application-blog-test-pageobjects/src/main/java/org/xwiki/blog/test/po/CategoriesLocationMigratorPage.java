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
package org.xwiki.blog.test.po;

import java.util.Arrays;

import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The {@code Blog.Code.CategoriesLocationMigrator} page, which moves the categories of the {@code Blog} space to the
 * {@code Blog.Categories} space and updates the blog posts in them.
 *
 * @version $Id$
 * @since 9.15.16
 */
public class CategoriesLocationMigratorPage extends ViewPage
{
    private static final DocumentReference REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("Blog", "Code"), "CategoriesLocationMigrator");

    private static final By START_BUTTON = By.cssSelector("#xwikicontent a.btn-primary");

    /**
     * Opens the migration page, which lists the categories to migrate and asks for a confirmation.
     *
     * @return the migration page
     */
    public static CategoriesLocationMigratorPage gotoPage()
    {
        return gotoPage("");
    }

    /**
     * Opens the migration page with the given query string.
     *
     * @param queryString the query string
     * @return the migration page
     */
    public static CategoriesLocationMigratorPage gotoPage(String queryString)
    {
        getUtil().gotoPage(REFERENCE, "view", queryString);
        return new CategoriesLocationMigratorPage();
    }

    /**
     * @return {@code true} if the button that starts the migration is displayed
     */
    public boolean canStart()
    {
        return getDriver().hasElementWithoutWaiting(START_BUTTON);
    }

    /**
     * Starts the migration and waits for the page to be reloaded. The migration is run by a job: it may still be
     * running when this method returns.
     *
     * @return the migration page displaying the progress of the migration
     */
    public CategoriesLocationMigratorPage start()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(START_BUTTON).click();
        getDriver().waitUntilPageIsReloaded();
        return new CategoriesLocationMigratorPage();
    }
}
