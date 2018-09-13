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
package org.xwiki.platform.blog.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.blog.BlogManager;
import org.xwiki.query.QueryException;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * Script service for managing Blogs.
 * <p>
 * A wrapper for the {@link BlogManager} component.
 * 
 * @version $Id$
 * @since 10.0
 */
@Component
@Named("blog")
@Singleton
public class BlogScriptService implements ScriptService
{
    @Inject
    private BlogManager blogManager;

    /** Get the corresponding Blog document for a given document reference.
     * 
     * @param documentReference the current document reference.
     * @return the corresponding Blog document for the document Reference
     * @throws XWikiException in case of XWiki exceptions
     * @throws QueryException in case of Query exceptions
     */
    public Document getBlogDocument(DocumentReference documentReference) throws XWikiException, QueryException
    {
        return blogManager.getBlogDocument(documentReference);
    }
}
