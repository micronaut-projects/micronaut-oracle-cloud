package io.micronaut.oraclecloud.function.http

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import jakarta.inject.Inject

/**
 * @author Pavol Gressa
 * @since 2.3*
 */
@MicronautTest(environments = ["custom-env"])
class MockFnHttpServerSpec extends Specification {

    @Inject
    @Client("/env")
    RxHttpClient client

    void "test env forwarded"() {
        given:
        def response = client.exchange(HttpRequest.GET("/"), Set<String>).blockingFirst()

        expect:
        response.status == HttpStatus.OK

        ["function","oraclecloud", "custom-env"].stream().allMatch(it -> response.body().contains(it))
    }

}
