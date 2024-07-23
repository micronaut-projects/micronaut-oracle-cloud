package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class SecondTestNettyClientFilter implements NettyClientFilter {

    private long startTime;

    private long endTime;

    @Override
    public void beforeRequest(HttpRequest request, Map<String, Object> context) {
        startTime = System.nanoTime();
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
    public int getPriority() {
        return 1;
    }
}
