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
package io.micronaut.oraclecloud.atp.jdbc.iam;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.Properties;

/**
 * Supplies JDBC connection properties required to enable IAM/OAuth2 token-based authentication
 * for Oracle Autonomous Database (ADB). Implementations are responsible for obtaining and refreshing
 * access tokens and returning the appropriate driver properties.
 *
 * <p>This module forwards the returned properties to the configured JDBC DataSource
 * (Hikari or UCP) so that Oracle JDBC can perform token-based authentication. The exact
 * property names and formats are driver-version specific and are owned by the implementation.</p>
 *
 * <p>Implementations may be qualified (named) to support multiple providers; the bean name can
 * be referenced via configuration to select a specific provider per datasource.</p>
 */
public interface IamDbTokenProvider {

    /**
     * Return connection properties enabling token-based authentication.
     *
     * @param dataSourceName The Micronaut datasource bean name
     * @param serviceAlias The selected ADB service alias (TNS alias) if known, otherwise null
     * @return Non-null, possibly empty, set of connection properties to merge into the DataSource
     */
    @NonNull
    Properties tokenConnectionProperties(@NonNull String dataSourceName, @Nullable String serviceAlias);
}
