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

import com.oracle.bmc.http.internal.ResponseHelper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Map;

/**
 * Deserializer for {@link ResponseHelper.ErrorCodeAndMessage} class. The deserializer is required
 * since micronaut serde doesn't support builder deserialization and the constructor is package-private,
 * and cannot be read as introspections are generated in a different package.
 */
@Singleton
public class ErrorCodeAndMessageDeserializer implements Deserializer<ResponseHelper.ErrorCodeAndMessage> {

    @Override
    public ResponseHelper.ErrorCodeAndMessage deserialize(Decoder decoder, @NonNull DecoderContext context, @NonNull Argument type) throws IOException {
        ResponseHelper.ErrorCodeAndMessage.Builder response = ResponseHelper.ErrorCodeAndMessage.builder();

        Decoder objectDecoder = decoder.decodeObject(type);
        String prop = objectDecoder.decodeKey();
        while (prop != null) {
            switch (prop) {
                case "code":
                    response.code(deserializeString(decoder, context));
                    break;
                case "message":
                    response.message(deserializeString(decoder, context));
                    break;
                case "originalMessage":
                    response.originalMessage(deserializeString(decoder, context));
                    break;
                case "originalMessageTemplate":
                    response.originalMessageTemplate(deserializeString(decoder, context));
                    break;
                case "messageArguments":
                    response.messageArguments(deserializeMap(decoder, context));
                default:
            }
            prop = objectDecoder.decodeKey();
        }

        return response.build();
    }

    private String deserializeString(Decoder decoder, DecoderContext context) throws IOException {
        return context.findDeserializer(Argument.STRING)
            .deserialize(decoder, context, Argument.STRING);
    }

    private Map<String, String> deserializeMap(Decoder decoder, DecoderContext context) throws IOException {
        Argument<Map<String, String>> arg = Argument.mapOf(String.class, String.class);
        return context.findDeserializer(arg)
            .createSpecific(context, arg)
            .deserialize(decoder, context, arg);
    }
}
