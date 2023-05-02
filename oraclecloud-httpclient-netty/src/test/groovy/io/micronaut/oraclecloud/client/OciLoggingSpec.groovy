package io.micronaut.oraclecloud.client

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.logging.LoggingManagement
import com.oracle.bmc.logging.LoggingManagementClient
import com.oracle.bmc.logging.model.*
import com.oracle.bmc.logging.requests.*
import com.oracle.bmc.loggingingestion.Logging
import com.oracle.bmc.loggingingestion.LoggingClient
import com.oracle.bmc.loggingingestion.model.LogEntry
import com.oracle.bmc.loggingingestion.model.LogEntryBatch
import com.oracle.bmc.loggingingestion.model.PutLogsDetails
import com.oracle.bmc.loggingingestion.requests.PutLogsRequest
import com.oracle.bmc.loggingsearch.LogSearch
import com.oracle.bmc.loggingsearch.LogSearchClient
import com.oracle.bmc.loggingsearch.model.SearchLogsDetails
import com.oracle.bmc.loggingsearch.requests.SearchLogsRequest
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.Instant
import java.time.temporal.ChronoUnit

@Requires(property = "test.compartment.id")
@Requires(bean = ConfigFileAuthenticationDetailsProvider)
@MicronautTest
@Stepwise
class OciLoggingSpec extends Specification {

    @Shared
    @Property(name = "test.compartment.id")
    String compartmentId

    @Shared
    @Inject
    @NonNull
    ConfigFileAuthenticationDetailsProvider authenticationDetailsProvider

    @Shared LoggingManagement loggingClient

    @Shared String logGroupName
    @Shared String logGroupId
    @Shared String logName
    @Shared String logId

    @spock.lang.Requires({ instance.compartmentId && instance.authenticationDetailsProvider })
    void "create log group"() {
        given:
        loggingClient = buildLoggingClient()
        logGroupName = "micronaut_test_" + new Random().nextInt(0, Integer.MAX_VALUE)

        var body = CreateLogGroupDetails.builder()
            .compartmentId(compartmentId)
            .displayName(logGroupName)
            .build()

        when:
        var response = loggingClient.createLogGroup(CreateLogGroupRequest.builder().createLogGroupDetails(body).build())

        then:
        response.__httpStatusCode__ < 300
    }

    void "find and wait for log group"() {
        when:
        var request = ListLogGroupsRequest.builder()
                .compartmentId(compartmentId)
                .limit(1)
                .displayName(logGroupName)
                .build()
        var response = loggingClient.listLogGroups(request)
        logGroupId = response.items[0].id

        then:
        response.items.size() == 1
        response.items[0].id != null
        response.items[0].displayName == logGroupName
        response.items[0].compartmentId == compartmentId

        when:
        var waitRequest = GetLogGroupRequest.builder().logGroupId(logGroupId).build()
        var waitResponse = loggingClient.waiters
                .forLogGroup(waitRequest, LogGroupLifecycleState.Active, LogGroupLifecycleState.Inactive).execute()

        then:
        waitResponse.__httpStatusCode__ < 300
        waitResponse.logGroup.id == logGroupId
        waitResponse.logGroup.displayName == logGroupName
        waitResponse.logGroup.lifecycleState == LogGroupLifecycleState.Active
    }

    void "create log"() {
        given:
        logName = "micronaut_test"

        var body = CreateLogDetails.builder()
            .displayName(logName)
            .logType(CreateLogDetails.LogType.Custom)
            .configuration(Configuration.builder().compartmentId(compartmentId).build())
            .build()

        when:
        var request = CreateLogRequest.builder()
                .createLogDetails(body)
                .logGroupId(logGroupId)
                .build()
        var response = loggingClient.createLog(request)

        then:
        response.__httpStatusCode__ < 300
    }

    void "find and wait for log"() {
        when:
        var request = ListLogsRequest.builder()
                .logGroupId(logGroupId).limit(1).displayName(logName).build()
        var response = loggingClient.listLogs(request)
        logId = response.items[0].id

        then:
        response.items.size() == 1
        response.items[0].id != null
        response.items[0].displayName == logName
        response.items[0].compartmentId == compartmentId
        response.items[0].logGroupId == logGroupId

        when:
        var waitRequest = GetLogRequest.builder().logGroupId(logGroupId).logId(logId).build()
        var waitResponse = loggingClient.waiters
                .forLog(waitRequest, LogLifecycleState.Active, LogLifecycleState.Inactive).execute()

        then:
        waitResponse.__httpStatusCode__ < 300
        waitResponse.log.id == logId
        waitResponse.log.displayName == logName
        waitResponse.log.lifecycleState == LogLifecycleState.Active
    }

    void "ingest logs"() {
        given:
        var loggingIngestionClient = buildLoggingIngestionClient()
        var body = PutLogsDetails.builder()
            .logEntryBatches([
                LogEntryBatch.builder()
                    .entries([
                        LogEntry.builder()
                                .id("1")
                                .time(Date.from(Instant.now().minusSeconds(30)))
                                .data("Start logging")
                                .build(),
                        LogEntry.builder()
                                .id("2")
                                .time(new Date())
                                .data("Test logging")
                                .build()
                    ])
                    .type("String")
                    .source(null)
                    .build()
            ])
            .specversion("1.0")
            .build()

        when:
        var request = PutLogsRequest.builder().logId(logId).putLogsDetails(body).build()
        var response = loggingIngestionClient.putLogs(request)

        then:
        response.__httpStatusCode__ < 300
    }

    void "get ingested logs"() {
        given:
        var loggingSearchClient = buildLoggingSearchClient()
        var body = SearchLogsDetails.builder()
            .searchQuery("search \"${compartmentId}/${logGroupId}/${logId}\"")
            .timeStart(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
            .timeEnd(Date.from(Instant.now()))
            .build()

        when:
        var request = SearchLogsRequest.builder().searchLogsDetails(body).build()
        var response = loggingSearchClient.searchLogs(request)

        then:
        response.__httpStatusCode__ < 300
        response.searchResponse.summary.resultCount == 2
        response.searchResponse.results.size() == 2
        response.searchResponse.results[0].data["logContent"]["data"]["message"] == "Start logging"
        response.searchResponse.results[1].data["logContent"]["data"]["message"] == "Test logging"
    }

    void "delete log"() {
        when:
        var request = DeleteLogRequest.builder().logGroupId(logGroupId).logId(logId).build()
        var response = loggingClient.deleteLog(request)

        then:
        response.__httpStatusCode__ < 300
    }

    void "delete log group"() {
        when:
        var request = DeleteLogGroupRequest.builder().logGroupId(logGroupId).build()
        var response = loggingClient.deleteLogGroup(request)

        then:
        response.__httpStatusCode__ < 300
    }

    LoggingManagement buildLoggingClient() {
        return LoggingManagementClient.builder().build(authenticationDetailsProvider)
    }

    Logging buildLoggingIngestionClient() {
        return LoggingClient.builder().build(authenticationDetailsProvider)
    }

    LogSearch buildLoggingSearchClient() {
        return LogSearchClient.builder().build(authenticationDetailsProvider)
    }

}
