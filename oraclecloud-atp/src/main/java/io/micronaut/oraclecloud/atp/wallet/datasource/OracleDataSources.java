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

import oracle.jdbc.datasource.OracleCommonDataSource;
import oracle.jdbc.pool.OracleDataSource;

import javax.net.ssl.SSLContext;
import java.sql.SQLException;

/** Services for working with {@link OracleDataSource} instances */
public class OracleDataSources {
    private static final OracleDataSources INSTANCE = new OracleDataSources();

    private OracleDataSources() {}

    public static OracleDataSources instance() {
        return INSTANCE;
    }

    public <T extends OracleCommonDataSource> Configurator<T> configurator(final T dataSource) {
        return new Configurator<>(dataSource);
    }

    public Builder builder() throws SQLException {
        return new Builder(configurator(new OracleDataSource()));
    }

    public static class Builder implements OracleDataSourceAttributes<Builder> {

        private final Configurator<OracleDataSource> configurator;

        Builder(Configurator<OracleDataSource> configurator) {
            this.configurator = configurator;
        }

        public OracleDataSource build() throws SQLException {
            return configurator.configure();
        }

        @Override
        public SSLContext sslContext() {
            return configurator.sslContext();
        }

        @Override
        public Builder sslContext(SSLContext sslContext) {
            configurator.sslContext(sslContext);
            return this;
        }

        @Override
        public String url() {
            return configurator.url();
        }

        @Override
        public Builder url(String url) {
            configurator.url(url);
            return this;
        }

        @Override
        public String user() {
            return configurator.user();
        }

        @Override
        public Builder user(String user) {
            configurator.user(user);
            return this;
        }

        @Override
        public char[] password() {
            return configurator.password();
        }

        @Override
        public Builder password(char[] password) {
            configurator.password(password);
            return this;
        }
    }

    public static class Configurator<T extends OracleCommonDataSource>
            extends OracleDataSourceAttributesBase<Configurator<T>>
            implements OracleDataSourceAttributes<Configurator<T>> {

        private final T dataSource;

        Configurator(final T dataSource) {
            this.dataSource = dataSource;
        }

        public T configure() throws SQLException {
            if (sslContext != null) {
                dataSource.setSSLContext(sslContext);
            }
            if (url != null) {
                dataSource.setURL(url);
            }
            if (user != null) {
                dataSource.setUser(url);
            }
            if (password != null) {
                dataSource.setPassword(new String(password));
            }
            return dataSource;
        }
    }
}
