/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.oraclecloud.serde;

import com.oracle.bmc.auth.okeworkloadidentity.internal.contract.GetOkeResourcePrincipalSessionTokenAndKeysDetails;
import com.oracle.bmc.auth.okeworkloadidentity.internal.contract.OkeResourcePrincipalSessionTokenAndKeys;
import io.micronaut.context.ApplicationContext;
import io.micronaut.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OkeWorkloadIdentitySerdeTest {

    @Test
    void serializesAndDeserializesOkeWorkloadIdentityTokenExchangeModels() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            JsonMapper jsonMapper = context.getBean(JsonMapper.class);

            jsonMapper.writeValueAsString(GetOkeResourcePrincipalSessionTokenAndKeysDetails.builder().build());
            OkeResourcePrincipalSessionTokenAndKeys token = jsonMapper.readValue(
                "{\"token\":\"token\",\"privateKey\":\"private\",\"publicKey\":\"public\"}",
                OkeResourcePrincipalSessionTokenAndKeys.class
            );

            assertEquals("token", token.getToken());
        }
    }
}
