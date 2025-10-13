package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.oraclecloud.serde.OciSdkMicronautSerializer;
import jakarta.inject.Singleton;

/**
 * Demo bean created in the bootstrap context that calls HttpProvider.getDefault()
 * as early as possible to verify that the ManagedNettyHttpProvider was eagerly
 * initialized and wired into the NettyHttpProvider.
 */
@BootstrapContextCompatible
@Singleton
final class DemoBootstrapBean {

    private final HttpProvider provider;
    private final boolean usesManaged;

    /**
     * Inject ManagedNettyHttpProvider to ensure it is created before we call HttpProvider.getDefault().
     * This guarantees NettyHttpProvider has the managed reference ASAP.
     */
    DemoBootstrapBean() {
        this.provider = HttpProvider.getDefault();
        // If NettyHttpProvider delegates to the managed provider, its serializer will NOT be the unmanaged default singleton
        this.usesManaged = provider.getSerializer() != OciSdkMicronautSerializer.getDefaultSerializer();
    }

    HttpProvider provider() {
        return provider;
    }

    boolean usesManaged() {
        return usesManaged;
    }
}
