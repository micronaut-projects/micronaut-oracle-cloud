package io.micronaut.oraclecloud.function.http;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(environments = Environment.ORACLE_CLOUD)
@Property(name = "fn.test.config.foo.bar", value = "good")
class ParameterBindingTest {

    @Client("/")
    @Inject
    HttpClient client;

    @Test
    void testUriParameterBinding() {
        HttpResponse<String> response = Mono.from(client.exchange(
                HttpRequest.GET("/parameters/uri/Fred"), String.class
        )).block();
        String result = response.body();
        assertEquals("Hello Fred", result);
    }

    @Test
    void testContextBinding() {
        HttpResponse<String> response = Mono.from(client.exchange(
                HttpRequest.GET("/parameters/context"), String.class
        )).block();
        String result = response.body();
        assertEquals("Got good context: myAppID", result);
    }

}
