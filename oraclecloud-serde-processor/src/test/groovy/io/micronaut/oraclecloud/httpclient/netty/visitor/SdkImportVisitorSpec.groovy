package io.micronaut.oraclecloud.httpclient.netty.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.inject.BeanDefinition
import io.micronaut.serde.annotation.SerdeImport

class SdkImportVisitorSpec extends AbstractTypeElementSpec {

    void "test generate OCI SDK factory"() {
        given:
        def definition = buildBeanDefinition("test.ObjectStorageClientFactory", """
package test;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import io.micronaut.oraclecloud.core.sdk.SdkImport;

@SdkImport(ObjectStorageClient.class)
class Test {}
""")

        expect:
        definition != null
        definition.stringValue(SerdeImport, "packageName").get() == 'com.oracle.bmc.objectstorage.model'
        // check introspections are generated
        definition.beanType.classLoader.loadClass('test.$com_oracle_bmc_objectstorage_model_ReencryptObjectDetails$Introspection')
    }

    void "test generate OCI SDK factory for client package model subpackage"() {
        given:
        def context = buildContext(new JavaFiles()
            .add("test.Test", """
package test;

import com.oracle.health.ehrc.organizer.OrganizerClient;
import io.micronaut.oraclecloud.core.sdk.SdkImport;

@SdkImport(OrganizerClient.class)
class Test {}
""")
            .add("com.oracle.health.ehrc.organizer.OrganizerClient", """
package com.oracle.health.ehrc.organizer;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.common.RegionalClientBuilder;
import com.oracle.bmc.http.internal.BaseSyncClient;
import com.oracle.health.ehrc.organizer.client.requests.GetPractitionerRequest;
import com.oracle.health.ehrc.organizer.client.responses.GetPractitionerResponse;

public class OrganizerClient extends BaseSyncClient {
    protected OrganizerClient(Builder builder, AbstractAuthenticationDetailsProvider authDetailsProvider) {
        super(builder, authDetailsProvider, (CircuitBreakerConfiguration) null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public GetPractitionerResponse getPractitioner(GetPractitionerRequest request) {
        return null;
    }

    public static class Builder extends RegionalClientBuilder<Builder, OrganizerClient> {
        public Builder() {
            super(null);
        }

        @Override
        public OrganizerClient build(AbstractAuthenticationDetailsProvider authDetailsProvider) {
            return new OrganizerClient(this, authDetailsProvider);
        }
    }
}
""")
            .add("com.oracle.health.ehrc.organizer.client.requests.GetPractitionerRequest", """
package com.oracle.health.ehrc.organizer.client.requests;

public class GetPractitionerRequest {
}
""")
            .add("com.oracle.health.ehrc.organizer.client.responses.GetPractitionerResponse", """
package com.oracle.health.ehrc.organizer.client.responses;

public class GetPractitionerResponse {
}
""")
            .add("com.oracle.health.ehrc.organizer.client.model.Practitioner", """
package com.oracle.health.ehrc.organizer.client.model;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

public class Practitioner extends ExplicitlySetBmcModel {
    private final String id;

    public Practitioner(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
"""))

        when:
        BeanDefinition definition = context.classLoader.loadClass('test.$OrganizerClientFactory$Definition').newInstance()

        then:
        definition.stringValue(SerdeImport, "packageName").get() == 'com.oracle.health.ehrc.organizer.client.model'
        context.classLoader.loadClass('test.$com_oracle_health_ehrc_organizer_client_model_Practitioner$Introspection')

        cleanup:
        context.close()
    }
}
