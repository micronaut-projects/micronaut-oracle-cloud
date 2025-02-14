package io.micronaut.discovery.cloud

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataConfiguration
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.filter.FilterContinuation
import io.micronaut.oraclecloud.core.InstancePrincipalConfiguration
import io.micronaut.oraclecloud.core.TenancyIdProvider
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import spock.lang.Specification

import java.nio.file.Paths

class OracleCloudV2MetadataCertSpec extends Specification {

    void "it can resolve metadata from a v2 endpoint"() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudV2MetadataCertSpec",
                        (OracleCloudMetadataConfiguration.PREFIX + ".v2-enabled"): true,
                ], Environment.ORACLE_CLOUD) as EmbeddedServer
        def url = server.URL.toString()

        EmbeddedServer serverNew = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudV2MetadataCert2Spec",
                        (InstancePrincipalConfiguration.PREFIX + ".enabled") : true,
                        (InstancePrincipalConfiguration.PREFIX + ".metadata-base-url") : "$url/opc/v2/",
                        (OracleCloudMetadataConfiguration.PREFIX + ".url") : "$url/opc/v2/",
                        (OracleCloudMetadataConfiguration.PREFIX + ".v2-enabled"): true,
                ], Environment.ORACLE_CLOUD) as EmbeddedServer


        def ctx = serverNew.applicationContext

        when:
        def provider = ctx.getBean(TenancyIdProvider)

        then:
        provider.tenancyId == "ocid1.tenancy.oc1..aaaaaaaabqhm3dknzpyjaw5rilgeis7mh42u4tu2starkttj2xqmsyyhrawq"

        cleanup:
        ctx.close()
        server.stop()
        serverNew.stop()
    }

    @Controller("/opc/v2")
    @Requires(property = "spec.name", value = "OracleCloudV2MetadataCertSpec")
    static class InstanceMetadataServiceV2Mock {
        private final String instanceMetadata
        private final String instanceNetworkMetadata

        InstanceMetadataServiceV2Mock() {
            String currentPath = Paths.get("").toAbsolutePath().toString()
            instanceMetadata = new File("${currentPath}/src/test/groovy/io/micronaut/discovery/cloud/instanceMetadata.json").text
            instanceNetworkMetadata = new File("${currentPath}/src/test/groovy/io/micronaut/discovery/cloud/instanceNetworkMetadata.json").text
        }

        @Get("/instance")
        String instance() {
            return instanceMetadata
        }

        @Get("/vnics")
        String vnics() {
            return instanceNetworkMetadata
        }

        @Produces(value = MediaType.TEXT_PLAIN)
        @Consumes(value = MediaType.TEXT_PLAIN)
        @Get("/instance/region")
        String region() {
            return "phx"
        }

        @Produces(value = "application/x-x509-ca-cert")
        @Consumes(value = MediaType.TEXT_PLAIN)
        @Get("/identity/cert.pem")
        byte[] cert() {
            return """
-----BEGIN CERTIFICATE-----
MIIIwDCCBqigAwIBAgIQbX9QrIT9Dc8ne7nvwF2H0zANBgkqhkiG9w0BAQsFADCB
nzFzMHEGA1UECxNqb3BjLWRldmljZTo0ZjplZDphYzo2ZjpiNjo4MjoyMzphODpk
Mzo2ZTo5MDpmODo0Yzo0NDoxOTpjNjo4ODpmNTo3ZToxNTphNDowOTozZjoxMDo3
Nzo0NzpiNTpiYjoxODplOTowZjo4NDEoMCYGA1UEAxMfUEtJU1ZDIElkZW50aXR5
IEludGVybWVkaWF0ZSByMjAeFw0yNTAxMzAxNTU4MTlaFw0yNTAxMzAxNzU5MTla
MIIBvDFcMFoGA1UEAxNTb2NpZDEuaW5zdGFuY2Uub2MxLnBoeC5hbnlocWxqdHJw
NGd0cHljZmx4cmFncG1namE2aDNnNnVtY2J5YTYzbGZyaDM3bmxscnk3dWZ3ZWV2
NWExHjAcBgNVBAsTFW9wYy1jZXJ0dHlwZTppbnN0YW5jZTFsMGoGA1UECxNjb3Bj
LWNvbXBhcnRtZW50Om9jaWQxLmNvbXBhcnRtZW50Lm9jMS4uYWFhYWFhYWEzYWVt
cDR0bWN0b29hZ3dmZzMyNmlzeXd2Z21tc2tsZW5wZTduaWt5NjZ1c3d6ajdsYWFh
MWkwZwYDVQQLE2BvcGMtaW5zdGFuY2U6b2NpZDEuaW5zdGFuY2Uub2MxLnBoeC5h
bnlocWxqdHJwNGd0cHljZmx4cmFncG1namE2aDNnNnVtY2J5YTYzbGZyaDM3bmxs
cnk3dWZ3ZWV2NWExYzBhBgNVBAsTWm9wYy10ZW5hbnQ6b2NpZDEudGVuYW5jeS5v
YzEuLmFhYWFhYWFhYnFobTNka256cHlqYXc1cmlsZ2VpczdtaDQydTR0dTJzdGFy
a3R0ajJ4cW1zeXlocmF3cTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB
AKB7FCKWhkcMD/WqIMUST8uylY2cwIsmmBibsVHW56X7DgPQl9Je9EvMjh6iAFaW
88eVYybnT0DrdgXxMJC4/TQ3X+tubNpvY4mJxUdQQgUb64vx/O2H0ybC4N7IAteS
TmypBciselxlfoQqewQ1KGmRkTccqk/sLJ22rWIDqgEpIJfP6rSj8VCKeSExSwAa
tHFBraBH0pd5osHLIfh72kdsHcR2JJWZn8QpSwqyz3BBlnymA3ZNnLiLHkuDaido
o7H/rb8X5ha/TQ00SyEvXkslY2V00mqW3Gk87yvvMHfmpjsazwXUarSwS/Whj7Nu
uT7aaFgN1ftjBD8KJ150rGcCAwEAAaOCAtYwggLSMBMGA1UdJQQMMAoGCCsGAQUF
BwMCMB8GA1UdIwQYMBaAFKhi/y7nkY1+4cbKcaz6IvONWzaoMIICmAYJKwYBBAFv
YgoBBIICiTCCAoWBCGluc3RhbmNlglNvY2lkMS5pbnN0YW5jZS5vYzEucGh4LmFu
eWhxbGp0cnA0Z3RweWNmbHhyYWdwbWdqYTZoM2c2dW1jYnlhNjNsZnJoMzdubGxy
eTd1ZndlZXY1YYNTb2NpZDEuY29tcGFydG1lbnQub2MxLi5hYWFhYWFhYTNhZW1w
NHRtY3Rvb2Fnd2ZnMzI2aXN5d3ZnbW1za2xlbnBlN25pa3k2NnVzd3pqN2xhYWGE
T29jaWQxLnRlbmFuY3kub2MxLi5hYWFhYWFhYWJxaG0zZGtuenB5amF3NXJpbGdl
aXM3bWg0MnU0dHUyc3Rhcmt0dGoyeHFtc3l5aHJhd3GFggF8QVFFQ0FSK0xDQUFB
QUFBQUFQK1ZrRjFMd3pBWWhTLzhKNEhjcFNWSjE3SDFTcVlJOWNMS09yellYV2pm
am1pYmpEY3BVc2YrdTgzcTE1Z2dRaUR3SFBLY2NLNE81QW5RYVd0SUpoakozVVBm
dG5jSTBGanNObXJuU09heGg4L2tGaHB0b0o2Q1JyVnVUT3FmN0VBa0ZZS0tCUzFR
VlMxRUFTZFUwQ1hsVk53Z0tBLzFhaUFabVkxd1JtVzRSS2d3MENuenJPSk92K2px
MnA1ZXg1WHRPR0dqVXliaGZEalhNUElPVEEzMWwxOGtvV0FOenZaWVFmRnFBUDlk
OHVmSEMvT0xVM0taUmx4RUNkK0lORXNYV2JxTVJaSnVPVGt5b2sremJmZDR1ZVVJ
eThGNU9KdTViUHZkSTlvOW9OZHcyak9rOTg2YVVyOUJibGFERDVoSUp1WnpKcGtN
SlpQdlVqYngzSGhBbzlydjVQZ08rd1g1ZmZZQkFBQT0wDQYJKoZIhvcNAQELBQAD
ggIBAA25N44SvJaafdiFLiKhqfrms7XLXjm2kB3YemxXGNfeWNx75LREGrPU7jGe
H0s3jVK8FBuZRv4RASVezOUUkyETHqo0mFMykcn0w37gngt0Bxc1cz3mWcoalz41
GMCvR1HdWyhVaBl2j9XOxRS1c+Ltt+uslHixMqiPHDDcGlmLuik+Ncs3+d+ebaX9
74l5Isht8ws75NarwN/w3/siembPKaqtjCNGzFdeMTMyGATaHqH5pQnTBx9b+szH
vpfFpD0WaGDoZMYma2YIjWpWyF7p5GdCn70Gg0FycUYXKT8rsg9Nh+7zUO7Bylpq
Tqyp2A8kOg3Rlr1WyLyqpghyTIGjjhmm49FUSLZzkRymmnY0VYy/8QvoZ6j2Jo7m
vArbS+3jc/SInlnzFf7OLPgRHCRN5kFljXpQGRtTamlbwESHbMML3U0IBnl2dQ4G
L4tAz20r+1ylWhpKXgT3SbAgIFgyHxdvZ+AnuX0iz7qJklOGBHNWgdi1hj6C9MO2
HeNHRfLIMBuyNgGB7gGfeFehcKQTYIEF77CfyLPjGijuNKKtwkR0OphbvHe6Kd76
5JgJcjKfB7psId1Ii7jUt78g6hOjPRgHyFFW92mH0Pd9XiruFsNBWd4wIzxGa5Gl
XIgAmexC2G1ggcTxUph4ui2/1PwumUc2fL5XXTfXguekWs6s
-----END CERTIFICATE-----
""".getBytes()
        }

    @Produces(value = "application/x-x509-ca-cert")
    @Consumes(value = MediaType.TEXT_PLAIN)
    @Get("/identity/key.pem")
    byte[] key() {
        return """
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEAoHsUIpaGRwwP9aogxRJPy7KVjZzAiyaYGJuxUdbnpfsOA9CX
0l70S8yOHqIAVpbzx5VjJudPQOt2BfEwkLj9NDdf625s2m9jiYnFR1BCBRvri/H8
7YfTJsLg3sgC15JObKkFyKx6XGV+hCp7BDUoaZGRNxyqT+wsnbatYgOqASkgl8/q
tKPxUIp5ITFLABq0cUGtoEfSl3miwcsh+HvaR2wdxHYklZmfxClLCrLPcEGWfKYD
dk2cuIseS4NqJ2ijsf+tvxfmFr9NDTRLIS9eSyVjZXTSapbcaTzvK+8wd+amOxrP
BdRqtLBL9aGPs265PtpoWA3V+2MEPwonXnSsZwIDAQABAoIBAAwBiT0ZM8yG636+
jpsPxqZ/os6oZoNCjOhyZW6w/D2fram9Zk5XykENeahzCEFyI6TP2U4kyfoaY4cI
R1Dcdhz885EifsRMxw6YHwk5yOam8xVSMlD282YX+EjSf9vu1y0r7AHaXYEiGrf6
kZ979p0HRezwmRFHowLdktUdXw+dDLVKE2n/sB4VfMFFmCN3ROU/Bmz6GzPGksTT
Vg85jmFZ8eJqQlekbCoY39uhGT7aBJSneorVCj6XoY1SHeM4QkfZFR0WGjT6AzRt
zaoWwlSYBpgDzs8JpGmhH4C6HXORzb98VHMRdQw8zWQkiuPqzISWmZGg3DgVrDMD
pGwemvECgYEA1KXVTiUGympRd35gcaaMLloYUbVTrNRxkCwfYMCaBWg+g31esAfu
UZrr0DtSu5lxDLgA7THrcYYjzDBMBEcPFHEeT60sV5GOatDV2mQWUhytwZxqADMD
rGDnQrwBeh1vxNJn0+YeMiN8oKDlMOi7f0SWYKfyER8XHMHha0Twix8CgYEAwTKh
PQ9E5/FCnzopsao0VUvz9l0QOox7Ebp5vaN1+vCZq81tkSvVm54x8Wlaf0vCABmK
PUEC7agz1Mw8S4shdO66/jLANGWJ2FgyrlsFFJHB+TAJk8dLKZifjEgpif0lhCQ+
cfCRglwpTFvJvC09/FibvVMlpSuPtspj6+MOfbkCgYACimStBVY6buDAS7s9QF8m
Yp8twrvYMcWVkmFhl4t/iwpMKeBKvB2FzhMJLtxDL4chPsWMD++fMJoW43DVEEBG
3z8cdpY0CaIo9ovHizCVFJCi4oqFrBZcJeA9dN+TpOxj7puc9X/g23mutZ1nYzBy
SdB+ISOMPtHY3xwhWvaAwwKBgEnBxoTFG41hMgoP7nMBg9E8mfroNJXlo+Z0xp17
lkMjL8fDOZ0+muI6Vt7PVdlbVskq9vfDphaNLJyFDE/a4f7+VS1OuspGKYYKxe6C
mUHtE5zKlh1w7GUI+4BW3GTt3DDClYRyT1rxTGL2d+H7c7qvKWXyJGEWbFtgkR0x
JgshAoGBANFhIJJkcMznockmaa7tXx//82zoi/noOktC30cZY9xl6zlBjidz7PRC
FFiV4Fi1C6zclK4jV+A5ph+Dk4FGjWjQZiQ7vXQZS7eAEGvx8phOT2TFzrIfRFmC
4AXXbL2J2HS2l65LCh7dl3lZ+a+/uQKeFXkRy72qSFCqS6x7lVdX
-----END RSA PRIVATE KEY-----
""".getBytes()
    }

        @Produces(value = "application/x-x509-ca-cert")
        @Consumes(value = MediaType.TEXT_PLAIN)
        @Get("/identity/intermediate.pem")
        byte[] intermediate() {
            return """
-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----
""".getBytes()
        }
}

    @ServerFilter("/opc/v2/**")
    static class InstanceMetadataServiceV2Filter {

        @RequestFilter
        @ExecuteOn(TaskExecutors.BLOCKING)
        HttpResponse<?> checkAuthorization(HttpRequest<?> request, FilterContinuation<MutableHttpResponse<?>> continuation) {
            if (request.headers.authorization.isEmpty() || request.headers.authorization.get() != "Bearer Oracle") {
                HttpResponse.unauthorized()
            } else {
                continuation.proceed()
            }
        }
    }
}
