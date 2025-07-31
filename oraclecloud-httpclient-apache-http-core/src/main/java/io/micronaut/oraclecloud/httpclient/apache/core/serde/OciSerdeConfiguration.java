/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.oraclecloud.httpclient.apache.core.serde;

import com.oracle.bmc.auth.internal.GetResourcePrincipalSessionTokenRequest;
import com.oracle.bmc.auth.internal.JWK;
import com.oracle.bmc.auth.internal.X509FederationClient;
import com.oracle.bmc.auth.okeworkloadidentity.internal.GetOkeResourcePrincipalSessionTokenDetails;
import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeResourcePrincipalSessionToken;
import com.oracle.bmc.encryption.internal.EncryptionHeader;
import com.oracle.bmc.encryption.internal.EncryptionKey;
import com.oracle.bmc.http.internal.ResponseHelper;
import com.oracle.bmc.model.RegionSchema;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.serde.annotation.SerdeImport;
import io.micronaut.serde.config.SerdeConfiguration;

/**
 * OCI-specific serde configuration.
 */
@ConfigurationProperties("oci.serde")
@Bean(typed = OciSerdeConfiguration.class)
@Internal
@BootstrapContextCompatible
@SerdeImport(GetResourcePrincipalSessionTokenRequest.class)
@SerdeImport(JWK.class)
@SerdeImport(RegionSchema.class)
@SerdeImport(ResponseHelper.ErrorCodeAndMessage.class)
@SerdeImport(X509FederationClient.SecurityToken.class)
@SerdeImport(X509FederationClient.X509FederationRequest.class)
@SerdeImport(GetOkeResourcePrincipalSessionTokenDetails.class)
@SerdeImport(OkeResourcePrincipalSessionToken.class)
@SerdeImport(value = EncryptionHeader.class, deserializable = false)
@SerdeImport(EncryptionKey.class)
public interface OciSerdeConfiguration extends SerdeConfiguration {
    @Override
    @Bindable(defaultValue = "false")
    boolean isWriteBinaryAsArray();
}
