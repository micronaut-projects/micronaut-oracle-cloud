from typing import Annotated

from jakarta.inject import Inject
from micronaut.core.type import Argument
from micronaut.http import HttpRequest, HttpStatus, MediaType
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.http.client.exceptions import HttpClientResponseException
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test
from reactor.core.publisher import Mono

from example.Book import Book


@MicronautTest
class BookControllerTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def test_validation(self) -> None:
        try:
            Mono.from_(self.client.exchange(
                HttpRequest.POST("/books", Book("", 400)).contentType(MediaType.APPLICATION_JSON_TYPE),
                Book)).block()
        except HttpClientResponseException as e:
            assert e.getResponse().status() == HttpStatus.BAD_REQUEST
        else:
            assert False, "validation should have failed"

    @Test
    def test_list_books(self) -> None:
        post_book_response = Mono.from_(self.client.exchange(
            HttpRequest.POST("/books", Book("Along Came a Spider", 400)).contentType(MediaType.APPLICATION_JSON_TYPE),
            Book)).block()

        assert post_book_response.status() == HttpStatus.CREATED

        assert post_book_response.body() is not None
        assert post_book_response.body().pages == 400

        response = Mono.from_(self.client.exchange(HttpRequest.GET("/books"), Argument.listOf(Book))).block()

        assert response.status() == HttpStatus.OK

        body = response.body()
        assert body is not None
        assert len(body) == 2
