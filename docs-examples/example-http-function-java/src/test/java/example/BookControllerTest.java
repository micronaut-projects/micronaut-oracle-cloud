package example;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
public class BookControllerTest {

    @Inject
    @Client("/")
    RxHttpClient client;

    @Test
    void testValidation() {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> client
                .exchange(HttpRequest.POST("/books", new Book("", 400))
                        .contentType(APPLICATION_JSON_TYPE), Book.class)
                .blockingFirst()
        );

        assertEquals(BAD_REQUEST, e.getResponse().status());
    }

    @Test
    void testListBooks() {
        final HttpResponse<Book> postBookResponse = client
                .exchange(HttpRequest.POST("/books", new Book("Along Came a Spider", 400))
                        .contentType(APPLICATION_JSON_TYPE), Book.class)
                .blockingFirst();

        assertEquals(CREATED, postBookResponse.status());

        assertNotNull(postBookResponse.body());
        assertEquals(400, postBookResponse.body().getPages());

        HttpResponse<List<Book>> response = client
                .exchange(HttpRequest.GET("/books"), Argument.listOf(Book.class))
                .blockingFirst();

        assertEquals(OK, response.status());

        final List<Book> body = response.body();
        assertNotNull(body);
        assertEquals(2, body.size());
    }
}
