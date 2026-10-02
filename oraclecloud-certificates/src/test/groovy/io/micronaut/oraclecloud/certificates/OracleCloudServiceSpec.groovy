package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey
import com.oracle.bmc.certificates.model.Validity
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.oraclecloud.certificates.events.CertificateEvent
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateFetcher
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateService
import spock.lang.Specification

import java.security.cert.CertificateException

class OracleCloudServiceSpec extends Specification {

    public static final String PRIVATE_KEY = """-----BEGIN ENCRYPTED PRIVATE KEY-----
MIIJtTBfBgkqhkiG9w0BBQ0wUjAxBgkqhkiG9w0BBQwwJAQQY9B7VqxPyjDLjw5n
BAU9LwICCAAwDAYIKoZIhvcNAgkFADAdBglghkgBZQMEASoEEC3ATtqJjFnLbh63
xUVoX60EgglQgi7UO7AvOreTp/4WgG3QkqJLKn9saPQsGwo6Y6P/HzCUOHsPZkn/
7bVEYF7OjrVOHy2mDuzj72FAK+7s6I1k6qT1F0jGWl291wZC0puN4ejIep3YnCKh
dLUdBZlsaq4EY+MoFdrXoQuWUGaKf5mvRZDBHmY8yqfFYFayiE5QD3mfxlN181Q+
ZTCXytOp+gLqipdamLaN4tE4d88XdF0yJThFOu+ca5Fn/xygfH4bXOgPhrjeEv1P
PUjGwB52zSO5+IpOOe0sDKFiSTafswWpYqpe0E7/5+7oqNO/pRHjACgealuwe5IU
m5Epn9zLdkaSyl7YvRKM8Oj7fvZZUZt7ayLAh/fPRjMkYvmRFuyCUy7UfhPS0lBF
BULrD2SjQpD4jsiOOzrGHO9vwN43QmJKx4ROj0ORGE73LJwI6dNgM4bKJyCgNWeY
qr/jjlaNTNM60vAFntEBYBZu8+ZOmLo+oEhPdZJbGATj+j34ezO4Zn4WLwj+tAbC
U9eJsilDch3TpJqHea3hXrvtLqya/4jySEBD0vu7zI44gVC/yCTUtsDhnOeFdFYs
DIQ23Jtl31ceI8U/Re2rcPW2YkywpO7h82rjVnABR1PixB4cZkZPbRHxK7T1b+0O
0dP93LTOMsiTO5ZaQPU9on1DneO7sIog3Qz06a64U6dLPcm4KxC7Q7QvBfQTkthP
6BOd2mHyGJi+IIvQqv/pcLGBPVIC1MSdx27Sg7av+zmz+wX7SfXs5HPFrljnjhDO
bA5cEiSrMRLf+5RapvhjfXm48u3ygp8jU4+0NIYLNb+iGCiym58S79O5X83TwG27
C4k0kFfQ/QwJAyXFgo3JPG14ONN2VwiPg3JrKeLLnMirTTTe3qKMQSfrb2ETVPU0
ZhmSZB+vvWikNCPA6N1BwFQ7wJIT1ZowSZx+mhquNgQLT6cG+5gK2bNz5JzLExqw
T2+6KgLRbUDxfuWF03u798lOAAeCxGBk0skccmYFvb8nmQdAkKa6CH+uCqJ33Z+q
+1i3DlI3TJSOKeAuOuP/WZYPF/PlUPYNCx0/dkpWDp8Mmt792UaJObs/4H9128e7
QQvfm/3wrlFJlpAhc9HgsZlzlcOaqsMp8TuM68B00LAeVxa1655zLBlUCQBjOw7t
Glpqv3sFHAkRlYIET5vVE3XkNML+oA6q903z2KEua9jXVRbqUlXth799eNG3N20g
TCppB8mVyx45E2/4Q3Xb55xiTDk/J+AOzcfNE9c1gijs4rhPMabuBKOs1uH8azQm
2XCL14BMJLIjBypaq8kGqsgK+ZdsqU9KaqHxgye8RqQtCODNWED0Bj1CIXP9clzR
8pMEPEAzFurNgPCEf30QpHoJXahqj/ck7+TXWF4tcwUU1g1MP0j1PBLmsgA03zqc
OQEEfNuNJdNRpZO5auaVa/LReB05vpfqK16PdD9A9g/UNY/Em/8bQHkuT/ho6Mcv
U3kW7LKa4v1Knep9LdKZWEKm66FFqQdlGWRboBxCCmixfokgTlKZRCUYTkYHrNFu
C0sOgKBWXJKRap+h+CpZwzW7DcTBNRbpF1cXPlbXppZWe0t4nEBp8+6ZBZzQkXbz
OiYAPfcRjG72Vm9wJrOgr+6y4FAC34PrBDy9sihdLS6u+jNqBKCFWC2QpIFa8+on
SXWhqil2iVwJ5KN1+Em+YzO0LTNguGuhZI1EMGlaMN6rdr8ZaIEji/zLXvslwoG4
W0j3c0+sas/IHD1UZhVYVV6SW69EEhOwndGYpXtO9uIOd9DINnTxd+5H35Y7V9od
RgdV1SQVGbbNwTQpPo/7K0GzJgvNx0nR4zJbtVGmwE9jXtq5XTpe/QRn19e7t01i
i3q8y5TuQ+uapOsdk4vDTg4/9GiiWBbSucX0aTjCoLK1Y+sH5YYoyMnqbcXlcngQ
dyCpMl1jSN9KOahc01Nwec9lGi5cC2GwvOrrxeOx88/2ALNrP1ax9Htb3d3phIfe
Ni1F9b4J7p9Frg3nBG1+JivBqspbETaxDLfgus5Q8oPsTkmuAvh+nI+QyhB9AzZr
8qwlem/1yHjBhRUJNwo+oaRjfHxVHsDjvOwkbE6G27WAs4zDkPW1RhP0aRYMKt/B
3Asa1gofFBfB7Pqge6SP19X66TwWCfNXDUjb4WVXoxsomJapWjXRybeH3QeU7XJc
vEMDKyO/GosSiPPKGyIaFYhJOM3hN0R0EgZ0k/ImzXJuabVwBpn5s3cKM1aW9r+n
s3+Q9Aw7D2IantHRGzfv4WZZjNdjVhamJHNwze0qF0GvBkJ1qaT9MXqeyBT7pP9X
ccExc0clptNeBl1vV8f9/NL6kFNTFurCTGhjnjWyhybdSsekCrj3BUjTUuhpMvUq
G0PiN3iv0l8mm6llIZkPLpoZSZwI6NY1b8s5W4dGWo/8W2ynzfUF745ZVsQwPZFg
KO/ORcTd7cktNPC2AHOEc1wJmu4ztK7j6eFUUGbRmL6EpvNZBN5SfgQogWmpg9JK
gJlZSsIvrf/qjUC+tOvME9NfvhGUTKl+AqqW9JWyhuRoFduTKIFSRTuuMx/Lzd6H
1giiIQGyT7/KNQIfIk5VVEYl25J1nfdei46DyNOpUzm3CuPQhE0/+pt8cZTquILT
Z6XJvblT46o0FMAfTzJA4UwbWHzPYiKwLzeEudNMpqwiy/83FFKxK5JAYEdhrsxb
PzDLFxfa+ZjHGhY+FE+F/2tJDhgoKh6bl/thMABWsaA/68ooSYZSQ9pyJZua15M2
TPx2y4j5jzBU6zlxc95g9kFhU7w8C7/b+39kETOZH0MC4c18ytbB//gF+Um6JciC
CiwtJm3qsXcmiylq0r/bo2zQSnfTdy2+iBXItneUJdJTbITw2deDWgqTzMuvUCQj
4dG1xRTgLeI+WQdqlYu6QODYbdHVNPDCU/ccqjWDyQQi9jm1Tb/voUuI9ExsLCAD
p1POy2BqmSf3WdpCNtRiBKIeon3tz5vsidUyeszDSqmFYAlKn9TfrujSLPDGtKjH
NNPCXeWvU6O9KrG3Jhx+W5/QPHN0m7mQ7hJPDz/11kk5IT1VELbtaTxQzxP4Ezow
nSrsRWT0hw6LB0FV0nOjIisaVetR0+vrN1f0gUkG8T6DmuOIECrwSyKQjrvPqqmw
pKZzJMMzw0Ud8uwkwq8KKE+nwJfBjhaPopoqLtXpvFJ8H0LTYXV5f/U=
-----END ENCRYPTED PRIVATE KEY-----"""

    public static final String PRIVATE_KEY_PASSPHRASE = "test"

    public static final String CLIENT_PRIVATE_KEY = """-----BEGIN PRIVATE KEY-----
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
AgcKDp9XFfpfQd0Iv0FzJcVhAILh3r0=
-----END PRIVATE KEY-----"""

    public static final String CERTIFICATE_STRING = """-----BEGIN CERTIFICATE-----
MIIF5DCCA8ygAwIBAgIUUeuV1Z3PgZAwZXv7tzh6PWT4Qs8wDQYJKoZIhvcNAQEL
BQAwdDELMAkGA1UEBhMCUlMxEzARBgNVBAgMClNvbWUtU3RhdGUxDTALBgNVBAoM
BFRFU1QxHTAbBgNVBAMMFG1pY3JvbmF1dC5ndWlkZS54NTA5MSIwIAYJKoZIhvcN
AQkBFhNuMHRsM3NzQG91dGxvb2suY29tMCAXDTI2MTAwMTA4MDUxNVoYDzIxMjYw
OTA3MDgwNTE1WjB9MQswCQYDVQQGEwJSUzETMBEGA1UECAwKU29tZS1TdGF0ZTEh
MB8GA1UECgwYSW50ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMRIwEAYDVQQDDAlsb2Nh
bGhvc3QxIjAgBgkqhkiG9w0BCQEWE24wdGwzc3NAb3V0bG9vay5jb20wggIiMA0G
CSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCxBHpPf6bv4Naa1zNF+cK7EMrEccmF
hi4uFDJY/x24zgUq5Btn6UeKA09qMxnoX2bKTh/ChEsUd44stU03oe2QMY7GM5Kn
w5G5efKTOuaz+r5d9dOs6e/IChYkHiAWMDADAkcqShGQzr5y4F3F6J/6iDYnUAQF
8NPpoMngWIW/GwajNRRYbUBJUsgeKEwjIek/q4NBAzA92cW8I583AkuPv4sGx0jF
y//pc3BzmeV+goyM1V5WvyYXmZCmFbZ7Xam7u1p52EEyjNXeBv9rVRJal9mkT4Kv
QOH7XJn0YstQTve7nm+qlVv/VCWlXPz3q8plfgPoXh1MkZb7Md0P/BkT+TfI3HDE
dNKpjIudiT8p+tt/O79jRQUEno7uo5MqTswueU9SH9V8cdguqND10VjJqFoEOPaV
Dm6qDJ7KeAQYtw2NbEum1w0jmmgTw3oZ2Kz2BTv7c06Ek2cdIGayBe/Rpvfs6lz1
EOXA0Lwk1DEKkj6SbmbGzepHsAFbvyHcFDgGB/5wC++dEoVyURtS4v+h6tvrGN/1
yhzfT0ZbF1r/+JQXxDrD18ja/oLrvA9pWLXDHepGwxuO9dIi+7OJZgw9Ov3IiTDW
hVp0iRftj/zXesDS0LdaYIi/KRCYsg7ROR/MvEWvJZjCMxEpj64L1BRXewBXmKgr
LrtPQWvK72nQbQIDAQABo2MwYTAdBgNVHQ4EFgQU3yFHQuWThRLLao/JyoATDhHB
O+8wCQYDVR0TBAIwADAUBgNVHREEDTALgglsb2NhbGhvc3QwHwYDVR0jBBgwFoAU
JNh7py2bHRydMNpdRYUKhU9uloYwDQYJKoZIhvcNAQELBQADggIBAAWRAMk964rd
/KaJeXaolkmzEmltCXj2zwkQigZcqynQAsTeuohkx5HLnliEidEyjPbqkxKHRnZ1
BwTuQt6/K91DgIqaljMfh6qpYxiMrkCTCBISTTljnF64NhtQoIvZaKETw9+hyraF
JNdpcZlXMzsGbc+yNDYLseBvTwNAOOJI5zVOTHr4WlnJ/GytU/laCv8SAn9FOZLE
uTIh3+4vBMdUfsoCbyPRu9Mh2z66GK+FEIVerb4zlE87ToFOWDlMlaacBGfW2hzg
uLRNKlf/+ZS2W5d6ZzTxNV9Dk5IoDqZi1NU7/p6jUBxkRzL6Ea/HieOAKHGhqO5w
47tyPpstU1TWTzgCvhl5llrS+2C9fYoN5GVyOOKR7arkf07J+xChFpPGr3YxdhWl
fbOxhZZgwHb4D1Hq7DPXpcUCT/5PQCX6rfuPYdfh3GGPybvA0Th8FJi46yWjLXiz
BaWPmXzRoYkqYy4mMXPI7EzooSboQJabhkx4/ZejlaNEcQytOBYLQ2QNHbd9tRLS
hlOVxKLmjovHmco3amRTtKT7WUsJNwL7dbdA48sMe8GXH9nF8RYGBntJRPrz/vv2
2HL0zEnBoEfY/g+FX39WcpOdxR7Pp0r/Nn4zrKxqk1WV3iJlAPLXPTZ5VaeciZcn
NUfxtI1oUn8VR1zrZ8dZJ70ieECEz5sE
-----END CERTIFICATE-----"""

    public static String CLIENT_CERTIFICATE_STRING = """-----BEGIN CERTIFICATE-----
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
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
        exception.message == 'Private key must be in PEM format'
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
