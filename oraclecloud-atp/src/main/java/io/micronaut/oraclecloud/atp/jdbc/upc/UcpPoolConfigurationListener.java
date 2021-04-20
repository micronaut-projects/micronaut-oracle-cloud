/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oraclecloud.atp.jdbc.upc;

import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanIdentifier;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.oraclecloud.atp.jdbc.AutonomousDatabaseConfiguration;
import io.micronaut.oraclecloud.atp.jdbc.OracleWalletArchiveProvider;
import io.micronaut.oraclecloud.atp.wallet.datasource.CanConfigureOracleDataSource;
import io.micronaut.oraclecloud.atp.wallet.datasource.OracleDataSourceAttributes;
import oracle.ucp.jdbc.PoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

/**
 * UCP connection pool listener that downloads oracle wallet and configures the {@link PoolDataSource}.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@Singleton
@Requires(classes = PoolDataSource.class)
@Requires(sdk = Requires.Sdk.JAVA, value = "11")
@Internal
public class UcpPoolConfigurationListener implements BeanCreatedEventListener<DataSource>, Ordered {

    public static final int POSITION = Ordered.HIGHEST_PRECEDENCE + 100;
    private static final String ORACLE_JDBC_POOL_ORACLE_DATA_SOURCE = "oracle.jdbc.pool.OracleDataSource";
    private static final Logger LOG = LoggerFactory.getLogger(UcpPoolConfigurationListener.class);

    private final OracleWalletArchiveProvider walletArchiveProvider;
    private final BeanLocator beanLocator;

    /**
     * Default constructor.
     * @param walletArchiveProvider The wallet archive provider
     * @param beanLocator The bean locator
     */
    protected UcpPoolConfigurationListener(OracleWalletArchiveProvider walletArchiveProvider, BeanLocator beanLocator) {
        this.walletArchiveProvider = walletArchiveProvider;
        this.beanLocator = beanLocator;
    }

    @Override
    public int getOrder() {
        return POSITION;
    }

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        final DataSource dataSource = event.getBean();
        if (dataSource instanceof PoolDataSource) {
            PoolDataSource bean = (PoolDataSource) dataSource;

            BeanIdentifier beanIdentifier = event.getBeanIdentifier();
            AutonomousDatabaseConfiguration autonomousDatabaseConfiguration = beanLocator
                    .findBean(AutonomousDatabaseConfiguration.class,
                            Qualifiers.byName(beanIdentifier.getName())).orElse(null);

            if (autonomousDatabaseConfiguration == null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("No AutonomousDatabaseConfiguration for [" + beanIdentifier.getName() + "] datasource");
                }
            } else if (autonomousDatabaseConfiguration.getOcid() == null || autonomousDatabaseConfiguration.getWalletPassword() == null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Skipping configuration of Oracle Wallet due to missin ocid or wallet password in " +
                            "AutonomousDatabaseConfiguration for [" + beanIdentifier.getName() + "] datasource");
                }
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Retrieving Oracle Wallet for DataSource [" + beanIdentifier.getName() + "]");
                }

                CanConfigureOracleDataSource walletArchive = walletArchiveProvider
                        .loadWalletArchive(autonomousDatabaseConfiguration);

                try {
                    if (StringUtils.isEmpty(bean.getConnectionFactoryClassName())) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Configured connection factory " + ORACLE_JDBC_POOL_ORACLE_DATA_SOURCE + " for [" + beanIdentifier.getName() + "] datasource");
                        }
                        bean.setConnectionFactoryClassName(ORACLE_JDBC_POOL_ORACLE_DATA_SOURCE);
                    }

                    walletArchive.configure(new OracleDataSourceAttributes() {

                        private SSLContext sslContext;

                        @Override
                        public SSLContext sslContext() {
                            return sslContext;
                        }

                        @Override
                        public OracleDataSourceAttributes sslContext(SSLContext sslContext) {
                            this.sslContext = sslContext;
                            bean.setSSLContext(sslContext);
                            return this;
                        }

                        @Override
                        public String url() {
                            return null;
                        }

                        @Override
                        public OracleDataSourceAttributes url(String url) {
                            try {
                                bean.setURL(url);
                                return this;
                            } catch (SQLException e) {
                                throw new ConfigurationException("Error configuring the [" + beanIdentifier.getName() + "] datasource url: " + e.getMessage(), e);
                            }
                        }

                        @Override
                        public String user() {
                            return bean.getUser();
                        }

                        @Override
                        public OracleDataSourceAttributes user(String user) {
                            try {
                                bean.setUser(user);
                                return this;
                            } catch (SQLException e) {
                                throw new ConfigurationException("Error configuring the [" + beanIdentifier.getName() + "] datasource user: " + e.getMessage(), e);
                            }
                        }

                        @Override
                        public char[] password() {
                            if (bean.getPassword() != null) {
                                return bean.getPassword().toCharArray();
                            } else {
                                return null;
                            }
                        }

                        @Override
                        public OracleDataSourceAttributes password(char[] password) {
                            try {
                                bean.setPassword(String.valueOf(password));
                                return this;
                            } catch (SQLException e) {
                                throw new ConfigurationException("Error configuring the [" + beanIdentifier.getName() + "] datasource password: " + e.getMessage(), e);
                            }
                        }
                    });

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Successfully configured OracleWallet for [" + beanIdentifier.getName() + "] datasource");
                    }
                } catch (IOException | SQLException e) {
                    throw new ConfigurationException("Error configuring the [" + beanIdentifier.getName() + "] datasource: " + e.getMessage(), e);
                }
            }
        }
        return dataSource;
    }
}
