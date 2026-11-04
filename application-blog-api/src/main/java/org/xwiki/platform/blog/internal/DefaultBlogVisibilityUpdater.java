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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    /**
     * Matches a users property that holds exactly one user, i.e. a non-empty value without list separator.
     */
    private static final Pattern SINGLE_USER = Pattern.compile("[^,|]+");

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
            // Set the document visibility according to the values of the blog object, only if it's a real blog post,
            // not the BlogPostTemplate.
            // The change will be saved after because the event is sent before the actual saving.
            final DocumentReference blogPostTemplate = new DocumentReference(BLOG_POST_TEMPLATE,
                new SpaceReference(BLOG_SPACE, document.getDocumentReference().getWikiReference()));
            if (!blogPostTemplate.equals(document.getDocumentReference())) {
                boolean hidden = blogPost.getIntValue("published") == 0 || blogPost.getIntValue("hidden") == 1;
                document.setHidden(hidden);
                try {
                    if (hidden) {
                        restrictVisibilityToCreator(document);
                    } else {
                        removeVisibilityRestrictions(document);
                    }
                } catch (XWikiException e) {
                    logger.warn("could not set/clear user rights on blog post [{}]",
                        document.getDocumentReference(), e);
                }
            }
        }
    }

    /**
     * Tell whether the given rights object has the shape of a visibility restriction added by this component: it
     * allows only the "view" right, to a single user and no group.
     *
     * @param rights the rights object to check, can be {@code null}
     * @return {@code true} if the given rights object is a visibility restriction
     */
    static boolean isVisibilityRestriction(BaseObject rights)
    {
        if (rights == null || rights.getIntValue(RIGHTS_ALLOWDENY) != 1) {
            return false;
        }
        return VIEW_RIGHT.equals(rights.getStringValue(RIGHTS_LEVELS))
            && rights.getStringValue(RIGHTS_GROUPS).isEmpty()
            && SINGLE_USER.matcher(rights.getStringValue(RIGHTS_USERS)).matches();
    }

    /**
     * Give the creator of the blog post the "view" right, which denies it to everybody else who is not an admin. The
     * restriction belongs to the creator, not to the current user, so that the other users who save the post do not
     * each add their own restriction (BLOG-172).
     */
    private void restrictVisibilityToCreator(XWikiDocument document) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        DocumentReference creatorReference = document.getCreatorReference();
        String creator = stringSerializer.serialize(
            creatorReference != null ? creatorReference : context.getUserReference());
        if (getVisibilityRestrictions(document).stream()
            .anyMatch(rights -> creator.equals(rights.getStringValue(RIGHTS_USERS)))) {
            return;
        }

        BaseObject rightsObject = document.newXObject(RIGHTS_CLASS, context);
        rightsObject.set(RIGHTS_ALLOWDENY, 1, context);
        PropertyClass usersPropClass = (PropertyClass) rightsObject.getXClass(context).get(RIGHTS_USERS);
        BaseProperty<?> usersProperty = usersPropClass.fromStringArray(new String[] { creator });
        rightsObject.set(RIGHTS_USERS, usersProperty.getValue(), context);
        PropertyClass levelsPropClass = (PropertyClass) rightsObject.getXClass(context).get(RIGHTS_LEVELS);
        BaseProperty<?> levelsProperty = levelsPropClass.fromStringArray(new String[] { VIEW_RIGHT });
        rightsObject.set(RIGHTS_LEVELS, levelsProperty.getValue(), context);
    }

    /**
     * Remove all the visibility restrictions from the given blog post, whoever they were added for. Older versions
     * added one for each user who saved the post while it was hidden, and publishing removed only the one of the
     * publisher (BLOG-172).
     */
    private void removeVisibilityRestrictions(XWikiDocument document)
    {
        for (BaseObject restriction : getVisibilityRestrictions(document)) {
            if (!document.removeXObject(restriction)) {
                logger.error("failed to remove visibility restriction for blog post [{}] on publication",
                    stringSerializer.serialize(document.getDocumentReference()));
            }
        }
    }

    private List<BaseObject> getVisibilityRestrictions(XWikiDocument document)
    {
        List<BaseObject> rightObjects = document.getXObjects(RIGHTS_CLASS);
        if (rightObjects == null) {
            return Collections.emptyList();
        }
        return rightObjects.stream().filter(DefaultBlogVisibilityUpdater::isVisibilityRestriction)
            .collect(Collectors.toList());
    }
}
