/*
 * Copyright 2017-2025 original authors
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

import com.oracle.bmc.encryption.internal.EncryptionHeader;
import com.oracle.bmc.encryption.internal.EncryptionKey;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Deserializer;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.serde.exceptions.SerdeException;
import io.micronaut.serde.util.CustomizableDeserializer;
import jakarta.inject.Singleton;

import java.util.List;

@Internal
@Singleton
@BootstrapContextCompatible
@Requires(classes = EncryptionHeader.class)
final class EncryptionHeaderDeserializer implements CustomizableDeserializer<EncryptionHeader> {

    @Override
    public @NonNull Deserializer<EncryptionHeader> createSpecific(@NonNull DecoderContext context, @NonNull Argument<? super EncryptionHeader> type) throws SerdeException {
        Argument<EncryptionHeaderDto> dtoArg = Argument.of(EncryptionHeaderDto.class);
        Deserializer<? extends EncryptionHeaderDto> dtoDeserializer = context.findDeserializer(dtoArg).createSpecific(context, dtoArg);
        return (decoder, context1, type1) -> {
            EncryptionHeaderDto dto = dtoDeserializer.deserialize(decoder, context1, dtoArg);
            EncryptionHeader result = new EncryptionHeader();
            for (EncryptionKey key : dto.encryptedDataKeys) {
                result.setEncryptionHeader(key, dto.IV, dto.additionalAuthenticatedData);
            }
            return result;
        };
    }

    @Serdeable.Deserializable
    record EncryptionHeaderDto(
        String additionalAuthenticatedData,
        String IV,
        List<EncryptionKey> encryptedDataKeys
    ) {
    }
}
