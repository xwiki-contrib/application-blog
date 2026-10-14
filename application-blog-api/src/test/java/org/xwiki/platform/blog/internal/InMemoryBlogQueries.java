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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Answers the queries of the blog listeners and migration from the documents held by a {@link MockitoOldcore}. A
 * query is answered according to what it looks for (blog posts in a category, subcategories of a category, legacy
 * categories, blog posts in a legacy category), not according to its exact statement, so that the tests don't depend
 * on how the queries are written.
 *
 * @version $Id$
 */
final class InMemoryBlogQueries
{
    static final LocalDocumentReference BLOG_POST_CLASS = new LocalDocumentReference("Blog", "BlogPostClass");

    static final LocalDocumentReference CATEGORY_CLASS = new LocalDocumentReference("Blog", "CategoryClass");

    static final String CATEGORY = "category";

    private static final String BLOG_SPACE = "Blog";

    private final MockitoOldcore oldcore;

    InMemoryBlogQueries(MockitoOldcore oldcore)
    {
        this.oldcore = oldcore;
    }

    /**
     * Make the given (mock) query manager answer its queries from the documents of the wiki.
     *
     * @param queryManager the mock query manager
     * @throws QueryException never
     */
    void answer(QueryManager queryManager) throws QueryException
    {
        when(queryManager.createQuery(anyString(), anyString()))
            .thenAnswer(invocation -> createQuery(invocation.getArgument(0)));
    }

    private Query createQuery(String statement) throws QueryException
    {
        Query query = mock(Query.class);
        Map<String, Object> parameters = new HashMap<>();
        AtomicReference<String> wiki = new AtomicReference<>();
        when(query.bindValue(anyString(), any())).thenAnswer(invocation -> {
            parameters.put(invocation.getArgument(0), invocation.getArgument(1));
            return query;
        });
        when(query.setWiki(any())).thenAnswer(invocation -> {
            wiki.set(invocation.getArgument(0));
            return query;
        });
        when(query.addFilter(any())).thenReturn(query);
        when(query.setLimit(anyInt())).thenReturn(query);
        when(query.setOffset(anyInt())).thenReturn(query);
        when(query.execute()).thenAnswer(invocation -> execute(statement, parameters, wiki.get()));
        return query;
    }

    private List<?> execute(String statement, Map<String, Object> parameters, String wiki)
    {
        String wikiId = wiki != null ? wiki : this.oldcore.getXWikiContext().getWikiId();
        boolean aboutBlogPosts = statement.contains("BlogPostClass");
        Object category = parameters.get(CATEGORY);
        List<?> result;
        if (parameters.containsKey("prefixedFullName")) {
            // The children of a renamed document (XWikiDocument#getChildrenReferences()). They don't matter here: the
            // migration re-parents the subcategories itself.
            result = Collections.emptyList();
        } else if (category != null) {
            result = aboutBlogPosts ? getBlogPostsIn(wikiId, Collections.singleton(category))
                : getSubcategoriesOf(wikiId, category);
        } else {
            Set<String> legacyCategories = getLegacyCategories(wikiId).map(XWikiDocument::getFullName)
                .collect(Collectors.toSet());
            result = aboutBlogPosts ? getBlogPostsIn(wikiId, legacyCategories)
                : getLegacyCategories(wikiId).filter(document -> !"CategoryTemplate".equals(
                    document.getDocumentReference().getName())).map(XWikiDocument::getDocumentReference)
                    .collect(Collectors.toList());
        }
        return result;
    }

    private Stream<XWikiDocument> getDocuments(String wikiId, LocalDocumentReference classReference)
    {
        return this.oldcore.getDocuments().values().stream()
            .filter(document -> wikiId.equals(document.getDocumentReference().getWikiReference().getName()))
            .filter(document -> document.getXObject(classReference) != null)
            .sorted(Comparator.comparing(document -> document.getDocumentReference().toString()));
    }

    private List<DocumentReference> getBlogPostsIn(String wikiId, Set<?> categories)
    {
        return getDocuments(wikiId, BLOG_POST_CLASS).filter(document -> {
            BaseObject blogPost = document.getXObject(BLOG_POST_CLASS);
            return ((List<?>) blogPost.getListValue(CATEGORY)).stream().anyMatch(categories::contains);
        }).map(XWikiDocument::getDocumentReference).collect(Collectors.toList());
    }

    private List<DocumentReference> getSubcategoriesOf(String wikiId, Object category)
    {
        return getDocuments(wikiId, CATEGORY_CLASS).filter(document -> category.equals(document.getParent()))
            .map(XWikiDocument::getDocumentReference).collect(Collectors.toList());
    }

    /**
     * @return the categories located directly in the {@code Blog} space
     */
    private Stream<XWikiDocument> getLegacyCategories(String wikiId)
    {
        return getDocuments(wikiId, CATEGORY_CLASS).filter(document -> {
            DocumentReference reference = document.getDocumentReference();
            return reference.getSpaceReferences().size() == 1
                && BLOG_SPACE.equals(reference.getLastSpaceReference().getName());
        });
    }
}
