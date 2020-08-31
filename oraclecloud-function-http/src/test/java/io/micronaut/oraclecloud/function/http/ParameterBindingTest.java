package io.micronaut.oraclecloud.function.http;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.micronaut.oraclecloud.function.http.test.FnHttpTest.invoke;

@MicronautTest
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

}
