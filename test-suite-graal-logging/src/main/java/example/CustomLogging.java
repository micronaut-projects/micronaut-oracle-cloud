package example;

import com.oracle.bmc.Region;
import com.oracle.bmc.loggingingestion.Logging;
import com.oracle.bmc.loggingingestion.LoggingClient;
import com.oracle.bmc.loggingingestion.model.LogEntry;
import com.oracle.bmc.loggingingestion.model.LogEntryBatch;
import com.oracle.bmc.loggingingestion.requests.PutLogsRequest;
import com.oracle.bmc.loggingingestion.responses.PutLogsResponse;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;


@Singleton
@Replaces(LoggingClient.class)
public class CustomLogging implements Logging {

    private final List<String> loggedMessages = new ArrayList<>();

    @Override
    public void refreshClient() {

    }

    @Override
    public void setEndpoint(String endpoint) {

    }

    @Override
    public String getEndpoint() {
        return null;
    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public void setRegion(String regionId) {

    }

    @Override
    public void useRealmSpecificEndpointTemplate(boolean realmSpecificEndpointTemplateEnabled) {

    }

    @Override
    public PutLogsResponse putLogs(PutLogsRequest request) {
        for (LogEntryBatch batch : request.getPutLogsDetails().getLogEntryBatches()) {
            for (LogEntry logEntry : batch.getEntries()) {
                loggedMessages.add(logEntry.getData());
            }
        }
        return PutLogsResponse.builder().opcRequestId("testId").build();
    }

    @Override
    public void close() throws Exception {

    }

    public List<String> getLoggedMessages() {
        return loggedMessages;
    }
}
