package io.micronaut.oraclecloud.httpclient.apache.core;

import com.oracle.bmc.http.client.HttpProvider;

public class UnmanagedSerdeNettyTest extends ApacheNettyTest {
    @Override
    HttpProvider provider() {
        return new ApacheCoreHttpProvider(new SerdeSerializer());
    }
}
