package example

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Mono
import spock.lang.Specification

import jakarta.inject.Inject

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.MediaType.APPLICATION_JSON_TYPE

@MicronautTest
class BookControllerSpec extends Specification {

    @Inject
    @Client('/')
    HttpClient client

    void 'test validation'() {
        when:
        Mono.from(client.exchange(HttpRequest.POST('/books', new Book('', 400))
                        .contentType(APPLICATION_JSON_TYPE), Book))
                .block()

        then:
        HttpClientResponseException e = thrown()
        e.response.status() == BAD_REQUEST
    }

    void 'test list books'() {
        when:
        HttpResponse<Book> postBookResponse = Mono.from(client
                .exchange(HttpRequest.POST('/books', new Book('Along Came a Spider', 400))
                                .contentType(APPLICATION_JSON_TYPE), Book))
                                .block()

        then:
        postBookResponse.status() == CREATED
        postBookResponse.body()
        400 == postBookResponse.body().pages

        when:
        HttpResponse<List<Book>> response = Mono.from(client
                .exchange(HttpRequest.GET('/books'), Argument.listOf(Book))
        ).block()

        then:
        response.status() == OK

        when:
        List<Book> body = response.body()

        then:
        body
        2 == body.size()
    }
}
