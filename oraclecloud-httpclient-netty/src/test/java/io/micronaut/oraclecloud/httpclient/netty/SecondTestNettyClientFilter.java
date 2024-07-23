package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.Map;

@Singleton
public class SecondTestNettyClientFilter implements OciNettyClientFilter {

    private long startTime;

    private long endTime;

    @Override
    public Map<String, Object> beforeRequest(HttpRequest request) {
        startTime = System.nanoTime();
        return Collections.emptyMap();
    }

    @Override
    public HttpResponse afterResponse(HttpRequest request, HttpResponse response, Throwable throwable, Map<String, Object> context) {
        endTime = System.nanoTime();
        return response;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
