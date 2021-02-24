package io.micronaut.oraclecloud.monitoring.micrometer

import io.micrometer.core.instrument.Meter
import spock.lang.Shared
import spock.lang.Specification

class OracleCloudMetricsNamingConventionSpec extends Specification {

    @Shared
    private OracleCloudMetricsNamingConvention namingConvention = new OracleCloudMetricsNamingConvention()

    def "test meter name reserved oci_ prefix"(){
        expect:
        namingConvention.name("oci_foo", Meter.Type.COUNTER, null) == "m_oci_foo"
    }

    def "test meter name valid characters"(){
        expect:
        namingConvention.name("nonvalid@12#3.with_a\\n,d-foo\$", Meter.Type.COUNTER, null) == "nonvalid_12_3.with_a_n_d-foo\$"
        namingConvention.name("valid123.with_and-foo\$", Meter.Type.COUNTER, null) == "valid123.with_and-foo\$"
    }

    def "test meter name starts with alphabetical character"(){
        expect:
        namingConvention.name("ok_name", Meter.Type.COUNTER, null) == "ok_name"
        namingConvention.name("_nok_name", Meter.Type.COUNTER, null) == "m__nok_name"
    }

    def "test tag key characters"(){
        expect:
        namingConvention.tagKey("valid@12#3with_a\\n,d-foo\$") == "valid@12#3with_a\\n,d-foo\$"
        namingConvention.tagKey("invalid.key-with space") == "invalid_key-with_space"
    }

    def "test tag key length"() {
        expect:
        namingConvention.tagKey(repeat("x", 257)).length() == 256
    }

    def "test tag value characters"(){
        expect:
        namingConvention.tagValue("valid@12#3.with_a\\n,d-foo\$") == "valid@12#3.with_a\\n,d-foo\$"
        namingConvention.tagValue("invalid\ttab\n") == "invalid_tab_"
    }

    def "test tag value length"() {
        expect:
        namingConvention.tagValue(repeat("x", 257)).length() == 256
    }

    private String repeat(String s, int repeat) {
        return String.join("", Collections.nCopies(repeat, s));
    }
}
