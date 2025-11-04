package example

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.BAD_REQUEST
import io.micronaut.http.HttpStatus.CREATED
import io.micronaut.http.HttpStatus.OK
import io.micronaut.http.MediaType.APPLICATION_JSON_TYPE
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@MicronautTest
class BookControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun testValidation() {
        val e = assertThrows(HttpClientResponseException::class.java) {
            Mono.from(
                client.exchange(
                    HttpRequest.POST("/books", Book("", 400))
                        .contentType(APPLICATION_JSON_TYPE), Book::class.java
                )
            ).block()
        }

        assertEquals(BAD_REQUEST, e.response.status())
    }

    @Test
    fun testListBooks() {
        val postBookResponse = Mono.from(
            client.exchange(
                HttpRequest.POST("/books", Book("Along Came a Spider", 400))
                    .contentType(APPLICATION_JSON_TYPE), Book::class.java
            )
        ).block()

        assertEquals(CREATED, postBookResponse!!.status())

        assertNotNull(postBookResponse.body())
        assertEquals(400, postBookResponse.body()!!.pages)

        val response = Mono.from(
            client.exchange(
                HttpRequest.GET<Any?>("/books"),
                Argument.listOf(Book::class.java)
            )
        ).block()

        assertEquals(OK, response!!.status())

        val body = response.body()
        assertNotNull(body)
        assertEquals(2, body!!.size)
    }
}
