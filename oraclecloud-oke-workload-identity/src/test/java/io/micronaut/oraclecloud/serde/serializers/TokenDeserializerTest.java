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
import io.micronaut.serde.Decoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TokenDeserializerTest  {

    @Test
    void testDeserializerString() throws IOException {
        TokenDeserializer tokenDeserializer = new TokenDeserializer();
        Decoder d = mock(Decoder.class);

        String decodeString = "test";
        when(d.decodeArbitrary()).thenReturn(decodeString);
        OkeResourcePrincipalSessionToken token = tokenDeserializer.deserialize(d, mock(), mock());

        Assertions.assertEquals("test", token.getToken());
    }

    @Test
    void testDeserializerMap() throws IOException {
        TokenDeserializer tokenDeserializer = new TokenDeserializer();
        Decoder d = mock(Decoder.class);

        Map decodeMap = Map.of("token", "test");
        when(d.decodeArbitrary()).thenReturn(decodeMap);
        OkeResourcePrincipalSessionToken token = tokenDeserializer.deserialize(d, mock(), mock());

        Assertions.assertEquals("test", token.getToken());
    }
}
