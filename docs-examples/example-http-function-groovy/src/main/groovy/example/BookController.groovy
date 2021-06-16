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

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status

import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import java.util.concurrent.ConcurrentHashMap

@Controller('/books')
@CompileStatic
class BookController {

    private final Map<String, Book> books = new ConcurrentHashMap<>()

    BookController() {
        books['The Stand'] = new Book('The Stand', 1000)
        books['The Shining'] = new Book('The Shining', 400)
    }

    @Get('/')
    Collection<Book> books() {
        books.values()
    }

    @Post('/')
    @Status(HttpStatus.CREATED)
    Book save(@Valid @NotNull @Body Book book) {
        books[book.title] = book
        book
    }

    @Delete('/{title}')
    Book delete(@NotBlank String title) {
        books.remove(title)
    }
}
