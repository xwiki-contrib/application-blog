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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.ui.TestUtils;

import static org.xwiki.test.ui.TestUtils.RestTestUtils.property;

/**
 * Creates the blogs, categories and posts the location tests need, through REST, and reads back the data that no user
 * looks at in the UI: where a page was stored, its parent, the stored properties of a blog or a post, the result of the
 * {@code Blog.BlogCode} macros that decide which blog a page belongs to, and what a category feed is about.
 *
 * @version $Id$
 */
class BlogTestData
{
    static final String WIKI = "xwiki";

    static final String BLOG_CLASS = "Blog.BlogClass";

    static final String BLOG_POST_CLASS = "Blog.BlogPostClass";

    static final String CATEGORY_CLASS = "Blog.CategoryClass";

    private static final String CATEGORY = "category";

    private static final String OUTPUT_SYNTAX = "outputSyntax";

    private static final String CATEGORIES = "Categories";

    private static final String SHEET_BINDING_CLASS = "XWiki.DocumentSheetBinding";

    private static final String SHEET = "sheet";

    private static final String CATEGORIES_SHEET = "Blog.CategoriesSheet";

    private static final String DESCRIPTION = "description";

    private static final String DESCRIPTION_PREFIX = "Description of ";

    private static final String PREFERENCES_CLASS = "XWiki.XWikiPreferences";

    private static final String SHOW_RIGHT_PANELS = "showRightPanels";

    private static final String RIGHT_PANELS = "rightPanels";

    /**
     * The panels displayed by {@code Blog.Management} next to the pages of a new blog.
     */
    private static final String BLOG_PANELS = "Blog.RecentPostsPanel,Blog.UnpublishedPanel,Blog.CategoriesPanel,"
        + "Blog.ArchivePanel,Blog.RelatedBlogsPanel,Blog.AllBlogsPanel";

    private static final String PLAIN = "plain";

    /**
     * Displays, for the space given in the request, the result of the macros of {@code Blog.BlogCode} that are kept
     * when the location logic moves to Java: the blog of the space, its posts location, its categories location, and
     * the blog owning the space when it is a categories space.
     */
    private static final String PROBE_CONTENT = "{{include reference=\"Blog.BlogCode\"/}}\n\n"
        + "{{velocity}}\n"
        + "#set ($probeSpace = \"$!request.space\")\n"
        + "#getBlogDocument($probeSpace $probeBlog)\n"
        + "#getBlogPostsLocation($probeSpace $probePosts)\n"
        + "#getBlogCategoriesLocation($probeSpace $probeCategories)\n"
        + "#getBlogDocumentForCategoriesSpace($probeSpace $probeCategoriesBlog)\n"
        + "#set ($probeResult = \"$probeBlog.fullName|$probePosts|$probeCategories|$probeCategoriesBlog.fullName\")\n"
        + "$services.rendering.escape($probeResult, 'xwiki/2.1')\n"
        + "{{/velocity}}";

    private static final DocumentReference PROBE = new DocumentReference(WIKI, "BlogLocationProbe", "WebHome");

    /**
     * Displays the stored parent and blog post categories of the page given in the request.
     */
    private static final String PAGE_PROBE_CONTENT = "{{velocity}}\n"
        + "#set ($probeDoc = $xwiki.getDocument($request.page))\n"
        + "#if ($probeDoc.isNew())\n"
        + "new\n"
        + "#else\n"
        + "#set ($probeCategories = $probeDoc.getObject('Blog.BlogPostClass').getProperty('category').value)\n"
        + "#set ($probeResult = \"$probeDoc.parent|$!stringtool.join($probeCategories, '|')\")\n"
        + "$services.rendering.escape($probeResult, 'xwiki/2.1')\n"
        + "#end\n"
        + "{{/velocity}}";

    private static final DocumentReference PAGE_PROBE = new DocumentReference(WIKI, "BlogPageProbe", "WebHome");

    private static final String NEW_PAGE = "new";

    private static final Pattern FEED_CHANNEL = Pattern.compile("<channel rdf:about=\"[^\"]*/view/([^\"]*)\"");

    private final TestUtils setup;

    BlogTestData(TestUtils setup)
    {
        this.setup = setup;
    }

    /**
     * Creates a blog descriptor, as {@code Blog.Management} does.
     *
     * @param reference the blog page
     * @param title the title of the blog
     * @param postsLocation the stored posts location, empty for the default
     * @param categoriesLocation the stored categories location, empty for the default
     * @param forceCategorySelection whether the post editor asks for a category before saving a post without one
     */
    void createBlog(DocumentReference reference, String title, String postsLocation, String categoriesLocation,
        boolean forceCategorySelection) throws Exception
    {
        if (hasSquareBrackets(reference)) {
            // The REST client of the tests can't build a URL with square brackets: use the browser instead.
            this.setup.createPage(reference, "", title);
            this.setup.addObject(reference, BLOG_CLASS, "title", title, "postsLocation", postsLocation,
                "categoriesLocation", categoriesLocation, "displayType", "paginated", "itemsPerPage", 10,
                "forceCategorySelection", forceCategorySelection ? 1 : 0);
            return;
        }
        createPage(reference, "", title);
        Object blog = this.setup.rest().object(reference, BLOG_CLASS);
        blog.getProperties().add(property("title", title));
        blog.getProperties().add(property("postsLocation", postsLocation));
        blog.getProperties().add(property("categoriesLocation", categoriesLocation));
        blog.getProperties().add(property("displayType", "paginated"));
        blog.getProperties().add(property("itemsPerPage", 10));
        blog.getProperties().add(property("forceCategorySelection", forceCategorySelection ? 1 : 0));
        this.setup.rest().add(blog);
    }

    /**
     * Creates a blog descriptor with the given posts layout, through REST, which stores the layout as it is given.
     *
     * @param reference the blog page
     * @param title the title of the blog
     * @param postsLayout the stored posts layout
     */
    void createBlog(DocumentReference reference, String title, String postsLayout) throws Exception
    {
        createPage(reference, "", title);
        Object blog = this.setup.rest().object(reference, BLOG_CLASS);
        blog.getProperties().add(property("title", title));
        blog.getProperties().add(property("postsLayout", postsLayout));
        blog.getProperties().add(property("displayType", "paginated"));
        blog.getProperties().add(property("itemsPerPage", 10));
        this.setup.rest().add(blog);
    }

    /**
     * Creates a custom blog post layout, i.e. a wiki macro named {@code blogPostLayout<Name>} that displays the
     * reference of the blog post it is given, after {@code Highlighted post: }.
     *
     * @param reference the page holding the wiki macro
     * @param macroId the identifier of the wiki macro
     */
    void createPostLayout(DocumentReference reference, String macroId) throws Exception
    {
        // The macro is a script and is visible in the whole wiki: its author needs script and admin rights.
        UsernamePasswordCredentials credentials = this.setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        try {
            createPage(reference, "", macroId);
            Object macro = this.setup.rest().object(reference, "XWiki.WikiMacroClass");
            macro.getProperties().add(property("id", macroId));
            macro.getProperties().add(property("name", macroId));
            macro.getProperties().add(property("contentType", "No content"));
            macro.getProperties().add(property("visibility", "Current Wiki"));
            macro.getProperties().add(property("code", "{{velocity}}Highlighted post: "
                + "$services.rendering.escape($xcontext.macro.params.reference, 'xwiki/2.1'){{/velocity}}"));
            this.setup.rest().add(macro);
            for (String parameter : Arrays.asList("reference", "params")) {
                Object macroParameter = this.setup.rest().object(reference, "XWiki.WikiMacroParameterClass");
                macroParameter.getProperties().add(property("name", parameter));
                this.setup.rest().add(macroParameter);
            }
        } finally {
            this.setup.setDefaultCredentials(credentials);
        }
    }

    /**
     * Creates the home page of a categories space, displayed with the categories sheet, as {@code Blog.Management}
     * does.
     *
     * @param reference the home page of the categories space
     */
    void createCategoriesHome(DocumentReference reference) throws Exception
    {
        if (hasSquareBrackets(reference)) {
            this.setup.createPage(reference, "", CATEGORIES);
            this.setup.addObject(reference, SHEET_BINDING_CLASS, SHEET, CATEGORIES_SHEET);
            return;
        }
        createPage(reference, "", CATEGORIES);
        Object binding = this.setup.rest().object(reference, SHEET_BINDING_CLASS);
        binding.getProperties().add(property(SHEET, CATEGORIES_SHEET));
        this.setup.rest().add(binding);
    }

    /**
     * @param reference the category page
     * @param name the name of the category
     * @param parent the parent of the category, empty for none
     */
    void createCategory(DocumentReference reference, String name, String parent) throws Exception
    {
        if (hasSquareBrackets(reference)) {
            this.setup.createPage(reference, "", name, "xwiki/2.1", parent);
            this.setup.addObject(reference, CATEGORY_CLASS, "name", name, DESCRIPTION, DESCRIPTION_PREFIX + name);
            return;
        }
        Page page = this.setup.rest().page(reference);
        page.setTitle(name);
        page.setContent("");
        page.setParent(parent);
        this.setup.rest().save(page);
        Object category = this.setup.rest().object(reference, CATEGORY_CLASS);
        category.getProperties().add(property("name", name));
        category.getProperties().add(property(DESCRIPTION, DESCRIPTION_PREFIX + name));
        this.setup.rest().add(category);
    }

    /**
     * Creates a published blog post.
     *
     * @param reference the blog post page
     * @param title the title of the post
     * @param category the full name of the category of the post
     */
    void createPost(DocumentReference reference, String title, String category) throws Exception
    {
        createPage(reference, "", title);
        Object post = this.setup.rest().object(reference, BLOG_POST_CLASS);
        post.getProperties().add(property("title", title));
        post.getProperties().add(property("content", "Content of " + title));
        post.getProperties().add(property(CATEGORY, category));
        post.getProperties().add(property("published", 1));
        post.getProperties().add(property("hidden", 0));
        this.setup.rest().add(post);
    }

    /**
     * Displays the blog panels on the pages of a space, as {@code Blog.Management} does for a new blog.
     *
     * @param space the space
     */
    void enablePanels(SpaceReference space) throws Exception
    {
        DocumentReference preferences = new DocumentReference("WebPreferences", space);
        if (hasSquareBrackets(preferences)) {
            this.setup.createPage(preferences, "", "");
            this.setup.addObject(preferences, PREFERENCES_CLASS, SHOW_RIGHT_PANELS, 1, RIGHT_PANELS, BLOG_PANELS);
            return;
        }
        createPage(preferences, "", "");
        Object object = this.setup.rest().object(preferences, PREFERENCES_CLASS);
        object.getProperties().add(property(SHOW_RIGHT_PANELS, 1));
        object.getProperties().add(property(RIGHT_PANELS, BLOG_PANELS));
        this.setup.rest().add(object);
    }

    /**
     * Configures, in the {@code WebPreferences} page of a space, the blog whose panels are displayed in that space.
     *
     * @param space the space
     * @param blog the blog, as stored by the blog panels configuration
     */
    void setPanelsBlog(SpaceReference space, String blog) throws Exception
    {
        DocumentReference preferences = new DocumentReference("WebPreferences", space);
        createPage(preferences, "", "");
        Object object = this.setup.rest().object(preferences, "Blog.EnablePanelsConfigurationClass");
        object.getProperties().add(property("blog", blog));
        this.setup.rest().add(object);
    }

    /**
     * @return whether the reference contains square brackets, which the REST client of the tests can't put in a URL
     */
    private boolean hasSquareBrackets(DocumentReference reference)
    {
        return this.setup.serializeReference(reference).matches(".*[\\[\\]].*");
    }

    void createPage(DocumentReference reference, String content, String title) throws Exception
    {
        this.setup.rest().savePage(reference, content, title);
    }

    boolean exists(DocumentReference reference) throws Exception
    {
        return !NEW_PAGE.equals(describe(reference));
    }

    /**
     * @param reference a page
     * @return the stored parent of the page
     */
    String getParent(DocumentReference reference) throws Exception
    {
        return StringUtils.substringBefore(describe(reference), "|");
    }

    /**
     * @param reference a page
     * @param className the class of the object
     * @param propertyName the name of the property
     * @return the stored value of the property of the first object of the given class, list values being separated by
     *         {@code |}
     */
    String getProperty(DocumentReference reference, String className, String propertyName) throws Exception
    {
        ObjectReference object = new ObjectReference(className + "[0]", reference);
        return this.setup.rest().<Property>get(new ObjectPropertyReference(propertyName, object)).getValue();
    }

    /**
     * @param reference a blog post
     * @return the stored categories of the post, separated by {@code |}
     */
    String getPostCategory(DocumentReference reference) throws Exception
    {
        return StringUtils.substringAfter(describe(reference), "|");
    }

    /**
     * Pages are described by a script rather than through REST, since the REST client of the tests can't build a URL
     * with square brackets.
     *
     * @return {@code <parent>|<categories of the blog post>}, or {@code new} when the page doesn't exist
     */
    private String describe(DocumentReference reference) throws Exception
    {
        createProbe(PAGE_PROBE, PAGE_PROBE_CONTENT);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(OUTPUT_SYNTAX, PLAIN);
        parameters.put("page", this.setup.serializeReference(reference));
        return getString(PAGE_PROBE, "get", parameters).trim();
    }

    private void createProbe(DocumentReference probe, String content) throws Exception
    {
        // The probes are scripts: their author needs script right, whoever is logged in.
        UsernamePasswordCredentials credentials = this.setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        try {
            if (!this.setup.rest().exists(probe)) {
                createPage(probe, content, probe.getLastSpaceReference().getName());
            }
        } finally {
            this.setup.setDefaultCredentials(credentials);
        }
    }

    /**
     * Resolves the blog of a space with the macros of {@code Blog.BlogCode}.
     *
     * @param space the space, as a string reference
     * @return {@code <blog>|<posts location>|<categories location>|<blog of the space as a categories space>}
     */
    String resolve(String space) throws Exception
    {
        createProbe(PROBE, PROBE_CONTENT);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(OUTPUT_SYNTAX, PLAIN);
        parameters.put("space", space);
        return getString(PROBE, "get", parameters).trim();
    }

    /**
     * @param setup the test utilities
     * @param reference a page
     * @param queryString the query string, possibly empty
     * @return the URL of the page as the wiki displays it in links, i.e. without the {@code WebHome} of a space home
     *         page
     */
    static String getViewURL(TestUtils setup, DocumentReference reference, String queryString)
    {
        String url = queryString.isEmpty() ? setup.getURL(reference) : setup.getURL(reference, "view", queryString);
        return url.replaceFirst("/WebHome(\\?|$)", "/$1");
    }

    /**
     * @param page the page on which the category feed sheet is applied
     * @param category the category of the feed, given in the request, or empty to take it from the page
     * @return the path of the category the feed is about, relative to the view action
     */
    String getFeedCategory(EntityReference page, String category) throws Exception
    {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("xpage", PLAIN);
        parameters.put(SHEET, "Blog.CategoryRss");
        if (!category.isEmpty()) {
            parameters.put(CATEGORY, category);
        }
        String feed = getString(page, "view", parameters);
        Matcher matcher = FEED_CHANNEL.matcher(feed);
        return matcher.find() ? matcher.group(1) : feed;
    }

    /**
     * @param page the page whose feed is displayed
     * @param sheet the feed sheet applied to the page, or empty when the page is the feed itself
     * @param parameters the other query string parameters, as name-value pairs
     * @return the feed, as sent by the wiki
     */
    String getFeed(EntityReference page, String sheet, String... parameters) throws Exception
    {
        Map<String, String> queryString = new LinkedHashMap<>();
        queryString.put("xpage", PLAIN);
        if (!sheet.isEmpty()) {
            queryString.put(SHEET, sheet);
        }
        for (int i = 0; i + 1 < parameters.length; i += 2) {
            queryString.put(parameters[i], parameters[i + 1]);
        }
        return getString(page, "view", queryString);
    }

    /**
     * Gets the content of a page from the test code, as the super administrator, without the browser: the URLs used by
     * the browser don't work from the test code when XWiki runs inside Docker. {@code TestUtils#getString} is not used
     * because, on the XWiki version the tests run on, it doesn't keep all the query string parameters.
     */
    private String getString(EntityReference page, String action, Map<String, String> parameters) throws Exception
    {
        String path = StringUtils.substringBefore(
            StringUtils.removeStart(this.setup.getURL(page, action, null), this.setup.getBaseURL()), "?");
        String restURL = this.setup.rest().getBaseURL();
        StringBuilder url = new StringBuilder(restURL.substring(0, restURL.lastIndexOf("rest"))).append(path);
        char separator = '?';
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            url.append(separator).append(parameter.getKey()).append('=')
                .append(URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8));
            separator = '&';
        }
        String credentials = TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName() + ':'
            + TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString())).header("Authorization",
            "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8))).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unexpected status [" + response.statusCode() + "] for [" + url + "]");
        }
        return response.body();
    }
}
