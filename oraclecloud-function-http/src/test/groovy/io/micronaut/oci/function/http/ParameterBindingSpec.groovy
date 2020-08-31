package io.micronaut.oci.function.http

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.oci.function.http.test.FnHttpTest
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
class ParameterBindingSpec extends Specification {

    @Inject
    @Client("/")
    RxHttpClient client

    void "test URI parameters"() {

        given:
        def response = client.exchange(HttpRequest.GET("/parameters/uri/Foo"), String.class).blockingFirst()

        expect:
        response.status == HttpStatus.OK
        response.contentType.get() == MediaType.TEXT_PLAIN_TYPE
        response.body() == 'Hello Foo'
    }

    void "test invalid HTTP method"() {
        when:
        client.exchange(HttpRequest.POST("/parameters/uri/Foo", ""), String.class).blockingFirst()

        then:
        def e = thrown(HttpClientResponseException)
        def response = e.response
        response.status() == HttpStatus.METHOD_NOT_ALLOWED
        def allow = response.headers.getAll(HttpHeaders.ALLOW)
        allow == ["HEAD,GET"]
    }

    void "test query value"() {

        given:
        def response = client.exchange(HttpRequest.GET("/parameters/query?q=Foo"), String.class).blockingFirst()

        expect:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.TEXT_PLAIN_TYPE
        response.body() == 'Hello Foo'
    }

    void "test all parameters"() {

        given:
        def response = client.exchange(HttpRequest.GET("/parameters/allParams?name=Foo&age=20"), String.class).blockingFirst()

        expect:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.TEXT_PLAIN_TYPE
        response.body() == 'Hello Foo 20'
    }

    void "test header value"() {

        given:
        def response = client.exchange(HttpRequest.GET("/parameters/header").header(HttpHeaders.CONTENT_TYPE, "text/plain;q=1.0"), String.class).blockingFirst()

        expect:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.TEXT_PLAIN_TYPE
        response.body() == 'Hello text/plain;q=1.0'
    }

//    void "test request and response"() {
//
//        given:
//        def response = new MockGoogleResponse()
//        def googleRequest = new MockGoogleRequest(HttpMethod.GET, "/parameters/reqAndRes")
//        new HttpFunction()
//                .service(googleRequest, response)
//
//        expect:
//        response.statusCode == HttpStatus.ACCEPTED.code
//        response.contentType.get() == MediaType.TEXT_PLAIN
//        response.text == 'Good'
//    }

    void "test string body"() {

        given:
        def response = client.exchange(HttpRequest.POST( "/parameters/stringBody", "Foo").header(HttpHeaders.CONTENT_TYPE, "text/plain"), String.class).blockingFirst()

        expect:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.TEXT_PLAIN_TYPE
        response.body() == 'Hello Foo'
    }


    void "test writable"() {

        given:
        def response = client.exchange(HttpRequest.POST("/parameters/writable", "Foo")
                .header(HttpHeaders.CONTENT_TYPE, "text/plain"), String)
                .blockingFirst()

        expect:
        response.status() == HttpStatus.CREATED
        response.contentType.get() == MediaType.TEXT_PLAIN_TYPE
        response.body() == 'Hello Foo'
        response.headers.getAll("Foo") == ['Bar']
    }


    void "test JSON POJO body"() {
        given:
        def json = '{"name":"bar","age":30}'
        def response = client.exchange(HttpRequest.POST(
                "/parameters/jsonBody",
                json
        ).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON), String)
                .blockingFirst()

        expect:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.APPLICATION_JSON_TYPE
        response.body() == json
    }

    void "test JSON POJO body - invalid JSON"() {
        when:
        def json = '{"name":"bar","age":30'
        client.exchange(HttpRequest.POST(
                "/parameters/jsonBody",
                json
        ).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON), String)
                .blockingFirst()

        then:
        def e = thrown(HttpClientResponseException)
        def response = e.response
        response.status() == HttpStatus.BAD_REQUEST
        response.body().contains("Error decoding JSON stream for type")
    }


    void "test JSON POJO body with no @Body binds to arguments"() {
        given:
        def json = '{"name":"bar","age":30}'
        def response = client.exchange(HttpRequest.POST(
                "/parameters/jsonBodySpread",
                json
        ).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON), String)
                .blockingFirst()

        expect:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.APPLICATION_JSON_TYPE
        response.body() == json
    }

    void "full Micronaut request and response"() {
        given:
        def json = '{"name":"bar","age":30}'
        def response = client.exchange(HttpRequest.POST(
                "/parameters/fullRequest",
                json
        ).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON), String)
                .blockingFirst()

        expect:
        response.status() == HttpStatus.OK
        response.contentType.get() == MediaType.APPLICATION_JSON_TYPE
        response.body() == json
        response.headers.getAll("Foo") == ['Bar']
    }


    void "full Micronaut request and response - invalid JSON"() {
        when:
        def json = '{"name":"bar","age":30'
        client.exchange(HttpRequest.POST(
                "/parameters/fullRequest",
                json
        ).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON), String)
                .blockingFirst()

        then:
        def e = thrown(HttpClientResponseException)
        def response = e.response
        response.status() == HttpStatus.BAD_REQUEST
        response.body().contains("Error decoding JSON stream for type")
    }

}
