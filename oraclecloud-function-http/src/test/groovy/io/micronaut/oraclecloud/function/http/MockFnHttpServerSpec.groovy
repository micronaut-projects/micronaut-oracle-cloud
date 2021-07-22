package io.micronaut.oraclecloud.function.http

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Mono
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
    HttpClient client

    void "test env forwarded"() {
        given:
        def response = Mono.from(client.exchange(HttpRequest.GET("/"), Set<String>)).block()

        expect:
        response.status == HttpStatus.OK

        ["function","oraclecloud", "custom-env"].stream().allMatch(it -> response.body().contains(it))
    }

}
