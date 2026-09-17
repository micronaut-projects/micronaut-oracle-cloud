from typing import Annotated

from jakarta.validation import Valid
from jakarta.validation.constraints import NotBlank, NotNull
from micronaut.http import HttpResponse
from micronaut.http.annotation import Body, Controller, Delete, Get, Post
from micronaut.validation import Validated

from example.Book import Book


@Validated
@Controller("/books")
class BookController:

    def __init__(self):
        self.books: dict[str, Book] = {
            "The Stand": Book("The Stand", 1000),
            "The Shining": Book("The Shining", 400)
        }

    @Get("/")
    def books(self) -> list[Book]:
        return list(self.books.values())

    @Post("/")
    def save(self, book: Annotated[Book, Valid, NotNull, Body]) -> HttpResponse[Book]:
        self.books[book.title] = book
        return HttpResponse.created(book)

    @Delete("/{title}")
    def delete(self, title: Annotated[str, NotBlank]) -> Book | None:
        return self.books.pop(title, None)
