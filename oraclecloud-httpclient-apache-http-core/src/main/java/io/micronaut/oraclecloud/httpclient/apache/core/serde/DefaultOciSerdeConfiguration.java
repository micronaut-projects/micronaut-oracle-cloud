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
import io.micronaut.serde.LimitingStream;
import io.micronaut.serde.annotation.SerdeImport;
import io.micronaut.serde.config.SerdeConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Default OCI-specific serde configuration.
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
final class DefaultOciSerdeConfiguration implements OciSerdeConfiguration {
    private Optional<String> dateFormat = Optional.empty();
    private SerdeConfiguration.TimeShape timeWriteShape = SerdeConfiguration.TimeShape.STRING;
    private SerdeConfiguration.NumericTimeUnit numericTimeUnit = SerdeConfiguration.NumericTimeUnit.SECONDS;
    private boolean writeBinaryAsArray;
    private Optional<Locale> locale = Optional.empty();
    private Optional<TimeZone> timeZone = Optional.empty();
    private List<String> includedIntrospectionPackages = List.of("io.micronaut");
    private int maximumNestingDepth = LimitingStream.DEFAULT_MAXIMUM_DEPTH;

    @Override
    public Optional<String> getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(Optional<String> dateFormat) {
        this.dateFormat = dateFormat == null ? Optional.empty() : dateFormat;
    }

    @Override
    @Bindable(defaultValue = "STRING")
    public SerdeConfiguration.TimeShape getTimeWriteShape() {
        return timeWriteShape;
    }

    public void setTimeWriteShape(SerdeConfiguration.TimeShape timeWriteShape) {
        this.timeWriteShape = timeWriteShape;
    }

    @Override
    @Bindable(defaultValue = "SECONDS")
    public SerdeConfiguration.NumericTimeUnit getNumericTimeUnit() {
        return numericTimeUnit;
    }

    public void setNumericTimeUnit(SerdeConfiguration.NumericTimeUnit numericTimeUnit) {
        this.numericTimeUnit = numericTimeUnit;
    }

    @Override
    @Bindable(defaultValue = "false")
    public boolean isWriteBinaryAsArray() {
        return writeBinaryAsArray;
    }

    public void setWriteBinaryAsArray(boolean writeBinaryAsArray) {
        this.writeBinaryAsArray = writeBinaryAsArray;
    }

    @Override
    public Optional<Locale> getLocale() {
        return locale;
    }

    public void setLocale(Optional<Locale> locale) {
        this.locale = locale == null ? Optional.empty() : locale;
    }

    @Override
    public Optional<TimeZone> getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(Optional<TimeZone> timeZone) {
        this.timeZone = timeZone == null ? Optional.empty() : timeZone;
    }

    @Override
    @Bindable(defaultValue = "io.micronaut")
    public List<String> getIncludedIntrospectionPackages() {
        return includedIntrospectionPackages;
    }

    public void setIncludedIntrospectionPackages(List<String> includedIntrospectionPackages) {
        this.includedIntrospectionPackages = includedIntrospectionPackages;
    }

    @Override
    @Bindable(defaultValue = LimitingStream.DEFAULT_MAXIMUM_DEPTH + "")
    public int getMaximumNestingDepth() {
        return maximumNestingDepth;
    }

    public void setMaximumNestingDepth(int maximumNestingDepth) {
        this.maximumNestingDepth = maximumNestingDepth;
    }
}
