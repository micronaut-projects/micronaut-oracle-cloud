package io.micronaut.oraclecloud.function.http;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@MicronautTest
@Property(name = "fn.test.config.foo.bar", value = "good")
public class ParameterBindingTest {

    @Client("/")
    @Inject
    RxHttpClient client;

    @Test
    void testUriParameterBinding() {
        HttpResponse<String> response = client.exchange(
                HttpRequest.GET("/parameters/uri/Fred"), String.class
        ).blockingFirst();
        String result = response.body();
        Assertions.assertEquals("Hello Fred", result);
    }

    @Test
    void testContextBinding() {
        HttpResponse<String> response = client.exchange(
                HttpRequest.GET("/parameters/context"), String.class
        ).blockingFirst();
        String result = response.body();
        Assertions.assertEquals("Got good context: myAppID", result);
    }

}
