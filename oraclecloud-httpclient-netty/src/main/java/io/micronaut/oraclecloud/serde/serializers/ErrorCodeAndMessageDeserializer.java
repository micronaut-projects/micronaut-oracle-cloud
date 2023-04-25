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
