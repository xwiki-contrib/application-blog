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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.platform.blog.events.BlogPostPublishedEvent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class BlogPostPublishedEventDescriptorTest
{
    @Rule
    public MockitoComponentMockingRule<BlogPostPublishedEventDescriptor> mocker =
            new MockitoComponentMockingRule<>(BlogPostPublishedEventDescriptor.class);

    private ContextualLocalizationManager contextualLocalizationManager;

    @Before
    public void setUp() throws Exception
    {
        contextualLocalizationManager = mocker.getInstance(ContextualLocalizationManager.class);
        when(contextualLocalizationManager.getTranslationPlain("blog.applicationName")).thenReturn("Blog");
        when(contextualLocalizationManager.getTranslationPlain("blog.events.blogpostpublished.description"))
                .thenReturn("An article has been posted!");
    }

    @Test
    public void getEventType() throws Exception
    {
        assertEquals(BlogPostPublishedEvent.class.getCanonicalName(), mocker.getComponentUnderTest().getEventType());
    }

    @Test
    public void getApplicationName() throws Exception
    {
        assertEquals("Blog", mocker.getComponentUnderTest().getApplicationName());
    }

    @Test
    public void getDescription() throws Exception
    {
        assertEquals("An article has been posted!", mocker.getComponentUnderTest().getDescription());
    }

    @Test
    public void getApplicationIcon() throws Exception
    {
        assertEquals("rss", mocker.getComponentUnderTest().getApplicationIcon());
    }
}
