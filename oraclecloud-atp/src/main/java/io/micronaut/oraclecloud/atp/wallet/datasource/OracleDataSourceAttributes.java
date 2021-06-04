/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.oraclecloud.atp.wallet.datasource;

import io.micronaut.core.annotation.Internal;

import javax.net.ssl.SSLContext;

/**
 * Represents the attributes of a {@link javax.sql.DataSource} that a {@link io.micronaut.oraclecloud.atp.wallet.Wallet} can configure.
 * Only the attributes that a io.micronaut.oraclecloud.adb.wallet can influence are enumerated in this type.
 *
 * @param <T> the data source generic type
 */
@Internal
public interface OracleDataSourceAttributes<T extends OracleDataSourceAttributes> {

    /**
     * The configured {@link SSLContext} if any.
     *
     * @return SSLContext instance, or null if no instance configured
     */
    SSLContext sslContext();

    /**
     * Configure the {@link SSLContext} used to create TLS connections to the database.
     *
     * @param sslContext The {@link SSLContext} holding the certificates used to secure access to
     *     the database
     * @return self
     */
    T sslContext(SSLContext sslContext);

    /**
     * The configured JDBC URL if any.
     *
     * @return String instance or null if no JDBC url configured
     */
    String url();

    /**
     * Configure the JDBC url to use to connect to the database.
     *
     * @param url The JDBC url of the database
     * @return self
     */
    T url(String url);

    /**
     * The configured database user if any.
     *
     * @return Database user name, or null if no username configured
     */
    String user();

    /**
     * Configure the database user to connect to.
     *
     * @param user The database username
     * @return self
     */
    T user(String user);

    /**
     * The configured database password if any. Note a copy of the password is returned, the caller
     * is responsible for zeroing out the array once the password value has been consumed.
     *
     * @return char[] array holding a copy of the password, or null if no password has been
     *     configured
     */
    char[] password();

    /**
     * Configure the database password to use to connect to the database.
     *
     * @param password The password to use to connect to the database. The caller should zero out
     *     this array after this method has been invoked. The implementor must create a copy of the
     *     supplied array
     * @return self
     */
    T password(char[] password);
}
