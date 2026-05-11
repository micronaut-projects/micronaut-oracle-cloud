package io.micronaut.oraclecloud.logging

import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.core.io.Readable
import jakarta.inject.Singleton

@Requires(property = "app.filepath")
@Singleton
class ReadableConfiguration {
    final Readable readable

    ReadableConfiguration(@Value('${app.filepath}') Readable readable) {
        this.readable = readable
    }
}
