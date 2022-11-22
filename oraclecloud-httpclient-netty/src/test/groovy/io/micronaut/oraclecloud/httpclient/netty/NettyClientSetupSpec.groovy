package io.micronaut.oraclecloud.httpclient.netty

import com.oracle.bmc.http.client.HttpProvider
import com.oracle.bmc.monitoring.MonitoringClient
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class NettyClientSetupSpec extends Specification {
    def test() {
        given:
        def ctx = ApplicationContext.run()

        expect:
        ctx.getBean(MonitoringClient)
        HttpProvider.getDefault() instanceof NettyHttpProvider
    }
}
