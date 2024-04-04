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

import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider.SessionToken;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import jakarta.inject.Singleton;

import java.io.IOException;

/**
 * Deserializer for {@link SessionToken} class. The deerializer is required now because of the
 * strange JsonValue and JsonBuilder annotation combination in the class that serde does not
 * seem to support correctly. This might change in the future.
 */
@Internal
@Singleton
final class SessionTokenDeserializer implements Deserializer<SessionToken> {

    @Override
    public @Nullable SessionToken deserialize(@NonNull Decoder decoder, DecoderContext context, @NonNull Argument<? super SessionToken> type) throws IOException {
        String token = null;

        Decoder objectDecoder = decoder.decodeObject(type);
        String prop = objectDecoder.decodeKey();
        while (prop != null) {
            if ("token".equals(prop)) {
                token = decoder.decodeStringNullable();
            } else {
                decoder.skipValue();
            }
            prop = objectDecoder.decodeKey();

        }
        return new SessionToken(token);
    }

}
