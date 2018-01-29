/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

/**
 * This class provides APIs to access Repository Configuration properties.
 */
public class RepositoryConfig {
    private final String id;
    private final String url;
    private final String apiKey;
    private final String user;
    private final String userPwd;

    /**
     * Create a RepositoryConfig object with all properties.
     *
     * @param id Name of repository
     * @param url url of repository
     * @param apiKey apiKey of repository
     * @param user user name of repository
     * @param userPwd user password of repository
     */
    public RepositoryConfig(String id, String url, String apiKey, String user, String userPwd) {
        this.id = id;
        this.url = url;
        this.apiKey = apiKey == null ? "0" : apiKey;
        this.user = user == null ? null : user.trim();
        this.userPwd = userPwd;
    }

    /**
     * Gets name of repository.
     *
     * @return id Name of repository
     */
    public String getId() {
        return id;
    }

    /**
     * Gets url of repository.
     *
     * @return url URL of repository
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets API key of repository.
     *
     * @return apikey API key of repository
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Gets user name of repository.
     *
     * @return user User name of repository
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the user password of repository.
     *
     * @return userPwd the user password
     */
    public String getUserPwd() {
        return userPwd;
    }

    /**
     * Checks if the id is a liberty repository and has a non-null url
     *
     * @return True if is a liberty repository
     */
    public boolean isLibertyRepository() {
        return id.equalsIgnoreCase(RepositoryConfigUtils.WLP_REPO) && url == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "RepositoryConfig(" + id + "," + url + "," + user + "," + (userPwd == null ? "null)" : "******)");
    }
}
