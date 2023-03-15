/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import java.util.concurrent.ConcurrentHashMap
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Controller("/books")
class BookController {

    private val books: MutableMap<String, Book> = ConcurrentHashMap()

    init {
        books["The Stand"] = Book("The Stand", 1000)
        books["The Shining"] = Book("The Shining", 400)
    }

    @Get("/")
    fun books(): Collection<Book> = books.values

    @Post("/")
    fun save(@Body book: @Valid @NotNull Book): HttpResponse<Book> {
        books[book.title] = book
        return HttpResponse.created(book)
    }

    @Delete("/{title}")
    fun delete(title: @NotBlank String): Book? = books.remove(title)
}
