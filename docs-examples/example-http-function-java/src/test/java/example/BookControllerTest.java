package example;

import java.util.List;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.oci.function.http.test.FnHttpTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BookControllerTest {

    @Test
    void testListBooks() {
        final HttpResponse<Book> postBookResponse = FnHttpTest
                .invoke(HttpRequest.POST("/books", new Book("Along Came a Spider", 400))
                                .contentType(MediaType.APPLICATION_JSON_TYPE), Book.class);

        assertEquals(
                HttpStatus.CREATED,
                postBookResponse.status()
        );

        assertNotNull(postBookResponse.body());
        assertEquals(
                400,
                postBookResponse.body().getPages()
        );

        HttpResponse<List<Book>> response = FnHttpTest
                .invoke(HttpRequest.GET("/books"), Argument.listOf(Book.class));

        assertEquals(
                HttpStatus.OK,
                response.status()
        );

        final List<Book> body = response.body();
        assertNotNull(body);
        assertEquals(2, body.size());
    }
}
