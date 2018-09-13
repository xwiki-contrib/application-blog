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
package org.xwiki.platform.blog.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.platform.blog.BlogManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link BlogManager}.
 * 
 * @version $Id$
 * @since 10.0
 */
@Component
@Singleton
public class DefaultBlogManager implements BlogManager
{
    /**
     * Name of the main Wiki.
     */
    public static final String XWIKI = "xwiki";

    /**
     * Reference of the Blog class document.
     */
    public static final DocumentReference BLOG_CLASS_REFERENCE = new DocumentReference(XWIKI, "Blog", "BlogClass");

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Override
    public Document getBlogDocument(DocumentReference docRef) throws XWikiException, QueryException
    {
        XWikiContext xcontext = contextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // First, assume that the current doc is a blog doc.
        XWikiDocument blogDoc = xwiki.getDocument(docRef, xcontext);

        if (!isBlog(blogDoc)) {
            String statement =
                new String("from doc.object(Blog.BlogClass) blog where doc.fullName <> 'Blog.BlogTemplate'");

            // Check if the current document name represents the 'categoriesLocation' property of a blog.
            String result = getBlogForCategoriesLocation(docRef, statement);

            // Check if the current document name represents the 'postsLocation' property of a blog.
            if (result == null) {
                result = getBlogForPostsLocation(docRef, statement);
            }

            // Check if the current page has a WebPreferences corresponding page that contains a
            // Blog.EnablePanelsConfigurationClass object.
            if (result == null) {
                result = getBlogFromWebPreferences(docRef);
            }

            // Check if the current document has a sibling blog. For example, it could be Blog.ManageCategories.
            if (result == null) {
                result = getSiblingBlog(docRef, statement);
            }

            // Check up in the pages hierarchy for a blog. This will match pages like panels.
            if (result == null) {
                result = getBlogFromHierarchy(docRef, statement);
            }

            // Finally, get the default blog.
            if (result == null) {
                result = "Blog.WebHome";
            }
            blogDoc = xwiki.getDocument(referenceResolver.resolve(result), xcontext);
        }

        return new Document(blogDoc, xcontext);
    }

    private boolean isBlog(XWikiDocument blogDoc)
    {
        return blogDoc.getXObject(BLOG_CLASS_REFERENCE) != null;
    }

    private String getSiblingBlog(DocumentReference docRef, String statement) throws QueryException
    {
        StringBuilder localStatement = new StringBuilder(statement);
        localStatement.append(" and doc.space = :space");
        Query query = this.queryManager.createQuery(localStatement.toString(), Query.XWQL);
        query.bindValue("space", serializer.serialize(docRef.getLastSpaceReference()));
        return getQueryResult(query);
    }

    private String getBlogForCategoriesLocation(DocumentReference docRef, String statement) throws QueryException
    {
        StringBuilder localStatement = new StringBuilder(statement);
        localStatement.append(" and blog.categoriesLocation = :categoriesLocation");
        Query query = this.queryManager.createQuery(localStatement.toString(), Query.XWQL);
        query.bindValue("categoriesLocation", serializer.serialize(docRef.getLastSpaceReference()));
        return getQueryResult(query);
    }

    private String getBlogForPostsLocation(DocumentReference docRef, String statement) throws QueryException
    {
        StringBuilder localStatement = new StringBuilder(statement);
        localStatement.append(" and blog.postsLocation = :postsLocation");
        Query query = this.queryManager.createQuery(localStatement.toString(), Query.XWQL);
        query.bindValue("postsLocation", serializer.serialize(docRef.getLastSpaceReference()));
        return getQueryResult(query);
    }

    private String getBlogFromWebPreferences(DocumentReference docRef) throws XWikiException
    {
        String result = null;
        XWikiContext xcontext = contextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        DocumentReference webPreferencesRef = new DocumentReference("WebPreferences", docRef.getLastSpaceReference());
        XWikiDocument webPreferencesDoc = xwiki.getDocument(webPreferencesRef, xcontext);
        BaseObject webPreferencesObj =
            webPreferencesDoc.getXObject(referenceResolver.resolve("Blog.EnablePanelsConfigurationClass"));
        if (webPreferencesObj != null) {
            result = (String) webPreferencesObj.getListValue("blog").get(0);
        }
        return result;
    }

    private String getBlogFromHierarchy(DocumentReference docRef, String statement)
        throws QueryException, XWikiException
    {
        String returnedResult = null;
        XWikiContext xcontext = contextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        Query query = this.queryManager.createQuery(statement, Query.XWQL);
        List<String> results = query.<String>execute();
        for (String result : results) {
            XWikiDocument blogDoc = xwiki.getDocument(referenceResolver.resolve(result), xcontext);
            if (isBlog(blogDoc)) {
                returnedResult = result;
                break;
            }
        }
        return returnedResult;
    }

    private String getQueryResult(Query query) throws QueryException
    {
        query.setLimit(1);
        query.setOffset(0);
        return query.<String>execute().size() > 0 ? query.<String>execute().get(0) : null;
    }
}
