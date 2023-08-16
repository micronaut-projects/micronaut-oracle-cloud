package io.micronaut.oraclecloud.serde

import com.oracle.bmc.Region
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

@MicronautTest
class RegionsClientSpec extends SerdeSpecBase {

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void "test region from imds deserialization"() {
        given:
        var server = initContext()

        when:
        // fail once, it should retry
        Region region = Region.getRegionFromImds(server.getURL().toString() + "/opc/v2/");

        then:
        "123" == region.regionId
        "123" == region.regionCode
        "ocxyz" == region.realm.realmId
        "ocxyz.example.com" == region.realm.secondLevelDomain
    }

    @Controller("/opc/v2/instance/regionInfo")
    static class RegionController {

        private static numCalled = 0;

        @Get
        String getRegionInfo(HttpRequest<?> request) {
            if (request.headers.get("Authorization") != "Bearer Oracle") {
                return null
            }

            numCalled++
            if (numCalled < 3) {
                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error")
            }
            return '{"realmKey":"ocxyz",' +
                    '"realmDomainComponent":"ocxyz.example.com",' +
                    '"regionIdentifier": "123",' +
                    '"regionKey": "123"}'
        }
    }

}
