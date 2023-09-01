/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.serde.serializers;

import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeResourcePrincipalSessionToken;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Map;

/**
 * Deserializer for {@link OkeResourcePrincipalSessionToken} class. The deserializer is required
 * because in first instance inside token field there is a base64 encoded json version of another {@link OkeResourcePrincipalSessionToken} instance.
 */
@Internal
@Singleton
final class TokenDeserializer implements Deserializer<OkeResourcePrincipalSessionToken> {

    @Override
    public OkeResourcePrincipalSessionToken deserialize(Decoder decoder, @NonNull DecoderContext context, @NonNull Argument type) throws IOException {
        Object decoded = decoder.decodeArbitrary();
        if (decoded instanceof String s) {
            return new OkeResourcePrincipalSessionToken(s);
        }
        return new OkeResourcePrincipalSessionToken(((Map<String, String>) decoded).get("token"));
    }
}
