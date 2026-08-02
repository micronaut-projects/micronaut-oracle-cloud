package io.micronaut.oraclecloud.queue

import groovy.transform.CompileStatic

import java.security.MessageDigest

@CompileStatic
class TestUtils {

    private static final HexFormat HEX = HexFormat.of()

    static String sha256(String s) {
        HEX.formatHex MessageDigest.getInstance('SHA-256').digest(s.bytes)
    }
}
