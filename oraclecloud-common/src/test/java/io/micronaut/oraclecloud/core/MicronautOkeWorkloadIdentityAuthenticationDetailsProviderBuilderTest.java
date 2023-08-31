package io.micronaut.oraclecloud.core;

import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.mockito.Mockito.mock;

public class MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilderTest {

    public static String CERTIFICATE_STRING = """
-----BEGIN CERTIFICATE-----
MIIFajCCBFKgAwIBAgIUdqm3iigytoj0A4qjzeqqcyUUiQQwDQYJKoZIhvcNAQEL
BQAwEDEOMAwGA1UEAwwFdGVzdDEwHhcNMjMwNTMwMTcyMDQyWhcNMjMwODE1MDAw
MDAwWjAUMRIwEAYDVQQDDAlNaWNyb25hdXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDaz3fk1bp7xmAY7wRYdu6998ioDEkwemwvr3/8OR5Ij+56tGcO
gSAu7HALtcGHYB91NCO3UlDgnauBZ5Crm7HMFdBLUCxfBoxn4s+oNO5EocPbPqru
n++V5FMcXCmj5FagO+TshSzyBl/QMC1SqnIiQ7VUlO/Zhsr2q4DKiCz1v6eT5WbF
ly8eH36M3//q7sb2qFEvOUaj4NhjG+LdbQll04EGUPhIPeBkuWS714EDjKtUv35+
Ncvd285q6MfPihku3mLQRLD6nHT97mPi0VMnOp1A3VTDnTx7kjXGJsY9oSE2X0PO
xACWnyV5a61HqAa8TpE4h2MBksI9nuRFz7dxAgMBAAGjggK2MIICsjAOBgNVHQ8B
Af8EBAMCBaAwDAYDVR0TAQH/BAIwADAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAgYI
KwYBBQUHAwEwggEzBgNVHQ4EggEqBIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBANrPd+TVunvGYBjvBFh27r33yKgMSTB6bC+vf/w5HkiP7nq0Zw6B
IC7scAu1wYdgH3U0I7dSUOCdq4FnkKubscwV0EtQLF8GjGfiz6g07kShw9s+qu6f
75XkUxxcKaPkVqA75OyFLPIGX9AwLVKqciJDtVSU79mGyvargMqILPW/p5PlZsWX
Lx4ffozf/+ruxvaoUS85RqPg2GMb4t1tCWXTgQZQ+Eg94GS5ZLvXgQOMq1S/fn41
y93bzmrox8+KGS7eYtBEsPqcdP3uY+LRUyc6nUDdVMOdPHuSNcYmxj2hITZfQ87E
AJafJXlrrUeoBrxOkTiHYwGSwj2e5EXPt3ECAwEAATCCATcGA1UdIwSCAS4wggEq
gIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANHglJHO6O56lMIp
DUCff+vMWhukXlnZTq4kS7wpiJdMSTYFzN8kOKLDRujLneAfyrYDW9Tm4xFYBZRX
uRiCq7H7ck+HFR0Xq5fuKzp5j/KaGxMbloSGOWqdcWvHOSMwAr4mmGui4le14nXI
fUYZcZuXwooEHSHOMkVrZUa/6M0lNNoTWvUCj74HAXJNAEu7D+hfcKOQ0E7WbSYR
Yol+Kjw3ij6HTGPIjUpY8emDd4tWkJx3pzuTUIABCAcqz9al/Ok3ac2ZEPQbM83N
4JbWrioj+ic8nkA+xvd8NYmdbfU9fiuUXOeQQMmUeAqlgmE3nC+zfSihLYaLJ61A
skTL7a0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIswvhJX2y0YZiiFJy5b6KPID
SJuD+UZmIiDs61ZL6IK3iNRMnJX0IgTQMi5Ema1svvFCMM6502fMm5ktDSEr/GXY
KvqeJ4VmcgfCAxSE5tDek5/KpKOw1X57jqdoMJ25xLWTneN4YeTJDeTAG56T5/Ci
nzsQ7KiJuJc/zsKCgscZ/bQocbz3Pn9jPZTBktCdqsCvVOqHB0RuaclQxWfKsRN4
52Xov7EGYbm0xfifdg2HLnZFUzOnVAEQNsnKXW095IGFgUYlonuQ+jotRfLEf3hm
IgQuEdz+6WvdabYC1igIWN9od6fnoNI3NSRwuttvnJVWX4FkVnhu1YRdGdNkGg==
-----END CERTIFICATE-----""";

    @Test
    void testThatIfNotInsideKuberentesPodIllegalArgumentExceptionIsThrown() {
        MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder = new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder();
        micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.circuitBreakerConfig(CircuitBreakerConfiguration.builder().build());
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.createFederationClient(mock(SessionKeySupplier.class)));
        Assertions.assertEquals( "Kubernetes service account ca cert doesn't exist.", exception.getMessage());
    }

    @Test
    void testInvalidKubernetesCa() throws IOException {
        File file = File.createTempFile("temp", ".ca");
        MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder = new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder(file.getAbsolutePath());
        micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.circuitBreakerConfig(CircuitBreakerConfiguration.builder().build());
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.createFederationClient(mock(SessionKeySupplier.class)));
        Assertions.assertEquals( "Invalid Kubernetes ca certification. Please contact OKE Foundation team for help.", exception.getMessage());
        file.deleteOnExit();
    }

    @Test
    void testThatIfFileDoesntExist() throws IOException {
        File file = File.createTempFile("temp", ".ca");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(CERTIFICATE_STRING);
        writer.flush();
        MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder = new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder(file.getAbsolutePath());
        micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.circuitBreakerConfig(CircuitBreakerConfiguration.builder().build());
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.createFederationClient(mock(SessionKeySupplier.class)));
        Assertions.assertEquals( "Invalid environment variable KUBERNETES_SERVICE_HOST, please contact OKE Foundation team for help.", exception.getMessage());
        file.deleteOnExit();
    }

}
