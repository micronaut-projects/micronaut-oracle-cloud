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
package io.micronaut.oraclecloud.httpclient.apache.serde.serializers;

import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider.SessionTokenRefreshRequest.SessionTokenRequest;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Encoder;
import io.micronaut.serde.Serializer;
import jakarta.inject.Singleton;

import java.io.IOException;

/**
 * Serializer for {@link SessionTokenRequest} class. The serializer is required now because
 * the request does not have a getter, only a public property. It might be removed in the
 * future if this changes.
 */
@Internal
@Singleton
@Secondary
final class SessionTokenRequestSerializer implements Serializer<SessionTokenRequest> {

    @Override
    public void serialize(@NonNull Encoder encoder, EncoderContext context, @NonNull Argument<? extends SessionTokenRequest> type, @NonNull SessionTokenRequest value) throws IOException {
        encoder.encodeObject(type);
        encoder.encodeKey("currentToken");
        encoder.encodeString(value.currentToken);
        encoder.finishStructure();
    }

}
