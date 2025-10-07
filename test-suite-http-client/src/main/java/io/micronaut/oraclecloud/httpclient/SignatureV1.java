package io.micronaut.oraclecloud.httpclient;

import io.netty.handler.codec.http.FullHttpRequest;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class SignatureV1 {
    public static void verify(FullHttpRequest request, PublicKey publicKey) {
        if (request.headers().getAll("Authorization").size() != 1) {
            throw new IllegalArgumentException("Missing or too many Authorization headers");
        }
        Map<String, String> authorization = parseAuthorizationHeader(request.headers().get("Authorization"));
        if (!authorization.get("version").equals("1")) {
            throw new IllegalArgumentException("Unsupported version");
        }
        String[] headers = authorization.get("headers").split(" ");
        StringBuilder signingString = new StringBuilder();
        for (String header : headers) {
            if (header.equals("(request-target)")) {
                signingString.append(header).append(": ")
                        .append(request.method().name().toLowerCase(Locale.ROOT)).append(' ').append(request.uri())
                        .append("\n");
            } else {
                signingString.append(header).append(": ").append(request.headers().get(header)).append("\n");
            }
        }
        // remove trailing \n
        signingString.setLength(signingString.length() - 1);
        if (!authorization.get("algorithm").equals("rsa-sha256")) {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
        if (publicKey == null) {
            // null means no verification, we just check the basic format.
            return;
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(signingString.toString().getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(Base64.getDecoder().decode(authorization.get("signature")))) {
                throw new IllegalArgumentException("Signature failed to verify");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (InvalidKeyException | SignatureException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Map<String, String> parseAuthorizationHeader(String header) {
        if (!header.startsWith("Signature ")) {
            throw new IllegalArgumentException("Not a signature header");
        }
        header = header.substring("Signature ".length());
        Map<String, String> result = new HashMap<>();
        for (String s : header.split(",\\s*")) {
            String[] parts = s.split("=", 2);
            if (!parts[1].startsWith("\"") || !parts[1].endsWith("\"")) {
                throw new IllegalArgumentException("Invalid header");
            }
            String value = parts[1].substring(1, parts[1].length() - 1);
            result.put(parts[0], value);
        }
        return result;
    }
}
