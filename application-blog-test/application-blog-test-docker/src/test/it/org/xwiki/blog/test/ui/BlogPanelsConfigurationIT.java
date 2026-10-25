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
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blog.test.po.AllBlogsPanel;
import org.xwiki.blog.test.po.BlogPanelsConfigurationModal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.property;

/**
 * Verify the configuration of the blog panels displayed in a space, saved from the "Blog configuration" modal of the
 * "More actions" menu, by {@code Blog.EnablePanelsConfigurationService}.
 *
 * @version $Id$
 */
@UITest
class BlogPanelsConfigurationIT
{
    private static final String WIKI = "xwiki";

    private static final String PREFIX = "BlogPanelsConfigurationIT";

    private static final String PASSWORD = "password";

    private static final String PREFERENCES_CLASS = "XWiki.XWikiPreferences";

    private static final String RIGHTS_CLASS = "XWiki.XWikiGlobalRights";

    private static final String CONFIGURATION_CLASS = "Blog.EnablePanelsConfigurationClass";

    private static final String RIGHT_PANELS = "rightPanels";

    private static final String WEB_PREFERENCES = "WebPreferences";

    /**
     * The panels displayed in the spaces of the tests before their configuration is saved.
     */
    private static final String INITIAL_PANELS = "Blog.ArchivePanel";

    private static final String ALL_BLOGS_PANEL = "Blog.AllBlogsPanel";

    private static final DocumentReference SERVICE =
        new DocumentReference(WIKI, "Blog", "EnablePanelsConfigurationService");

    @Test
    @Order(1)
    void saveTheConfigurationOfTheBlogPanels(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        SpaceReference space = new SpaceReference(WIKI, PREFIX + "Saved");
        DocumentReference page = createSpace(setup, space);
        DocumentReference preferences = new DocumentReference(WEB_PREFERENCES, space);

        // Only the blog panels.
        setup.gotoPage(page);
        BlogPanelsConfigurationModal.open(new ViewPage()).setPanels(List.of(ALL_BLOGS_PANEL)).setInheritPanels(false)
            .save();
        assertTrue(new AllBlogsPanel().isDisplayed());
        assertEquals(ALL_BLOGS_PANEL, data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS));
        assertEquals(ALL_BLOGS_PANEL, data.getProperty(preferences, CONFIGURATION_CLASS, "panels"));
        assertEquals("0", data.getProperty(preferences, CONFIGURATION_CLASS, "inheritPanels"));

        // The blog panels after the panels of the wiki (whose list depends on the wiki configuration).
        BlogPanelsConfigurationModal.open(new ViewPage()).setInheritPanels(true).setPanelsPosition("after").save();
        assertTrue(new AllBlogsPanel().isDisplayed());
        assertThat(data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS), endsWith(',' + ALL_BLOGS_PANEL));
        assertEquals("1", data.getProperty(preferences, CONFIGURATION_CLASS, "inheritPanels"));
        assertEquals("after", data.getProperty(preferences, CONFIGURATION_CLASS, "panelsPosition"));

        // The blog panels before the panels of the wiki.
        BlogPanelsConfigurationModal.open(new ViewPage()).setPanelsPosition("before").save();
        assertThat(data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS), startsWith(ALL_BLOGS_PANEL + ','));
        assertEquals("before", data.getProperty(preferences, CONFIGURATION_CLASS, "panelsPosition"));
    }

    @Test
    @Order(2)
    void saveRequiresAValidFormTokenAndAPostRequest(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        SpaceReference space = new SpaceReference(WIKI, PREFIX + "Request");
        createSpace(setup, space);
        DocumentReference preferences = new DocumentReference(WEB_PREFERENCES, space);

        // Without form token.
        assertEquals(403, post(setup, TestUtils.SUPER_ADMIN_CREDENTIALS, space, ""));
        assertEquals(INITIAL_PANELS, data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS));

        // With a valid form token, but not with a POST request.
        setup.gotoPage(SERVICE, "get", getParameters(space, setup.getSecretToken()));
        assertEquals(INITIAL_PANELS, data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS));
    }

    @Test
    @Order(3)
    void saveRequiresAdminRightOnTheSpace(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        BlogTestData data = new BlogTestData(setup);
        SpaceReference space = new SpaceReference(WIKI, PREFIX + "Rights");
        DocumentReference page = createSpace(setup, space);
        DocumentReference preferences = new DocumentReference(WEB_PREFERENCES, space);
        DocumentReference blogPreferences = new DocumentReference(WIKI, "Blog", WEB_PREFERENCES);
        String blogAdmin = PREFIX + "BlogAdmin";
        String spaceAdmin = PREFIX + "SpaceAdmin";
        setup.createUser(blogAdmin, PASSWORD, null);
        setup.createUser(spaceAdmin, PASSWORD, null);
        ObjectReference blogAdminRight = grantAdminRight(setup, blogPreferences, blogAdmin);
        ObjectReference spaceAdminRight = grantAdminRight(setup, preferences, spaceAdmin);
        try {
            // An administrator of the blog space who is not an administrator of the space.
            setup.login(blogAdmin, PASSWORD);
            assertEquals(403,
                post(setup, new UsernamePasswordCredentials(blogAdmin, PASSWORD), space, setup.getSecretToken()));
            assertEquals(INITIAL_PANELS, data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS));

            // An administrator of the space who is not an administrator of the blog space.
            setup.login(spaceAdmin, PASSWORD);
            setup.gotoPage(page);
            BlogPanelsConfigurationModal.open(new ViewPage()).setPanels(List.of(ALL_BLOGS_PANEL))
                .setInheritPanels(false).save();
            assertEquals(ALL_BLOGS_PANEL, data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS));

            // The administrator of the space loses the admin right while configuring the panels.
            BlogPanelsConfigurationModal modal =
                BlogPanelsConfigurationModal.open(new ViewPage()).setPanels(List.of(INITIAL_PANELS));
            deleteAsSuperAdmin(setup, spaceAdminRight);
            assertEquals("You are not allowed to configure the blog panels of this space.", modal.saveWithError());
            assertEquals(ALL_BLOGS_PANEL, data.getProperty(preferences, PREFERENCES_CLASS, RIGHT_PANELS));
        } finally {
            deleteAsSuperAdmin(setup, blogAdminRight);
            setup.loginAsSuperAdmin();
        }
    }

    /**
     * Creates a space whose pages display the right panels.
     *
     * @return the home page of the space
     */
    private static DocumentReference createSpace(TestUtils setup, SpaceReference space) throws Exception
    {
        DocumentReference home = new DocumentReference("WebHome", space);
        setup.rest().savePage(home, "", space.getName());
        DocumentReference preferences = new DocumentReference(WEB_PREFERENCES, space);
        setup.rest().savePage(preferences, "", "");
        Object object = setup.rest().object(preferences, PREFERENCES_CLASS);
        object.getProperties().add(property("showRightPanels", 1));
        object.getProperties().add(property(RIGHT_PANELS, INITIAL_PANELS));
        setup.rest().add(object);
        return home;
    }

    /**
     * Grants the admin right to a user, in a {@code WebPreferences} page that has no rights yet.
     *
     * @return the added rights object
     */
    private static ObjectReference grantAdminRight(TestUtils setup, DocumentReference preferences, String user)
        throws Exception
    {
        Object rights = setup.rest().object(preferences, RIGHTS_CLASS);
        rights.getProperties().add(property("users", "XWiki." + user));
        rights.getProperties().add(property("levels", "admin"));
        rights.getProperties().add(property("allow", 1));
        setup.rest().add(rights);
        return new ObjectReference(RIGHTS_CLASS + "[0]", preferences);
    }

    private static void deleteAsSuperAdmin(TestUtils setup, ObjectReference object) throws Exception
    {
        UsernamePasswordCredentials credentials = setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        try {
            setup.rest().delete(object);
        } finally {
            setup.setDefaultCredentials(credentials);
        }
    }

    /**
     * @return the parameters sent by the modal to save the configuration of the blog panels of a space
     */
    private static String getParameters(SpaceReference space, String formToken)
    {
        return Stream.of("outputSyntax=plain", "docSpace=" + space.getName(),
            CONFIGURATION_CLASS + "_0_panels=" + ALL_BLOGS_PANEL, CONFIGURATION_CLASS + "_0_inheritPanels=0",
            "form_token=" + formToken).map(parameter -> StringUtils.substringBefore(parameter, "=") + '='
                + URLEncoder.encode(StringUtils.substringAfter(parameter, "="), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    }

    /**
     * Sends a POST request to save the configuration of the blog panels of a space, from the test code, without the
     * browser: the URLs used by the browser don't work from the test code when XWiki runs inside Docker.
     *
     * @return the status of the response
     */
    private static int post(TestUtils setup, UsernamePasswordCredentials user, SpaceReference space,
        String formToken) throws Exception
    {
        String path = StringUtils.substringBefore(
            StringUtils.removeStart(setup.getURL(SERVICE, "get", null), setup.getBaseURL()), "?");
        String restURL = setup.rest().getBaseURL();
        String url = restURL.substring(0, restURL.lastIndexOf("rest")) + path;
        String credentials = user.getUserName() + ':' + user.getPassword();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(BodyPublishers.ofString(getParameters(space, formToken))).build();
        return HttpClient.newHttpClient().send(request, BodyHandlers.discarding()).statusCode();
    }
}
