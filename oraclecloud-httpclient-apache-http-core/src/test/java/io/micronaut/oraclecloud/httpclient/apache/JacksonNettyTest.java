package io.micronaut.oraclecloud.httpclient.apache;

import com.oracle.bmc.http.client.HttpProvider;

public class JacksonNettyTest extends NettyTest {
    @Override
    HttpProvider provider() {
        return new ApacheHttpProvider(new JacksonSerializer());
    }
}
