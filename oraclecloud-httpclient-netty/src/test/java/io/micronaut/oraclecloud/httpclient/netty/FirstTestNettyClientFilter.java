package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import jakarta.inject.Singleton;

@Singleton
public class FirstTestNettyClientFilter implements OciNettyClientFilter<Void> {

    private long startTime;

    private long endTime;

    @Override
    public Void beforeRequest(HttpRequest request) {
        startTime = System.nanoTime();
        return null;
    }

    @Override
    public HttpResponse afterResponse(HttpRequest request, HttpResponse response, Throwable throwable, Void ignored) {
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
