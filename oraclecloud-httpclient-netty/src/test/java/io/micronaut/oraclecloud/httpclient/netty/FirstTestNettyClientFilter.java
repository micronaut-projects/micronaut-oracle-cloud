package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import jakarta.inject.Singleton;

@Singleton
public class FirstTestNettyClientFilter implements OciNettyClientFilter<Object> {

    private long startTime;

    private long endTime;

    @Override
    public Object beforeRequest(HttpRequest request) {
        startTime = System.nanoTime();
        return new Object();
    }

    @Override
    public HttpResponse afterResponse(HttpRequest request, HttpResponse response, Throwable throwable, Object ignored) {
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
        return 0;
    }
}
