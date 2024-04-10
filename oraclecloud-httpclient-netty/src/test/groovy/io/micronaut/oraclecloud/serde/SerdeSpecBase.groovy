package io.micronaut.oraclecloud.serde

import com.oracle.bmc.http.client.HttpClient
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel
import io.micronaut.context.ApplicationContext
import io.micronaut.core.beans.BeanIntrospection
import io.micronaut.oraclecloud.httpclient.netty.NettyHttpProvider
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
        try (HttpClient client = new NettyHttpProvider().newBuilder()
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

    ExplicitlySetBmcModel copyExplicitlySet(ExplicitlySetBmcModel from, ExplicitlySetBmcModel to) {
        return ModelUtils.copyExplicitlySet(from, to)
    }

    boolean equalsIgnoreExplicitlySet(ExplicitlySetBmcModel expected, ExplicitlySetBmcModel model) {
        return expected == copyExplicitlySet(expected, model)
    }

    // Class that has access to ExplicitlySetBmcModel protected methods
    static abstract class ModelUtils extends ExplicitlySetBmcModel {
        static ExplicitlySetBmcModel copyExplicitlySet(ExplicitlySetBmcModel from, ExplicitlySetBmcModel to) {
            BeanIntrospection.getIntrospection(((Object) from).getClass()).beanProperties.forEach(p -> {
                if (from.wasPropertyExplicitlySet(p.getName())) {
                    to.markPropertyAsExplicitlySet(p.getName())
                }
            })
            return from
        }
    }
}
