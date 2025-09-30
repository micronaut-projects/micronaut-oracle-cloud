package io.micronaut.http.ssl

import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey
import com.oracle.bmc.certificates.model.Validity
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration
import io.micronaut.oraclecloud.certificates.events.CertificateEvent
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateFetcher
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateService
import spock.lang.Specification

import java.security.cert.CertificateException

class OracleCloudServiceSpec extends Specification {

    public static final String PRIVATE_KEY = """-----BEGIN ENCRYPTED PRIVATE KEY-----
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
-----END ENCRYPTED PRIVATE KEY-----"""

    public static final String PRIVATE_KEY_PASSPHRASE = "test"

    public static final String CLIENT_PRIVATE_KEY = """-----BEGIN PRIVATE KEY-----
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
AgcKDp9XFfpfQd0Iv0FzJcVhAILh3r0=
-----END PRIVATE KEY-----"""

    public static final String CERTIFICATE_STRING = """-----BEGIN CERTIFICATE-----
MIIF4jCCA8qgAwIBAgIUe9H8t3DShH5O9IDAI7vbHlGiLNAwDQYJKoZIhvcNAQEL
BQAwdDELMAkGA1UEBhMCUlMxEzARBgNVBAgMClNvbWUtU3RhdGUxDTALBgNVBAoM
BFRFU1QxHTAbBgNVBAMMFG1pY3JvbmF1dC5ndWlkZS54NTA5MSIwIAYJKoZIhvcN
AQkBFhNuMHRsM3NzQG91dGxvb2suY29tMB4XDTI1MDkyNjA3MDQyMloXDTI2MDky
NjA3MDQyMlowfTELMAkGA1UEBhMCUlMxEzARBgNVBAgMClNvbWUtU3RhdGUxITAf
BgNVBAoMGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDESMBAGA1UEAwwJbG9jYWxo
b3N0MSIwIAYJKoZIhvcNAQkBFhNuMHRsM3NzQG91dGxvb2suY29tMIICIjANBgkq
hkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAsQR6T3+m7+DWmtczRfnCuxDKxHHJhYYu
LhQyWP8duM4FKuQbZ+lHigNPajMZ6F9myk4fwoRLFHeOLLVNN6HtkDGOxjOSp8OR
uXnykzrms/q+XfXTrOnvyAoWJB4gFjAwAwJHKkoRkM6+cuBdxeif+og2J1AEBfDT
6aDJ4FiFvxsGozUUWG1ASVLIHihMIyHpP6uDQQMwPdnFvCOfNwJLj7+LBsdIxcv/
6XNwc5nlfoKMjNVeVr8mF5mQphW2e12pu7taedhBMozV3gb/a1USWpfZpE+Cr0Dh
+1yZ9GLLUE73u55vqpVb/1QlpVz896vKZX4D6F4dTJGW+zHdD/wZE/k3yNxwxHTS
qYyLnYk/Kfrbfzu/Y0UFBJ6O7qOTKk7MLnlPUh/VfHHYLqjQ9dFYyahaBDj2lQ5u
qgyeyngEGLcNjWxLptcNI5poE8N6Gdis9gU7+3NOhJNnHSBmsgXv0ab37Opc9RDl
wNC8JNQxCpI+km5mxs3qR7ABW78h3BQ4Bgf+cAvvnRKFclEbUuL/oerb6xjf9coc
309GWxda//iUF8Q6w9fI2v6C67wPaVi1wx3qRsMbjvXSIvuziWYMPTr9yIkw1oVa
dIkX7Y/813rA0tC3WmCIvykQmLIO0TkfzLxFryWYwjMRKY+uC9QUV3sAV5ioKy67
T0Fryu9p0G0CAwEAAaNjMGEwHwYDVR0jBBgwFoAUf8ubc5wsgYR5dt9mE4gEa747
EQcwCQYDVR0TBAIwADAUBgNVHREEDTALgglsb2NhbGhvc3QwHQYDVR0OBBYEFN8h
R0Llk4USy2qPycqAEw4RwTvvMA0GCSqGSIb3DQEBCwUAA4ICAQAM0ofi773lBRxn
XKg9ceUDcDV7b145/kVTntwK6mvgn1s4aMDEEz9CO4X98pKwGuvKi+gz8Q4rEc34
FoUPPdEqEn2dFDDb4gC5qWZgL3gJV05D0uzs7ZwJrFeC1G69qffeuIVtVYcfr3Rx
q8cD0A1CavneXDWDFS7A4B0kZ0QGL9T7NCzWy9yqIpZRxJOMCjWG3zErdfMVemcZ
2H4zrX+ikunW77yqdSFr0DI9PjKVf/uqdmYHVyoK30kPa8pDthRPUfiYsHWW++lZ
NRpQ9H3hNcriRchc6IoJ/iPOd9Bdm/fwcKX/W4fUIpsXUCHYv3IQFbNYsFoGEuTX
8FHLjPRA9f/5Lk3x3NdwZFUxdRDongdIu4SNFaHP+e47d87uySDWr5exlZjp4zrt
qGCZoGXWcn01/hq4l3KWyaS5E7fjk0NsAWQVuZf5VRboOLXc4ywuPPX7dpeApbR6
r0OJ//A5IDKU9ES2Sd6PA0vLyB/+JurjD6Nt2b3nO2pxD/xovcUEsLwBf+IEvzk9
D/uqI4d2G6jVtswP7N8HHwAUPmSkyaYxc+B8pltft4mWitxlM9FMX3RLNgbsNUjU
8iKaaKELVWFYA0m1WSO2SsvszATzVty+lqrOs4degBJKc93z+VKqrRJ26FmL0NTh
RalLIP2O3EVJ90qoeG4KPBt+hcSdaA==
-----END CERTIFICATE-----"""

    public static String CLIENT_CERTIFICATE_STRING = """-----BEGIN CERTIFICATE-----
MIIFvzCCA6egAwIBAgIUe9H8t3DShH5O9IDAI7vbHlGiLNEwDQYJKoZIhvcNAQEL
BQAwdDELMAkGA1UEBhMCUlMxEzARBgNVBAgMClNvbWUtU3RhdGUxDTALBgNVBAoM
BFRFU1QxHTAbBgNVBAMMFG1pY3JvbmF1dC5ndWlkZS54NTA5MSIwIAYJKoZIhvcN
AQkBFhNuMHRsM3NzQG91dGxvb2suY29tMB4XDTI1MDkyNjA3MDcxNloXDTI2MDky
NjA3MDcxNlowezELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAf
BgNVBAoMGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDEQMA4GA1UEAwwHbjB0bDNz
czEiMCAGCSqGSIb3DQEJARYTbjB0bDNzc0BvdXRsb29rLmNvbTCCAiIwDQYJKoZI
hvcNAQEBBQADggIPADCCAgoCggIBAOIrI6P9FS3arVOzBJFATnbXONlG/EiFz5hP
JPskeQN+RC1W1evbIh7XoeNUJUgZv4dTpPyqhDxgiMSnbZYycF8kKIFJNYoLLCZ2
bLOrZjD/xdzQ1Knk6kdMMPUUWms+xSU8uVKY4ysiXKqKUbQxH7C9bID/P53NHDf5
aC8u3hKAi+2YjCsQEJ57VwJEbRBY/geGFdOEF0N+bIo+ZNslKUnNrBk5Gk4kCxMh
J24CqVpQWxTEbQlQdvCbNSrQ3TQ3TenqE6QEDZdrFUgO5gpx6a1grafqYSjbEhHV
dtlObhd23We2/DFkpivn22Qsx5CWHysT0UVeN7Y2AbRE4GGXxn+mt2NUyl14UFbd
PF6mnCMGTV81NzuGw98RXtm6MaxIkdkUTz91Yxzuz9hszWBzdsipTHZswNPY2SDW
k2hNxCzcksrNLOu7lrt/9Br1sN5LKxwa8JfvY6Go+HnpPM3Xk7xcSv6VCy8TKFGp
RqICOGV6OoxUs+5gmrSKsKpDU3R97B4dhob+3zBAjG9mTb9F+opVYEp4a4PvvxPR
JryZNLjNplmVKEpUBf+Q7SEIi2rpkVDQ/xbJrWn/EK066olmgSAjUSkI0L9Q4AQy
7CFd+GHlXRV0V93n6fFd3dvjH5IdWT0ICKAdA/nxvkRcA4m/UUBxg7mxCVG4rC7k
IeWp9DL3AgMBAAGjQjBAMB0GA1UdDgQWBBQgTv4Ix6/hbaMLOD71Fi9QerrCqjAf
BgNVHSMEGDAWgBR/y5tznCyBhHl232YTiARrvjsRBzANBgkqhkiG9w0BAQsFAAOC
AgEAEtzlVd7m6S+EcMZBBzMoutuUvvppTH1nMHPnG3r8nkHYHpi0XZh7UHUVNdGS
RuWd6/Py2fv8f2D8/KvDpEnIp8TepCtx3qh9Q0tLz8mXHJWZHBQUDSTjCrsXcvji
bgZC1Rnf9aujdlo4qe753m1HmsdRM6lW7xa4AvOkvch4c50sxEJmI0kDOGr3qi7+
bFWBmJp0gsNSPtG0c5W/8jA8tZF+0kSwVmGazSL0EzM+X2tBVsQBcjqhqEFNXErA
gr3EfDHrFyMaqxPMbtvzSPvHg/x4ZAAR3zB/chFFZwwgjSKPmAwFFy9jZMm86c6b
39GaBBkSS1m+07sDXiy9y1BHco/zHpWIEEOaNlgzqeq3s/5lB6DEW6jVyg29/aDr
2Q52pzp7/h740oX4nGjRvrJUXGcOiq+7wZrdJ8Xb8roMuc3t3P8UH10C01CJZDrQ
DFdhuYFqz/rrNgEC4UcKDzO1TmX9gmP2wbYL/qGotvUz7SsTFGXw4ofsG6bO0qgS
ZSHfKQ/VI5N1Wy8MvUooY2Wp5/DR7jWzMDphFjN8HhTfXDEXgshk3lsekJP9RWtJ
l2B2nwVmFR42amR0k2G51GnWFChRG3mj1UMy4EXX4w7rX5rQxiqfkYPND44FDbRY
w5kFJv4b0I0vpGeLwz64g3nm4Hr5BuzqGGFKHQDnY6jarro=
-----END CERTIFICATE-----"""

    public static String CERTIFICATE_CHAIN_STRING = """-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----"""

    def "refresh certificate with chain"() {
        CertificateEvent firedEvent

        given:
        def oracleCloudCertificationsConfiguration =  new OracleCloudCertificationsConfiguration("testId", 0, "testName", true)
        def mockOracleCloudCertificateFetcher = Mock(OracleCloudCertificateFetcher)
        def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)

        def service = new OracleCloudCertificateService(List.of(oracleCloudCertificationsConfiguration), mockApplicationEventPublisher, mockOracleCloudCertificateFetcher)

        def resp = GetCertificateBundleResponse.builder()
                .certificateBundle(
                        CertificateBundleWithPrivateKey.builder()
                                .privateKeyPem(PRIVATE_KEY)
                                .certificateId("testId")
                                .serialNumber("test")
                                .privateKeyPemPassphrase(PRIVATE_KEY_PASSPHRASE)
                                .timeCreated(new Date())
                                .certChainPem(CERTIFICATE_CHAIN_STRING)
                                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                                .certificatePem(CERTIFICATE_STRING).build())
                .build()

        def event = OracleCloudCertificateFetcher.getEventFromGetCertificateBundleResponse(resp)

        when:
        service.refreshCertificate()

        then:
        1 * mockOracleCloudCertificateFetcher.retrieveCertificate(*_) >> event

        1 * mockApplicationEventPublisher.publishEvent(*_) >> {arguments -> firedEvent=arguments[0]}
        firedEvent != null
        firedEvent.privateKey() != null
        firedEvent.intermediate() != null
        firedEvent.intermediate().size() == 1
        firedEvent.certificate() != null
    }

    def "refresh certificate"() {
        CertificateEvent firedEvent
        given:
        def oracleCloudCertificationsConfiguration =  new OracleCloudCertificationsConfiguration("testId", 0, "testName", true)
        def mockOracleCloudCertificateFetcher = Mock(OracleCloudCertificateFetcher)
        def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)

        def service = new OracleCloudCertificateService(List.of(oracleCloudCertificationsConfiguration), mockApplicationEventPublisher, mockOracleCloudCertificateFetcher)
        def resp = GetCertificateBundleResponse.builder()
                .certificateBundle(
                        CertificateBundleWithPrivateKey.builder()
                                .privateKeyPem(PRIVATE_KEY)
                                .privateKeyPemPassphrase(PRIVATE_KEY_PASSPHRASE)
                                .certificateId("testId")
                                .serialNumber("test")
                                .timeCreated(new Date())
                                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                                .certificatePem(CERTIFICATE_STRING).build())
                .build()

        def event = OracleCloudCertificateFetcher.getEventFromGetCertificateBundleResponse(resp)

        when:
        service.refreshCertificate()

        then:
        1 * mockOracleCloudCertificateFetcher.retrieveCertificate(*_) >> event

        1 * mockApplicationEventPublisher.publishEvent(*_) >> {arguments -> firedEvent=arguments[0]}
        firedEvent != null
        firedEvent.privateKey() != null
        firedEvent.intermediate() != null
        firedEvent.intermediate().size() == 0
        firedEvent.certificate() != null
    }

    def "refresh certificate with invalid private key"() {
        given:
        def resp = GetCertificateBundleResponse.builder()
                .certificateBundle(CertificateBundleWithPrivateKey.builder()
                        .privateKeyPem("Invalid private key")
                        .certificateId("testId")
                        .serialNumber("test")
                        .timeCreated(new Date())
                        .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                        .certificatePem(CERTIFICATE_STRING).build()).build()

        when:
        OracleCloudCertificateFetcher.getEventFromGetCertificateBundleResponse(resp)

        then:
        final RuntimeException exception = thrown()
        exception.message == 'io.micronaut.http.ssl.PemParser$NotPemException: Missing start tag'
    }

    def "refresh certificate with invalid certificate"() {
        given:
        def resp = GetCertificateBundleResponse.builder()
                .certificateBundle(
                        CertificateBundleWithPrivateKey.builder()
                                .privateKeyPem(PRIVATE_KEY)
                                .privateKeyPemPassphrase(PRIVATE_KEY_PASSPHRASE)
                                .certificateId("testId")
                                .serialNumber("test")
                                .timeCreated(new Date())
                                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                                .certificatePem("Invalid Cert").build())
                .build()

        when:
        OracleCloudCertificateFetcher.getEventFromGetCertificateBundleResponse(resp)

        then:
        final CertificateException exception = thrown()
        exception.message == 'Could not parse certificate: java.io.IOException: Empty input'
    }
}
