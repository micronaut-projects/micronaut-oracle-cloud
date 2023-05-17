package io.micronaut.oraclecloud.serde

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel
import io.micronaut.runtime.server.EmbeddedServer

import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

class DateSerdeSpec extends SerdeSpecBase {

    void "test Date serialization"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        def dateTime = ZonedDateTime.of(
                LocalDateTime.of(2000, Month.JANUARY, 2, 3, 4, 5, (int) 678e6),
                ZoneId.of("America/Toronto")
        )
        def date = new Date(dateTime.toInstant().toEpochMilli())

        var value = echoTest(embeddedServer, date).replaceAll('"', '')
        var requiredValue = "2000-01-02T08:04:05.678Z"

        then:
        requiredValue == value
    }

    void "test Date deserialization '#dateString'"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        Calendar calendar = Calendar.getInstance(
                TimeZone.getTimeZone(ZoneId.of('-05:00')),
                Locale.CANADA
        )
        calendar.setTimeInMillis(789)
        calendar.set(2000, Calendar.JANUARY, 2, 3, 4, 56)
        Date requiredDate = calendar.getTime()

        when:
        var dateModel = echoTest(embeddedServer, dateString, DateModel)

        then:
        requiredDate == dateModel.date

        where:
        dateString                                       | _
        '{"date":"2000-01-02T03:04:56.789-05:00"}'        | _
        '{"date":"2000-01-02T08:04:56.789Z"}'            | _
        '{"date":"2000-01-02T09:04:56.789+01:00"}'        | _
    }

    void "test Date deserialization '#dateText'"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var body = "{\"date\":\"${dateText}\"}"
        var dateModel = echoTest(embeddedServer, body.toString(), DateModel)
        var date = Date.from(Instant.parse(dateText))

        then:
        date == dateModel.date

        where:
        dateText | _
        '1937-01-01T12:00:27.873834939+00:20' | _
        '1937-01-01T12:00:27.873834939Z'      | _
        '2021-04-29T10:00:00Z'                | _
        '2021-05-13T10:58:09.628313-07:00'    | _
    }

    static class DateModel extends ExplicitlySetBmcModel {
        Date date
    }

}
