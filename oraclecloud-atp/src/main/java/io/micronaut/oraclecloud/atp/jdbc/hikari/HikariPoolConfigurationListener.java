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
package io.micronaut.oraclecloud.atp.jdbc.hikari;

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration;
import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanInitializedEventListener;
import io.micronaut.context.event.BeanInitializingEvent;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.oraclecloud.atp.jdbc.AutonomousDatabaseConfiguration;
import io.micronaut.oraclecloud.atp.jdbc.OracleWalletArchiveProvider;
import io.micronaut.oraclecloud.atp.wallet.datasource.CanConfigureOracleDataSource;
import oracle.jdbc.datasource.impl.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Hikari connection pool listener that downloads oracle wallet and extends the Hikari {@link DatasourceConfiguration}.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@Singleton
@Requires(classes = DatasourceConfiguration.class)
@Requires(sdk = Requires.Sdk.JAVA, value = "11")
@Requires(beans = OracleWalletArchiveProvider.class)
@Internal
public class HikariPoolConfigurationListener implements BeanInitializedEventListener<DatasourceConfiguration>, Ordered {
    public static final int POSITION = Ordered.HIGHEST_PRECEDENCE + 100;
    public static final String ORACLE_JDBC_ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
    private static final Logger LOG = LoggerFactory.getLogger(HikariPoolConfigurationListener.class);

    private final OracleWalletArchiveProvider walletArchiveProvider;
    private final BeanLocator beanLocator;

    /**
     * Default constructor.
     *
     * @param walletArchiveProvider The wallet archive provider
     * @param beanLocator           The bean locator
     */
    protected HikariPoolConfigurationListener(OracleWalletArchiveProvider walletArchiveProvider, BeanLocator beanLocator) {
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

        AutonomousDatabaseConfiguration autonomousDatabaseConfiguration = beanLocator.findBean(AutonomousDatabaseConfiguration.class,
                Qualifiers.byName(beanName)).orElse(null);

        if (autonomousDatabaseConfiguration == null) {
            LOG.trace("No AutonomousDatabaseConfiguration for [{}] datasource", beanName);
        } else if (autonomousDatabaseConfiguration.getOcid() == null || autonomousDatabaseConfiguration.getWalletPassword() == null) {
            LOG.trace("Skipping configuration of Oracle Wallet due to missin ocid or wallet password in " +
                    "AutonomousDatabaseConfiguration for [{}] datasource", beanName);
        } else {
            LOG.trace("Retrieving Oracle Wallet for DataSource [{}]", beanName);

            final CanConfigureOracleDataSource walletArchive = walletArchiveProvider
                    .loadWalletArchive(autonomousDatabaseConfiguration);

            try {
                OracleDataSource oracleDataSource = new OracleDataSource();
                walletArchive.configure(oracleDataSource);
                bean.setDataSource(oracleDataSource);
                bean.setUrl(oracleDataSource.getURL());
                bean.setDriverClassName(ORACLE_JDBC_ORACLE_DRIVER);
                final Properties dataSourceProperties = bean.getDataSourceProperties();
                if (dataSourceProperties != null && !dataSourceProperties.isEmpty()) {
                    oracleDataSource.setConnectionProperties(dataSourceProperties);
                }
            } catch (SQLException | IOException e) {
                throw new ConfigurationException("Error configuring the [" + beanName + "] datasource: " + e.getMessage(), e);
            }
        }
        return bean;
    }

}
