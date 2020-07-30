package io.micronaut.oci.streaming;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.oci.core.OracleCloudCoreFactory;

import javax.validation.constraints.NotBlank;

/**
 * Configures streaming.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@ConfigurationProperties(OciStreamingConfiguration.PREFIX)
public class OciStreamingConfiguration {
    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".streaming";

    @NotBlank
    private String username;
    @NotBlank
    private String authToken;
    @NotBlank
    private String streamPoolId;

    /**
     * @return The username of the user account that has permission to use the streaming service.
     */
    public String getUsername() {
        if (username == null) {
            throw new ConfigurationException("Streaming account username name not set");
        }
        return username;
    }

    /**
     * Sets the username of the user account that has permission to use the streaming service.
     * @param username The username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return The auth token of the user account that has permission to use the streaming service.
     */
    public String getAuthToken() {
        if (authToken == null) {
            throw new ConfigurationException("Streaming account auth token not set");
        }
        return authToken;
    }

    /**
     * Sets the auth token of the user account that has permission to use the streaming service.
     * @param authToken the auth token
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    /**
     * The OCID of the stream pool to use.
     * @return The OCID
     */
    public String getStreamPoolId() {
        if (streamPoolId == null) {
            throw new ConfigurationException("The stream pool OCID is not set!");
        }
        return streamPoolId;
    }

    /**
     * Sets the OCID of the stream pool to use.
     * @param streamPoolId The stream pool ID
     */
    public void setStreamPoolId(String streamPoolId) {
        this.streamPoolId = streamPoolId;
    }
}
