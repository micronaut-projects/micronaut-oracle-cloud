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
}
