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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.platform.blog.BlogVisibilityUpdater;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Default implementation of {@link BlogVisibilityUpdater}.
 *
 * @version $Id$
 *
 * @since 9.0RC1
 * @since 8.4.3
 * @since 7.4.6
 */
@Component
@Singleton
public class DefaultBlogVisibilityUpdater implements BlogVisibilityUpdater
{
    /**
     * The name of the space where the blog application should be installed.
     */
    private static final String BLOG_SPACE = "Blog";

    /**
     * The name of the blog post class page.
     */
    private static final String BLOG_POST_CLASS = "BlogPostClass";

    /**
     * The name of the blog post template page.
     */
    private static final String BLOG_POST_TEMPLATE = "BlogPostTemplate";

    /**
     * The reference to the xwiki rights, relative to the current wiki.
     */
    private static final EntityReference RIGHTS_CLASS = new EntityReference("XWikiRights", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The users property of the rights class.
     */
    private static final String RIGHTS_USERS = "users";

    /**
     * The groups property of the rights class.
     */
    private static final String RIGHTS_GROUPS = "groups";

    /**
     * The levels property of the rights class.
     */
    private static final String RIGHTS_LEVELS = "levels";

    /**
     * The name of the &quot;view&quot; rights level.
     */
    private static final String VIEW_RIGHT = "view";

    /**
     * The 'allow / deny' property of the rights class.
     */
    private static final String RIGHTS_ALLOWDENY = "allow";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceSerializer<String> stringSerializer;

    @Inject
    private Logger logger;

    @Override
    public void synchronizeHiddenMetadata(XWikiDocument document)
    {
        final DocumentReference blogPostClass = new DocumentReference(BLOG_POST_CLASS, new SpaceReference(BLOG_SPACE,
                document.getDocumentReference().getWikiReference()));

        BaseObject blogPost = document.getXObject(blogPostClass);
        if (blogPost != null) {
            // Set the document visibility according to the values of the blog object.
            // The change will be saved after because the event is sent before the actual saving.
            boolean hidden = blogPost.getIntValue("published") == 0 || blogPost.getIntValue("hidden") == 1;
            document.setHidden(hidden);

            final DocumentReference blogPostTemplate = new DocumentReference(BLOG_POST_TEMPLATE,
                new SpaceReference(BLOG_SPACE, document.getDocumentReference().getWikiReference()));
            if (!blogPostTemplate.equals(document.getDocumentReference())) {
                try {
                    if (hidden) {
                        restrictVisibilityToCurrentUser(document);
                    } else {
                        unrestrictVisibilityToCurrentUser(document);
                    }
                } catch (XWikiException e) {
                    logger.warn("could not set/clear user rights on blog post [{}]",
                        document.getDocumentReference(), e);
                }
            }
        }
    }

    /**
     * Give the current user the "view" right, restricting view to this user only.
     * This assumes there are no other "view" rights set yet; if this is the case, this method will
     * not work as expected. leaving this possibility aside for now
     */
    private void restrictVisibilityToCurrentUser(XWikiDocument document) throws XWikiException
    {
        BaseObject existingRightsObject = findVisibilityToCurrentUserRestriction(document);
        if (existingRightsObject != null) {
            return;
        }

        XWikiContext context = contextProvider.get();
        BaseObject rightsObject = document.newXObject(RIGHTS_CLASS, context);
        rightsObject.set(RIGHTS_ALLOWDENY, 1, context);
        PropertyClass usersPropClass = (PropertyClass) rightsObject.getXClass(context).get(RIGHTS_USERS);
        BaseProperty<?> usersProperty = usersPropClass.fromStringArray(new String[] {
            stringSerializer.serialize(context.getUserReference()) });
        rightsObject.set(RIGHTS_USERS, usersProperty.getValue(), context);
        PropertyClass levelsPropClass = (PropertyClass) rightsObject.getXClass(context).get(RIGHTS_LEVELS);
        BaseProperty<?> levelsProperty = levelsPropClass.fromStringArray(new String[] { VIEW_RIGHT });
        rightsObject.set(RIGHTS_LEVELS, levelsProperty.getValue(), context);
    }

    /**
     * Remove the visibility restriction for the given blog post.
     * This assumes the restriction has been set by a previous call
     * to {@link #restrictVisibilityToCurrentUser(XWikiDocument)}
     * and has not been modified in between.
     */
    private void unrestrictVisibilityToCurrentUser(XWikiDocument document)
    {
        for (int tries = 0; tries < 5; tries++) {
            BaseObject rightsObjectToRemove = findVisibilityToCurrentUserRestriction(document);
            if (rightsObjectToRemove != null) {
                boolean result = document.removeXObject(rightsObjectToRemove);
                if (!result) {
                    logger.error("failed to remove visibility restriction for blog post [{}] on publication",
                        stringSerializer.serialize(document.getDocumentReference()));
                }
            } else {
                break;
            }
        }
    }

    private BaseObject findVisibilityToCurrentUserRestriction(XWikiDocument document)
    {
        XWikiContext context = contextProvider.get();
        String userRef = stringSerializer.serialize(context.getUserReference());
        BaseObject rightsObject = null;
        List<BaseObject> rightObjects = document.getXObjects(RIGHTS_CLASS);
        if (rightObjects != null) {
            for (BaseObject rObj : rightObjects) {
                if (rObj != null) {
                    if (rObj.getIntValue(RIGHTS_ALLOWDENY) == 1
                        && userRef.equals(rObj.getStringValue(RIGHTS_USERS))
                        && VIEW_RIGHT.equals(rObj.getStringValue(RIGHTS_LEVELS))
                        && rObj.getStringValue(RIGHTS_GROUPS).isEmpty()
                        ) {
                        rightsObject = rObj;
                        break;
                    }
                }
            }
        }

        return rightsObject;
    }

    // FIXME: only exists for the unit tests :(
    Provider<XWikiContext> getContextProvider()
    {
        return contextProvider;
    }

}
