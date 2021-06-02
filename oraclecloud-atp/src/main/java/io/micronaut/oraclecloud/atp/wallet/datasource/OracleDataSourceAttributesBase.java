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

import javax.net.ssl.SSLContext;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Base class implementation of {@link OracleDataSourceAttributes}.
 *
 * @param <T> The concrete sub-type of this type
 */
class OracleDataSourceAttributesBase<T extends OracleDataSourceAttributes>
        implements OracleDataSourceAttributes<T> {
    protected SSLContext sslContext;
    protected String url;
    protected String user;
    protected char[] password;

    public OracleDataSourceAttributesBase() {
        super();
    }

    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    @Override
    public T sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return (T) this;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public T url(String url) {
        this.url = url;
        return (T) this;
    }

    @Override
    public String user() {
        return user;
    }

    @Override
    public T user(String user) {
        this.user = user;
        return (T) this;
    }

    @Override
    public char[] password() {
        if (password == null) {
            return null;
        } else {
            return Arrays.copyOf(password, password.length);
        }
    }

    @Override
    public T password(char[] password) {
        if (password == null) {
            erase(this.password);
            this.password = null;
        } else {
            this.password = Arrays.copyOf(password, password.length);
        }
        return (T) this;
    }

    private void erase(char[] text) {
        if (text != null) {
            Arrays.fill(text, '*');
        }
    }

    static class Configurator<T extends OracleCommonDataSource>
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
