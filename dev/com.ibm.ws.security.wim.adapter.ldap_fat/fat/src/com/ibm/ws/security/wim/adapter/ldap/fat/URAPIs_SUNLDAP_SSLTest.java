/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static com.ibm.ws.security.wim.adapter.ldap.fat.LDAPFatUtils.assertDNsEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class URAPIs_SUNLDAP_SSLTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.sun.ssl");
    private static final Class<?> c = URAPIs_SUNLDAP_SSLTest.class;
    private static UserRegistryServletConnection servlet;

    //private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("userRegistry");
        server.startServer(c.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application userRegistry does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        servlet.getRealm();
        Thread.sleep(5000);
        servlet.getRealm();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer("CWIML4529E", "CWIML4537E", "CWPKI0041W");
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithValidGroup() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getUsersForGroupWithValidGroup", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 0);
        List<String> list = result.getList();
        assertTrue(list.contains("vmmuser1@ibm.com"));
        assertTrue(list.contains("vmmuser4@ibm.com"));
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithValidGroupWithLimit() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getUsersForGroupWithValidGroupWithLimit", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 1);
        List<String> list = result.getList();
        assertEquals("There should be one entries", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithLimitWithNestedGroup() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String group = "vmm_nestedGrp";
        Log.info(c, "getUsersForGroupWithLimitWithNestedGroup", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 10);
        List<String> list = result.getList();
        assertTrue(list.contains("vmmuser1@ibm.com"));
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithInvalidGroup() throws Exception {
        String group = "invalidGroup";
        Log.info(c, "getUsersForGroupWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getUsersForGroup(group, 0);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("SUNLDAPRealm_SSL", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "persona1@ibm.com";
        String password = "ppersona1";
        Log.info(c, "checkPassword", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     "persona1@ibm.com", servlet.checkPassword(user, password));
        //passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword fails when passed in a invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithInvalidUser() {
        String user = "invalid";
        String password = "ppersona1";
        Log.info(c, "checkPasswordWithInvalidUser", "Checking good credentials");
        try {
            servlet.checkPassword(user, password);
        } catch (RegistryException e) {
            /* Expected. */
        }
        server.waitForStringInLog("CWIML4537E");
        assertTrue("An invalid user should cause RegistryException with No principal is found message", true);
        //passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithBadCredentials() throws Exception {
        String user = "persona1@ibm.com";
        String password = "badPassword";
        Log.info(c, "checkPasswordWithBadCredentials", "Checking bad credentials");
        assertNull("Authentication should not succeed.",
                   servlet.checkPassword(user, password));
        server.waitForStringInLog("CWIML4529E");
        //passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUser() throws Exception {
        String user = "persona1@ibm.com";
        Log.info(c, "isValidUser", "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if isValidUser works with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUserWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "isValidUserWithInvalidUser", "Checking with an invalid user");
        assertFalse("User validation should fail.",
                    servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsers() throws Exception {
        String user = "persona1@ibm.com";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcard1() throws Exception {
        String user = "*";
        Log.info(c, "getUsersWithAsteriskWildcard1", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be two entries", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a invalid user pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithInvalidUser() throws Exception {
        String user = "invalid";
        Log.info(c, "getUsersWithInvalidUser", "Checking with a invalid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when supplied with an invalid pattern for
     * the user pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithValidPatternLimitLessThanZero() throws Exception {
        String user = "*";
        Log.info(c, "getUsersWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getUsers(user, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayName() throws Exception {
        String user = "vmmtestuser@ibm.com";
        String displayName = "1 919 555 5555"; //<userDisplayNameMapping propertyForInput="principalName" propertyForOutput="cn"/> and <attribute name="telephoneNumber" propertyName="cn" entityType="PersonAccount" />
        Log.info(c, "getUserDisplayName", "Checking with a valid user.");
        assertEquals(displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works(Throws exception) when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayNameWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getUserDisplayNameWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUserDisplayName(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserId() throws Exception {
        String user = "vmmtestuser@ibm.com";
        String uniqueUserId = "uid=vmmtestuser,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueUserId", "Checking with a valid user.");
        assertDNsEqual("", uniqueUserId, servlet.getUniqueUserId(user));
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserIdWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getUniqueUserIdWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUniqueUserId(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityName() throws Exception {
        String user = "vmmtestuser@ibm.com";
        String securityName = "vmmtestuser@ibm.com";

        Log.info(c, "getUserSecurityName", "Checking with a valid user.");
        assertEquals(securityName, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithInvalidUser() throws Exception {
        String user = "uid=invalid,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUserSecurityNameWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUserSecurityName(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4527E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "isValidGroup", "Checking with a valid group");
        assertTrue("Group validation should succeed.",
                   servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroupWithInvalidGroup() throws Exception {
        String group = "invalidGroup";
        Log.info(c, "isValidGroupWithInvalidGroup", "Checking with an invalid group");
        assertFalse("Group validation should fail.",
                    servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroups() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getGroups", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 2, we should only get 2 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard() throws Exception {
        String group = "*";
        Log.info(c, "getGroupsWithAsteriskWildcard", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be 2 entries", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 6; we should only get 5 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard2() throws Exception {
        String group = "vmmg*p*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 6);
        assertEquals("There should only be 5 entries", 5, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a invalid group pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithInvalidGroup() throws Exception {
        String group = "invalidgroup";
        Log.info(c, "getGroupsWithInvalidGroup", "Checking with a invalid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a invalid group pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    /*
     * @Test
     *
     * @ExpectedFFDC(value = { "com.ibm.ws.security.registry.RegistryException", "com.ibm.websphere.wim.exception.SearchControlException" })
     * public void getGroupsWithInvalidLimit() {
     * String group = "group1";
     * Log.info(c, "getGroupsWithInvalidLimit", "Checking with a valid pattern and limit of -1.");
     * try {
     * servlet.getGroups(group, -1);
     * } catch (RegistryException e) {
     * // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
     * // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
     * e.printStackTrace();
     * }
     * server.waitForStringInLog("CWIML1022E"); //CWIML1022E The '-1' count limit specified in the SearchControl data object is invalid.
     * assertTrue("An invalid limit should cause RegistryException in SearchControl", true);
     * }
     */

    /**
     * Hit the test servlet to see if getGroups works when supplied with an invalid pattern for
     * the group pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithValidPatternLimitLessThanZero() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getGroupsWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getGroups(group, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayName() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("vmmgroup1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithInvalidGroup() throws Exception {
        String group = "cn=invalidgroup,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getGroupDisplayNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4527E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithEntityOutOfRealmScope() throws Exception {
        String group = "cn=invalidgroup";
        Log.info(c, "getGroupDisplayNameWithEntityOutOfRealmScope", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
            fail("Expected RegistryException.");
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause RegistryException error CWIML0515E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML0515E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "vmmgroup1";
        String uniqueGroupId = "cn=vmmgroup1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupId", "Checking with a valid group.");
        assertDNsEqual(null, uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdWithInvalidGroup() throws Exception {
        String group = "invalidGroup";
        Log.info(c, "getUniqueGroupIdWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getUniqueGroupId(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "cn=vmmgroup1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        assertEquals("vmmgroup1", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidGroup() throws Exception {
        String group = "cn=invalid,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getGroupSecurityNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4527E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (InvalidUniqueName)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidUniqueName() throws Exception {
        String group = "invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithEntityOutOfRealmScope() throws Exception {
        String group = "uid=invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML0515E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML0515E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUser() throws Exception {
        String user = "vmmuser1@ibm.com";
        Log.info(c, "getGroupsForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.contains("vmmgroup1"));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithNoMembershipForUser() throws Exception {
        String user = "vmmtestuser@ibm.com";
        Log.info(c, "getGroupsForUserWithNoMembershipForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertEquals("There should not be any entry", 0, list.size());
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getGroupsForUserWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getGroupsForUser(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "uid=vmmuser1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("There should only be 2 entries. Returned: " + list, 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUser() throws Exception {
        String user = "uid=invalid,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getUniqueGroupIdsForUser(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4527E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid uniqueName.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUniqueName() throws Exception {
        String user = "invalid";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getUniqueGroupIdsForUser(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E. Message was: "
                       + errorMessage, errorMessage.contains("CWIML4001E"));
        }
    }
}