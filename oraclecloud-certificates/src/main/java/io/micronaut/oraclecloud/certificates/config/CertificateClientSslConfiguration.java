package io.micronaut.oraclecloud.certificates.config;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.naming.Named;
import io.micronaut.http.ssl.ClientSslConfiguration;

/**
 * Implementation of {@link ClientSslConfiguration} for a specific certificate.
 */
@EachBean(ClientOracleCloudCertificateConfiguration.class)
public class CertificateClientSslConfiguration extends ClientSslConfiguration implements Named {
    private final ClientOracleCloudCertificateConfiguration certificateConfiguration;
    private final String name;

    public CertificateClientSslConfiguration(@Parameter String name, ClientOracleCloudCertificateConfiguration certificateConfiguration) {
        this.name = name;
        this.certificateConfiguration = certificateConfiguration;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * @return The certificate configuration.
     */
    public ClientOracleCloudCertificateConfiguration getCertificateConfiguration() {
        return certificateConfiguration;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }
}
