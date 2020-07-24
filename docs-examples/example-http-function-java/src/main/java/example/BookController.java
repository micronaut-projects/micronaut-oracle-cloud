package example;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller("/books")
public class BookController {
    private final Map<String, Book> books = new ConcurrentHashMap<>();

    public BookController() {
        books.put("The Stand", new Book("The Stand", 1000));
        books.put("The Shining", new Book("The Shining", 400));
    }

    @Get("/")
    Collection<Book> books() {
        return books.values();
    }

    @Post("/")
    @Status(HttpStatus.CREATED)
    Book save(@Valid @NotNull @Body Book book) {
        this.books.put(book.getTitle(), book);
        return book;
    }

    @Delete("/{title}")
    Book delete(@NotBlank String title) {
        return books.remove(title);
    }
}
