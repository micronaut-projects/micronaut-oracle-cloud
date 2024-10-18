package io.micronaut.oraclecloud.httpclient.apache;

import com.oracle.bmc.http.client.HttpProvider;

public class UnmanagedSerdeNettyTest extends NettyTest {
    @Override
    HttpProvider provider() {
        return new ApacheHttpProvider(new SerdeSerializer());
    }
}
