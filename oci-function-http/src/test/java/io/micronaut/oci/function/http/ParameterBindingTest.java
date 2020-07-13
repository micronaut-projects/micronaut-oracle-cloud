package io.micronaut.oci.function.http;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.micronaut.oci.function.http.test.FnHttpTest.invoke;

public class ParameterBindingTest {

    @Test
    void testUriParameterBinding() {
        HttpResponse<String> response = invoke(
                HttpRequest.GET("/parameters/uri/Fred")
        );
        String result = response.body();
        Assertions.assertEquals("Hello Fred", result);
    }

}
