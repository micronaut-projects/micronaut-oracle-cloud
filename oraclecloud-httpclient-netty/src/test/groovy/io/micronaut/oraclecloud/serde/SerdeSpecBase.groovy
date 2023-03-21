package io.micronaut.oraclecloud.serde

import com.oracle.bmc.http.client.HttpClient
import io.micronaut.context.ApplicationContext
import io.micronaut.oraclecloud.httpclient.netty.NettyHttpClientBuilder
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.Specification

import static com.oracle.bmc.http.client.Method.POST

class SerdeSpecBase extends Specification {

    EmbeddedServer initContext() {
        ApplicationContext ctx = ApplicationContext.run([
                'micronaut.server.port': '-1'
        ])
        return ctx.getBean(EmbeddedServer.class).start()
    }

    static <T> T echoTest(EmbeddedServer embeddedServer, Object requestBody, Class<T> bodyType = String) throws Exception {
        try (HttpClient client = new NettyHttpClientBuilder()
                .baseUri(embeddedServer.URI)
                .build()) {
            var response = client.createRequest(POST)
                    .appendPathPart('/echo')
                    .body(requestBody)
                    .execute().toCompletableFuture().get()
            if (bodyType == String) {
                return response.textBody().toCompletableFuture().get()
            }
            return response.body(bodyType).toCompletableFuture().get()
        }
    }

}
