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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.TestUtils;

/**
 * Creates blog posts directly, without going through the blog post creation UI, so that functional tests can set up
 * the posts they need quickly. The posts are created by the user currently logged in the browser, who becomes their
 * author: an unpublished or hidden post is visible only to that user.
 *
 * @version $Id$
 * @since 9.15.13
 */
public class BlogPostFixture
{
    private static final String BLOG_POST_CLASS = "Blog.BlogPostClass";

    /**
     * The format of the {@code publishDate} property of {@code Blog.BlogPostClass}.
     */
    private static final String PUBLISH_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

    private final TestUtils testUtils;

    private final EntityReference reference;

    private final String title;

    private Date publishDate = new Date();

    private String category = "";

    private boolean published = true;

    private boolean hidden;

    /**
     * @param testUtils the utilities used to create the post
     * @param reference the reference of the blog post page
     * @param title the title of the blog post
     */
    public BlogPostFixture(TestUtils testUtils, EntityReference reference, String title)
    {
        this.testUtils = testUtils;
        this.reference = reference;
        this.title = title;
    }

    /**
     * @param publishDate the publication date of the post, which places it in the blog archive; defaults to now
     * @return this fixture
     */
    public BlogPostFixture withPublishDate(Date publishDate)
    {
        this.publishDate = publishDate;
        return this;
    }

    /**
     * @param category the full name of the category page of the post, e.g. {@code Blog.Categories.News}
     * @return this fixture
     */
    public BlogPostFixture withCategory(String category)
    {
        this.category = category;
        return this;
    }

    /**
     * @param published whether the post is published; defaults to {@code true}
     * @return this fixture
     */
    public BlogPostFixture withPublished(boolean published)
    {
        this.published = published;
        return this;
    }

    /**
     * @param hidden whether the post is hidden from the users other than its author; defaults to {@code false}
     * @return this fixture
     */
    public BlogPostFixture withHidden(boolean hidden)
    {
        this.hidden = hidden;
        return this;
    }

    /**
     * Creates the blog post, replacing any existing page with the same reference.
     */
    public void create()
    {
        this.testUtils.deletePage(this.reference);
        this.testUtils.createPage(this.reference, "", this.title);
        this.testUtils.addObject(this.reference, BLOG_POST_CLASS,
            "title", this.title,
            "content", "Content of " + this.title,
            "publishDate", new SimpleDateFormat(PUBLISH_DATE_FORMAT).format(this.publishDate),
            "category", this.category,
            "published", this.published ? 1 : 0,
            "hidden", this.hidden ? 1 : 0);
    }
}
