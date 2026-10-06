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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Updates the locations of the posts and of the categories of a blog that is moved, when they are under the blog. They
 * are stored as text in the blog object, so the move doesn't update them, and the new posts would otherwise be created
 * at the old location of the blog.
 *
 * @version $Id$
 * @since 9.15.13
 */
@Component
@Named(BlogMoveListener.NAME)
@Singleton
public class BlogMoveListener extends AbstractEventListener
{
    protected static final String NAME = "BlogMoveListener";

    private static final LocalDocumentReference BLOG_CLASS = new LocalDocumentReference("Blog", "BlogClass");

    private static final List<String> LOCATION_PROPERTIES = Arrays.asList("postsLocation", "categoriesLocation");

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Default constructor.
     */
    public BlogMoveListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentRenamedEvent renamedEvent = (DocumentRenamedEvent) event;
        String oldLocation = this.localSerializer.serialize(renamedEvent.getSourceReference().getLastSpaceReference());
        String newLocation = this.localSerializer.serialize(renamedEvent.getTargetReference().getLastSpaceReference());
        if (oldLocation.equals(newLocation)) {
            return;
        }

        XWikiContext context = this.contextProvider.get();
        XWiki xwiki = context.getWiki();
        try {
            XWikiDocument blogDoc = xwiki.getDocument(renamedEvent.getTargetReference(), context);
            BaseObject blogObj = blogDoc.getXObject(BLOG_CLASS);
            if (blogObj != null && updateLocations(blogObj, oldLocation, newLocation)) {
                blogDoc.setMetaDataDirty(false);
                blogDoc.setContentDirty(false);
                xwiki.saveDocument(blogDoc, context);
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to update the locations of the moved blog [{}]",
                renamedEvent.getTargetReference(), e);
        }
    }

    private boolean updateLocations(BaseObject blogObj, String oldLocation, String newLocation)
    {
        boolean updated = false;
        for (String property : LOCATION_PROPERTIES) {
            String location = blogObj.getStringValue(property);
            if (location.equals(oldLocation) || location.startsWith(oldLocation + '.')) {
                blogObj.setStringValue(property, newLocation + location.substring(oldLocation.length()));
                updated = true;
            }
        }
        return updated;
    }
}
