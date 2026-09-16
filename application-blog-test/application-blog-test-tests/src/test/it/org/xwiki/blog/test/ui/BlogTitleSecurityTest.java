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

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Verify that the title of a {@code Blog.BlogClass} object is displayed as data and never evaluated as Velocity, so
 * that an unprivileged user cannot execute code through it.
 * <p>
 * Regression test for <a href="https://jira.xwiki.org/browse/BLOG-265">BLOG-265</a>: {@code Blog.BlogSheet} used to
 * {@code #evaluate} the title from its privileged (admin-authored) context, letting an EDIT-only user reach
 * programming-right APIs stored in the attacker-controlled title property.
 *
 * @version $Id$
 * @since 9.15.11
 */
public class BlogTitleSecurityTest extends AbstractTest
{
    private static final String BLOG_CLASS = "Blog.BlogClass";

    private static final String PASSWORD = "password";

    /**
     * A privileged API call. Rendered literally when the title is treated as data; rendered as {@code true} if the
     * title is evaluated with the sheet's programming rights.
     */
    private static final String PROGRAMMING_RIGHTS_PROBE = "$xwiki.hasProgrammingRights()";

    private static final String MARKER = "BLOG265";

    /** The value an attacker stores in the blog title property. */
    private static final String MALICIOUS_TITLE = MARKER + " " + PROGRAMMING_RIGHTS_PROBE;

    @Test
    public void blogTitleIsDisplayedAsDataAndNotEvaluated()
    {
        String space = getTestClassName();
        String page = getTestMethodName();
        String user = space + "_" + page;

        // Clean up any left-over from a previous run.
        getUtil().loginAsSuperAdmin();
        getUtil().deletePage(space, page);
        getUtil().deletePage("XWiki", user);

        // Act as a plain registered user with only EDIT rights (no SCRIPT, PROGRAM or ADMIN right), both in the
        // browser and for the REST calls below, so that the created page and object are authored by that user.
        getUtil().createUserAndLogin(user, PASSWORD);
        UsernamePasswordCredentials previousCredentials =
            getUtil().setDefaultCredentials(new UsernamePasswordCredentials(user, PASSWORD));
        try {
            getUtil().createPage(space, page, "Blog title security check.", "Initial title");
            // Adding a Blog.BlogClass object makes the page render through Blog.BlogSheet (class-sheet binding).
            getUtil().addObject(space, page, BLOG_CLASS,
                "title", MALICIOUS_TITLE,
                "itemsPerPage", "10",
                "displayType", "paginated");
        } finally {
            getUtil().setDefaultCredentials(previousCredentials);
        }

        // View the page as that same unprivileged user and read the rendered document title.
        ViewPage viewPage = getUtil().gotoPage(space, page);
        String displayedTitle = viewPage.getDocumentTitle();

        // The stored Velocity must appear verbatim: it is displayed, not executed.
        Assert.assertTrue(
            "The blog title must be displayed as literal data but was: [" + displayedTitle + "]",
            displayedTitle.contains(PROGRAMMING_RIGHTS_PROBE));
        // And it must never have been evaluated with the sheet's programming rights.
        Assert.assertFalse(
            "The blog title was evaluated as Velocity, leaking programming rights: [" + displayedTitle + "]",
            displayedTitle.contains(MARKER + " true"));
    }
}
