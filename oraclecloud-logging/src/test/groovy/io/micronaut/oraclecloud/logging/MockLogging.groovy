package io.micronaut.oraclecloud.logging

import com.oracle.bmc.Region
import com.oracle.bmc.loggingingestion.Logging
import com.oracle.bmc.loggingingestion.LoggingClient
import com.oracle.bmc.loggingingestion.requests.PutLogsRequest
import com.oracle.bmc.loggingingestion.responses.PutLogsResponse
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@Requires(property = "spec.name")
@Singleton
@Replaces(LoggingClient)
class MockLogging implements Logging {

    final List<PutLogsRequest> putLogsRequestList = Collections.synchronizedList(new ArrayList<>())

    private boolean success = true

    @Override
    void refreshClient() {

    }

    @Override
    void setEndpoint(String endpoint) {

    }

    @Override
    void useRealmSpecificEndpointTemplate(boolean use) {

    }

    boolean getSuccess() {
        return success
    }

    void setSuccess(boolean success) {
        this.success = success
    }

    @Override
    String getEndpoint() {
        return 'mock-logging-endpoint'
    }

    @Override
    void setRegion(Region region) {

    }

    @Override
    void setRegion(String regionId) {

    }

    List<PutLogsRequest> getPutLogsRequestList() {
        synchronized (putLogsRequestList) {
            return new ArrayList<PutLogsRequest>(putLogsRequestList)
        }
    }

    @Override
    PutLogsResponse putLogs(PutLogsRequest request) {
        synchronized (putLogsRequestList) {
            putLogsRequestList.add(request)
        }
        if (success) {
            return PutLogsResponse.builder().opcRequestId("validId").build()
        }
        return PutLogsResponse.builder().opcRequestId(null).__httpStatusCode__(404).build()
    }

    @Override
    void close() throws Exception {

    }
}
