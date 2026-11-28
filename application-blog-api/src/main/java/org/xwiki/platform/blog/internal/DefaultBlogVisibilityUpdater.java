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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
     * The property of the blog post class that lists the users who can view the post while it is hidden.
     */
    private static final String SHARED_USERS = "sharedUsers";

    /**
     * The property of the blog post class that lists the groups whose members can view the post while it is hidden.
     */
    private static final String SHARED_GROUPS = "sharedGroups";

    /**
     * Separates the values of a users or groups list property.
     */
    private static final Pattern LIST_SEPARATOR = Pattern.compile("[,|]");

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
                        restrictVisibility(document, blogPost);
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
     * Tell whether the given rights object is a visibility restriction added by this component. Such a restriction
     * allows only the "view" right, and either lists the creator of the blog post as its first user, or is given to a
     * single user and no group, like the restrictions that older versions added for each user who saved the post
     * (BLOG-172).
     *
     * @param rights the rights object to check, can be {@code null}
     * @param creator the serialized reference of the creator of the blog post, can be {@code null}
     * @return {@code true} if the given rights object is a visibility restriction
     */
    static boolean isVisibilityRestriction(BaseObject rights, String creator)
    {
        return isViewAllowance(rights) && (isCreatorLock(rights, creator) || isSingleUserLock(rights));
    }

    private static boolean isViewAllowance(BaseObject rights)
    {
        return rights != null && rights.getIntValue(RIGHTS_ALLOWDENY) == 1
            && VIEW_RIGHT.equals(rights.getStringValue(RIGHTS_LEVELS));
    }

    private static boolean isCreatorLock(BaseObject rights, String creator)
    {
        List<String> users = parseList(rights.getStringValue(RIGHTS_USERS));
        return !users.isEmpty() && users.get(0).equals(creator);
    }

    private static boolean isSingleUserLock(BaseObject rights)
    {
        return parseList(rights.getStringValue(RIGHTS_USERS)).size() == 1
            && rights.getStringValue(RIGHTS_GROUPS).isEmpty();
    }

    private static List<String> parseList(String value)
    {
        return LIST_SEPARATOR.splitAsStream(value).map(String::trim).filter(item -> !item.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Give the "view" right to the creator of the blog post and to the users and groups the post is shared with, which
     * denies it to everybody else who is not an admin. The restriction does not depend on the current user, so that
     * the other users who save the post do not each add their own restriction (BLOG-172).
     */
    private void restrictVisibility(XWikiDocument document, BaseObject blogPost) throws XWikiException
    {
        String creator = getCreator(document);
        Set<String> users = new LinkedHashSet<>();
        users.add(creator);
        users.addAll(parseList(blogPost.getStringValue(SHARED_USERS)));
        Set<String> groups = new LinkedHashSet<>(parseList(blogPost.getStringValue(SHARED_GROUPS)));

        // Only replace the lock of the creator: single user view rights, which an admin may also have set by hand,
        // stay until the post is published.
        List<BaseObject> restrictions = getRightObjects(document).stream()
            .filter(rights -> isViewAllowance(rights) && isCreatorLock(rights, creator))
            .collect(Collectors.toList());
        if (restrictions.size() == 1 && grantsViewTo(restrictions.get(0), users, groups)) {
            return;
        }
        removeVisibilityRestrictions(document, restrictions);

        XWikiContext context = contextProvider.get();
        BaseObject rightsObject = document.newXObject(RIGHTS_CLASS, context);
        rightsObject.set(RIGHTS_ALLOWDENY, 1, context);
        setList(rightsObject, RIGHTS_USERS, users, context);
        if (!groups.isEmpty()) {
            setList(rightsObject, RIGHTS_GROUPS, groups, context);
        }
        setList(rightsObject, RIGHTS_LEVELS, Collections.singletonList(VIEW_RIGHT), context);
    }

    private boolean grantsViewTo(BaseObject restriction, Set<String> users, Set<String> groups)
    {
        return parseList(restriction.getStringValue(RIGHTS_USERS)).equals(new ArrayList<>(users))
            && parseList(restriction.getStringValue(RIGHTS_GROUPS)).equals(new ArrayList<>(groups));
    }

    private void setList(BaseObject object, String property, Collection<String> values, XWikiContext context)
    {
        PropertyClass propertyClass = (PropertyClass) object.getXClass(context).get(property);
        BaseProperty<?> value = propertyClass.fromStringArray(values.toArray(new String[0]));
        object.set(property, value.getValue(), context);
    }

    /**
     * Remove all the visibility restrictions from the given blog post, whoever they were added for. Older versions
     * added one for each user who saved the post while it was hidden, and publishing removed only the one of the
     * publisher (BLOG-172).
     */
    private void removeVisibilityRestrictions(XWikiDocument document)
    {
        removeVisibilityRestrictions(document, getVisibilityRestrictions(document, getCreator(document)));
    }

    private void removeVisibilityRestrictions(XWikiDocument document, List<BaseObject> restrictions)
    {
        for (BaseObject restriction : restrictions) {
            if (!document.removeXObject(restriction)) {
                logger.error("failed to remove visibility restriction for blog post [{}]",
                    stringSerializer.serialize(document.getDocumentReference()));
            }
        }
    }

    private List<BaseObject> getVisibilityRestrictions(XWikiDocument document, String creator)
    {
        return getRightObjects(document).stream().filter(rights -> isVisibilityRestriction(rights, creator))
            .collect(Collectors.toList());
    }

    private List<BaseObject> getRightObjects(XWikiDocument document)
    {
        List<BaseObject> rightObjects = document.getXObjects(RIGHTS_CLASS);
        return rightObjects != null ? rightObjects : Collections.emptyList();
    }

    private String getCreator(XWikiDocument document)
    {
        DocumentReference creatorReference = document.getCreatorReference();
        return stringSerializer.serialize(
            creatorReference != null ? creatorReference : contextProvider.get().getUserReference());
    }
}
