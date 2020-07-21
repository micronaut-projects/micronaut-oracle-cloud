package example;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Introspected
public class Book {
    @NotBlank
    private final String name;
    @Min(100)
    private final int pages;

    public Book(String name, int pages) {
        this.name = name;
        this.pages = pages;
    }

    public String getName() {
        return name;
    }

    public int getPages() {
        return pages;
    }
}
