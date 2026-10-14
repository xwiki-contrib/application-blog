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
package org.xwiki.contrib.blog;

import java.util.Arrays;

import org.mockito.Answers;
import org.xwiki.contrib.blog.script.BlogScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds, in the in-memory wiki of a page test, the blogs whose location rules are pinned by the page tests:
 * <ul>
 * <li>A: the default blog {@code Blog.WebHome}, with no stored posts or categories location;</li>
 * <li>B: a whole-space blog {@code SpaceB.WebHome} storing its own space and {@code SpaceB.Categories}, as the blog
 * management page does;</li>
 * <li>C: a whole-space blog {@code SpaceC.WebHome} whose posts and categories live elsewhere;</li>
 * <li>D: an in-space blog {@code SpaceD.Blog} (not a {@code WebHome}), with no stored location;</li>
 * <li>E: a nested blog whose space name contains a dot, {@code ]]} and {@code ~}, with no stored location.</li>
 * </ul>
 * The XAR pages needed by these blogs (the blog class and the code pages) are loaded by the test itself.
 *
 * @version $Id$
 */
final class BlogLocationFixture
{
    static final String WIKI = "xwiki";

    static final DocumentReference BLOG_CLASS = new DocumentReference(WIKI, "Blog", "BlogClass");

    static final DocumentReference BLOG_POST_CLASS = new DocumentReference(WIKI, "Blog", "BlogPostClass");

    static final DocumentReference CATEGORY_CLASS = new DocumentReference(WIKI, "Blog", "CategoryClass");

    static final DocumentReference ENABLE_PANELS_CLASS =
        new DocumentReference(WIKI, "Blog", "EnablePanelsConfigurationClass");

    static final DocumentReference BLOG_A = new DocumentReference(WIKI, "Blog", "WebHome");

    static final DocumentReference BLOG_B = new DocumentReference(WIKI, "SpaceB", "WebHome");

    static final DocumentReference BLOG_C = new DocumentReference(WIKI, "SpaceC", "WebHome");

    static final String BLOG_C_POSTS_LOCATION = "PostsC";

    static final String BLOG_C_CATEGORIES_LOCATION = "CategoriesC";

    static final DocumentReference BLOG_D = new DocumentReference(WIKI, "SpaceD", "Blog");

    static final DocumentReference BLOG_E =
        new DocumentReference(WIKI, Arrays.asList("Parent", "Child.Space]] [[~"), "WebHome");

    /**
     * The local serialization of the space of blog E, i.e. the value of {@code $doc.space} for a page of that space.
     */
    static final String BLOG_E_SPACE = "Parent.Child\\.Space]] [[~";

    private final XWiki xwiki;

    private final XWikiContext context;

    BlogLocationFixture(XWiki xwiki, XWikiContext context)
    {
        this.xwiki = xwiki;
        this.context = context;
    }

    /**
     * Registers a mock of the blog script service ({@code $services.blog}) so that a test can decide whether the wiki
     * still has blog posts assigned to legacy categories. The mock answers {@code false} by default, as on a wiki
     * whose categories have been migrated to {@code Blog.Categories}.
     *
     * @param componentManager the component manager of the test
     * @return the registered mock
     * @throws Exception in case of errors
     */
    static BlogScriptService registerBlogScriptService(MockitoComponentManager componentManager) throws Exception
    {
        BlogScriptService blogScriptService = mock(BlogScriptService.class);
        componentManager.registerComponent(ScriptService.class, "blog", blogScriptService);
        return blogScriptService;
    }

    /**
     * Registers a query script service ({@code $services.query}) whose queries can be built and executed, and return no
     * result. The location rules that depend on a query result are not covered by the page tests.
     *
     * @param componentManager the component manager of the test
     * @throws Exception in case of errors
     */
    static void registerEmptyQueryService(MockitoComponentManager componentManager) throws Exception
    {
        QueryManagerScriptService queryService = mock(QueryManagerScriptService.class);
        when(queryService.hql(any())).thenAnswer(invocation -> mock(ScriptQuery.class, Answers.RETURNS_SELF));
        when(queryService.xwql(any())).thenAnswer(invocation -> mock(ScriptQuery.class, Answers.RETURNS_SELF));
        when(queryService.createQuery(any(), any()))
            .thenAnswer(invocation -> mock(ScriptQuery.class, Answers.RETURNS_SELF));
        componentManager.registerComponent(ScriptService.class, "query", queryService);
    }

    /**
     * Creates the blogs A to E.
     *
     * @throws Exception in case of errors
     */
    void createBlogs() throws Exception
    {
        createBlog(BLOG_A, "", "");
        createBlog(BLOG_B, "SpaceB", "SpaceB.Categories");
        createBlog(BLOG_C, BLOG_C_POSTS_LOCATION, BLOG_C_CATEGORIES_LOCATION);
        createBlog(BLOG_D, "", "");
        createBlog(BLOG_E, "", "");
    }

    /**
     * @param reference the reference of the blog document
     * @param postsLocation the stored posts location, empty for the default
     * @param categoriesLocation the stored categories location, empty for the default
     * @return the saved blog document
     * @throws Exception in case of errors
     */
    XWikiDocument createBlog(DocumentReference reference, String postsLocation, String categoriesLocation)
        throws Exception
    {
        XWikiDocument blogDocument = newDocument(reference, "");
        BaseObject blogObject = blogDocument.newXObject(BLOG_CLASS, this.context);
        blogObject.set("postsLocation", postsLocation, this.context);
        blogObject.set("categoriesLocation", categoriesLocation, this.context);
        save(blogDocument);
        return blogDocument;
    }

    /**
     * @param reference the reference of the blog post document
     * @return the saved blog post document
     * @throws Exception in case of errors
     */
    XWikiDocument createPost(DocumentReference reference) throws Exception
    {
        XWikiDocument postDocument = newDocument(reference, "");
        BaseObject postObject = postDocument.newXObject(BLOG_POST_CLASS, this.context);
        postObject.set("title", reference.getName(), this.context);
        save(postDocument);
        return postDocument;
    }

    /**
     * @param reference the reference of the category document
     * @return the saved category document
     * @throws Exception in case of errors
     */
    XWikiDocument createCategory(DocumentReference reference) throws Exception
    {
        XWikiDocument categoryDocument = newDocument(reference, "");
        BaseObject categoryObject = categoryDocument.newXObject(CATEGORY_CLASS, this.context);
        categoryObject.set("name", reference.getName(), this.context);
        save(categoryDocument);
        return categoryDocument;
    }

    /**
     * Configures, in the {@code WebPreferences} page of the given space, the blog whose panels are displayed there.
     *
     * @param preferencesReference the reference of the {@code WebPreferences} page
     * @param blogReference the blog to use in that space, as stored by the panels configuration
     * @throws Exception in case of errors
     */
    void configurePanelsBlog(DocumentReference preferencesReference, String blogReference) throws Exception
    {
        XWikiDocument preferencesDocument = newDocument(preferencesReference, "");
        BaseObject configurationObject = preferencesDocument.newXObject(ENABLE_PANELS_CLASS, this.context);
        configurationObject.set("blog", blogReference, this.context);
        save(preferencesDocument);
    }

    /**
     * @param reference the reference of the page
     * @param content the content of the page, in XWiki 2.1 syntax
     * @return a new page, not saved yet
     * @throws Exception in case of errors
     */
    XWikiDocument newDocument(DocumentReference reference, String content) throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(reference, this.context).clone();
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent(content);
        return document;
    }

    /**
     * @param document the page to save
     * @throws Exception in case of errors
     */
    void save(XWikiDocument document) throws Exception
    {
        this.xwiki.saveDocument(document, this.context);
    }

    /**
     * Saves a page with the given content and renders it as the current page.
     *
     * @param reference the reference of the page, whose objects are kept if it exists already
     * @param content the content of the page, in XWiki 2.1 syntax
     * @return the rendered content
     * @throws Exception in case of errors
     */
    String render(DocumentReference reference, String content) throws Exception
    {
        XWikiDocument document = newDocument(reference, content);
        save(document);
        this.context.setDoc(document);
        return document.getRenderedContent(this.context);
    }

    /**
     * Saves a copy of a page under a new reference, keeping its content and objects.
     *
     * @param source the page to copy
     * @param target the reference of the copy
     * @return the saved copy
     * @throws Exception in case of errors
     */
    XWikiDocument copy(XWikiDocument source, DocumentReference target) throws Exception
    {
        XWikiDocument copy = source.duplicate(target);
        save(copy);
        return copy;
    }
}
