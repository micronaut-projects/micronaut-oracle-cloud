package io.micronaut.oci.streaming;

import com.oracle.bmc.auth.RegionProvider;
import io.micronaut.configuration.kafka.config.AbstractKafkaConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.oci.core.TenancyIdProvider;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.plain.PlainLoginModule;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Properties;

@Singleton
@Requires(env = Environment.ORACLE_CLOUD)
@TypeHint(PlainLoginModule.class)
public class OciStreamingConfigurer implements BeanCreatedEventListener<AbstractKafkaConfiguration<?, ?>> {
    private final Provider<RegionProvider> regionProvider;
    private final Provider<TenancyIdProvider> tenancyIdProvider;
    private final OciStreamingConfiguration streamingConfiguration;

    public OciStreamingConfigurer(
            OciStreamingConfiguration streamingConfiguration,
            Provider<RegionProvider> regionProvider,
            Provider<TenancyIdProvider> tenancyIdProviderProvider) {
        this.streamingConfiguration = streamingConfiguration;
        this.regionProvider = regionProvider;
        this.tenancyIdProvider = tenancyIdProviderProvider;
    }

    @Override
    public AbstractKafkaConfiguration<?, ?> onCreated(BeanCreatedEvent<AbstractKafkaConfiguration<?, ?>> event) {
        AbstractKafkaConfiguration<?, ?> bean = event.getBean();
        Properties config = bean.getConfig();
        config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "streaming." + regionProvider.get().getRegion() + ".oci.oraclecloud.com:9092");
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
        config.put(SaslConfigs.SASL_MECHANISM, "plain");
        config.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" +
                tenancyIdProvider.get().getTenancyId() + "/" +
                streamingConfiguration.getUsername() + "/" +
                streamingConfiguration.getStreamPoolId() + "\" password=\"" +
                streamingConfiguration.getAuthToken() + "\";"
        );

        return bean;
    }
}
