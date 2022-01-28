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

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import io.micronaut.configuration.jdbc.ucp.DatasourceConfiguration;
import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanInitializedEventListener;
import io.micronaut.context.event.BeanInitializingEvent;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.context.exceptions.NoSuchBeanException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.oraclecloud.atp.jdbc.AutonomousDatabaseConfiguration;
import io.micronaut.oraclecloud.atp.jdbc.OracleWalletArchiveProvider;
import io.micronaut.oraclecloud.atp.wallet.datasource.CanConfigureOracleDataSource;
import io.micronaut.oraclecloud.atp.wallet.datasource.OracleDataSourceAttributes;
import jakarta.inject.Singleton;
import oracle.ucp.jdbc.PoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.sql.SQLException;

/**
 * UCP connection pool listener that downloads oracle wallet and configures the {@link PoolDataSource}.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@Singleton
@Requires(sdk = Requires.Sdk.JAVA, value = "11")
@Requires(classes = PoolDataSource.class)
@Internal
public class UcpPoolConfigurationListener implements BeanInitializedEventListener<DatasourceConfiguration>, Ordered {

    public static final int POSITION = Ordered.HIGHEST_PRECEDENCE + 100;

    private static final String ORACLE_JDBC_POOL_ORACLE_DATA_SOURCE = "oracle.jdbc.pool.OracleDataSource";
    private static final Logger LOG = LoggerFactory.getLogger(UcpPoolConfigurationListener.class);

    private final OracleWalletArchiveProvider walletArchiveProvider;
    private final BeanLocator beanLocator;

    /**
     * Default constructor.
     *
     * @param walletArchiveProvider The wallet archive provider
     * @param beanLocator           The bean locator
     */
    protected UcpPoolConfigurationListener(@Nullable OracleWalletArchiveProvider walletArchiveProvider,
                                           @NonNull BeanLocator beanLocator) {
        this.walletArchiveProvider = walletArchiveProvider;
        this.beanLocator = beanLocator;
    }

    @Override
    public int getOrder() {
        return POSITION;
    }

    @Override
    public DatasourceConfiguration onInitialized(BeanInitializingEvent<DatasourceConfiguration> event) {

        DatasourceConfiguration bean = event.getBean();
        String beanName = bean.getName();

        AutonomousDatabaseConfiguration autonomousDatabaseConfiguration = beanLocator
                .findBean(AutonomousDatabaseConfiguration.class,
                        Qualifiers.byName(beanName)).orElse(null);

        if (autonomousDatabaseConfiguration == null) {
            LOG.trace("No AutonomousDatabaseConfiguration for [{}] datasource", beanName);
        } else if (autonomousDatabaseConfiguration.getOcid() == null || autonomousDatabaseConfiguration.getWalletPassword() == null) {
            LOG.trace("Skipping configuration of Oracle Wallet due to missing ocid or wallet password in " +
                    "AutonomousDatabaseConfiguration for [{}] datasource", beanName);
        } else {

            if (walletArchiveProvider == null && !beanLocator.findBean(AbstractAuthenticationDetailsProvider.class).isPresent()) {
                LOG.error("Datasource configuration [{}] requires to have the OCI SDK authentication configured.", beanName);
                throw new NoSuchBeanException(OracleWalletArchiveProvider.class);
            }

            LOG.trace("Retrieving Oracle Wallet for DataSource [{}]", beanName);

            CanConfigureOracleDataSource walletArchive = walletArchiveProvider
                    .loadWalletArchive(autonomousDatabaseConfiguration);

            try {
                if (StringUtils.isEmpty(bean.getConfiguredDriverClassName())) {
                    LOG.debug("Configured connection factory " + ORACLE_JDBC_POOL_ORACLE_DATA_SOURCE + " for [{}] datasource",
                            beanName);
                    bean.setDriverClassName(ORACLE_JDBC_POOL_ORACLE_DATA_SOURCE);
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
                        bean.getPoolDataSource().setSSLContext(sslContext);
                        return this;
                    }

                    @Override
                    public String url() {
                        return null;
                    }

                    @Override
                    public OracleDataSourceAttributes url(String url) {
                            bean.setUrl(url);
                            return this;
                    }

                    @Override
                    public String user() {
                        return bean.getUsername();
                    }

                    @Override
                    public OracleDataSourceAttributes user(String user) {
                        bean.setUsername(user);
                        return this;

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
                            bean.setPassword(String.valueOf(password));
                            return this;
                    }
                });

                LOG.debug("Successfully configured OracleWallet for [{}] datasource", beanName);
            } catch (IOException | SQLException e) {
                throw new ConfigurationException("Error configuring the [" + beanName + "] datasource: " + e.getMessage(), e);
            }
        }

        return bean;
    }
}
