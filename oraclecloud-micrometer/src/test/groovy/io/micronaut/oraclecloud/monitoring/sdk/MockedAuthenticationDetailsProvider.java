package io.micronaut.oraclecloud.monitoring.sdk;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Singleton
@Primary
@Replaces(ConfigFileAuthenticationDetailsProvider.class)
public class MockedAuthenticationDetailsProvider implements AuthenticationDetailsProvider {

    private static final String dummyPemKey = """
-----BEGIN RSA PRIVATE KEY-----
MIIBOAIBAAJAUczpZlq0T4QOr4F1RAg/lp0CJLn56ldrmis7bDQ1+XiC3/j7DzhP
oLCd2PWHU/jniJdWAw6wESix/nb0xs/EiQIDAQABAkAqmNqyQmnDPrGnE3NNij4S
4JBNL8vFDOEr13eKUWYKEvAAYEnscgyWQvGb7yvAQ5z/YBYatnAjakHRDO5kXtAB
AiEAoQm2tcP3IiBm8BxstWKJlJ3xYA1euqLFdPnAaPQ5L6ECIQCCCYE0CSeLxgWw
YqJyStqFbAzlUO1yarWIL3L61IeL6QIgenKQYxVmzKQmoVx7rFAInOCbsJV5+h/a
VF+zVhqdgQECIEZZ3gzI5xw3hdxngHtVA+QrEM7/eXbtREjpYstRMAQBAiAy3g7Q
ikw16ABtUnL1IVcwxBPZpSowDd5G3bcJyt+NSQ==
-----END RSA PRIVATE KEY-----""";

    @Override
    public String getFingerprint() {
        return "";
    }

    @Override
    public String getTenantId() {
        return "";
    }

    @Override
    public String getUserId() {
        return "";
    }

    @Override
    public String getKeyId() {
        return "";
    }

    @Override
    public InputStream getPrivateKey() {
        return new ByteArrayInputStream(dummyPemKey.getBytes());
    }

    @Override
    public String getPassPhrase() {
        return "";
    }

    @Override
    public char[] getPassphraseCharacters() {
        return new char[0];
    }
}
