package io.micronaut.oraclecloud.certificates.config;

import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;

import java.util.Map;


@ContextConfigurer
@Internal
public class CertificateClientContextConfigurer implements ApplicationContextConfigurer {
    @Override
    public void configure(ApplicationContextBuilder builder) {
        System.setProperty(Environment.BOOTSTRAP_CONTEXT_PROPERTY, StringUtils.TRUE);
        builder.properties(Map.of(
            "oci.clients.certificates.ssl.enabled", "false"
        ));
    }
}
