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
package io.micronaut.oraclecloud.clients;

import com.oracle.bmc.analytics.AnalyticsAsyncClient;
import com.oracle.bmc.analytics.AnalyticsClient;
import com.oracle.bmc.announcementsservice.AnnouncementAsyncClient;
import com.oracle.bmc.announcementsservice.AnnouncementClient;
import com.oracle.bmc.apigateway.GatewayAsyncClient;
import com.oracle.bmc.apigateway.GatewayClient;
import com.oracle.bmc.applicationmigration.ApplicationMigrationAsyncClient;
import com.oracle.bmc.applicationmigration.ApplicationMigrationClient;
import com.oracle.bmc.audit.AuditAsyncClient;
import com.oracle.bmc.audit.AuditClient;
import com.oracle.bmc.autoscaling.AutoScalingAsyncClient;
import com.oracle.bmc.autoscaling.AutoScalingClient;
import com.oracle.bmc.bds.BdsAsyncClient;
import com.oracle.bmc.bds.BdsClient;
import com.oracle.bmc.budget.BudgetAsyncClient;
import com.oracle.bmc.budget.BudgetClient;
import com.oracle.bmc.cims.IncidentAsyncClient;
import com.oracle.bmc.cims.IncidentClient;
import com.oracle.bmc.containerengine.ContainerEngineAsyncClient;
import com.oracle.bmc.containerengine.ContainerEngineClient;
import com.oracle.bmc.core.BlockstorageAsyncClient;
import com.oracle.bmc.core.BlockstorageClient;
import com.oracle.bmc.core.ComputeAsyncClient;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.ComputeManagementAsyncClient;
import com.oracle.bmc.core.ComputeManagementClient;
import com.oracle.bmc.core.VirtualNetworkAsyncClient;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.database.DatabaseAsyncClient;
import com.oracle.bmc.database.DatabaseClient;
import com.oracle.bmc.datacatalog.DataCatalogAsyncClient;
import com.oracle.bmc.datacatalog.DataCatalogClient;
import com.oracle.bmc.dataflow.DataFlowAsyncClient;
import com.oracle.bmc.dataflow.DataFlowClient;
import com.oracle.bmc.dataintegration.DataIntegrationAsyncClient;
import com.oracle.bmc.dataintegration.DataIntegrationClient;
import com.oracle.bmc.datasafe.DataSafeClient;
import com.oracle.bmc.datascience.DataScienceAsyncClient;
import com.oracle.bmc.datascience.DataScienceClient;
import com.oracle.bmc.dts.ApplianceExportJobAsyncClient;
import com.oracle.bmc.dts.ApplianceExportJobClient;
import com.oracle.bmc.dts.ShippingVendorsAsyncClient;
import com.oracle.bmc.dts.ShippingVendorsClient;
import com.oracle.bmc.dts.TransferApplianceAsyncClient;
import com.oracle.bmc.dts.TransferApplianceClient;
import com.oracle.bmc.dts.TransferApplianceEntitlementAsyncClient;
import com.oracle.bmc.dts.TransferApplianceEntitlementClient;
import com.oracle.bmc.dts.TransferDeviceAsyncClient;
import com.oracle.bmc.dts.TransferDeviceClient;
import com.oracle.bmc.dts.TransferJobAsyncClient;
import com.oracle.bmc.dts.TransferJobClient;
import com.oracle.bmc.dts.TransferPackageAsyncClient;
import com.oracle.bmc.dts.TransferPackageClient;
import com.oracle.bmc.email.EmailAsyncClient;
import com.oracle.bmc.email.EmailClient;
import com.oracle.bmc.events.EventsAsyncClient;
import com.oracle.bmc.events.EventsClient;
import com.oracle.bmc.filestorage.FileStorageAsyncClient;
import com.oracle.bmc.filestorage.FileStorageClient;
import com.oracle.bmc.functions.FunctionsInvokeAsyncClient;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.FunctionsManagementAsyncClient;
import com.oracle.bmc.functions.FunctionsManagementClient;
import com.oracle.bmc.healthchecks.HealthChecksAsyncClient;
import com.oracle.bmc.healthchecks.HealthChecksClient;
import com.oracle.bmc.identity.IdentityAsyncClient;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.integration.IntegrationInstanceAsyncClient;
import com.oracle.bmc.integration.IntegrationInstanceClient;
import com.oracle.bmc.keymanagement.KmsVaultAsyncClient;
import com.oracle.bmc.keymanagement.KmsVaultClient;
import com.oracle.bmc.limits.LimitsAsyncClient;
import com.oracle.bmc.limits.LimitsClient;
import com.oracle.bmc.limits.QuotasAsyncClient;
import com.oracle.bmc.limits.QuotasClient;
import com.oracle.bmc.loadbalancer.LoadBalancerAsyncClient;
import com.oracle.bmc.loadbalancer.LoadBalancerClient;
import com.oracle.bmc.marketplace.MarketplaceAsyncClient;
import com.oracle.bmc.marketplace.MarketplaceClient;
import com.oracle.bmc.monitoring.MonitoringAsyncClient;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.mysql.DbBackupsAsyncClient;
import com.oracle.bmc.mysql.DbBackupsClient;
import com.oracle.bmc.mysql.DbSystemAsyncClient;
import com.oracle.bmc.mysql.DbSystemClient;
import com.oracle.bmc.mysql.MysqlaasAsyncClient;
import com.oracle.bmc.mysql.MysqlaasClient;
import com.oracle.bmc.mysql.WorkRequestsAsyncClient;
import com.oracle.bmc.mysql.WorkRequestsClient;
import com.oracle.bmc.nosql.NosqlAsyncClient;
import com.oracle.bmc.nosql.NosqlClient;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.oce.OceInstanceAsyncClient;
import com.oracle.bmc.oce.OceInstanceClient;
import com.oracle.bmc.ocvp.EsxiHostAsyncClient;
import com.oracle.bmc.ocvp.EsxiHostClient;
import com.oracle.bmc.ocvp.SddcAsyncClient;
import com.oracle.bmc.ocvp.SddcClient;
import com.oracle.bmc.ocvp.WorkRequestAsyncClient;
import com.oracle.bmc.ocvp.WorkRequestClient;
import com.oracle.bmc.oda.OdaAsyncClient;
import com.oracle.bmc.oda.OdaClient;
import com.oracle.bmc.ons.NotificationControlPlaneAsyncClient;
import com.oracle.bmc.ons.NotificationControlPlaneClient;
import com.oracle.bmc.ons.NotificationDataPlaneAsyncClient;
import com.oracle.bmc.ons.NotificationDataPlaneClient;
import com.oracle.bmc.osmanagement.OsManagementAsyncClient;
import com.oracle.bmc.osmanagement.OsManagementClient;
import com.oracle.bmc.resourcemanager.ResourceManagerAsyncClient;
import com.oracle.bmc.resourcemanager.ResourceManagerClient;
import com.oracle.bmc.resourcesearch.ResourceSearchAsyncClient;
import com.oracle.bmc.resourcesearch.ResourceSearchClient;
import com.oracle.bmc.secrets.SecretsAsyncClient;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.streaming.StreamAdminAsyncClient;
import com.oracle.bmc.streaming.StreamAdminClient;
import com.oracle.bmc.usageapi.UsageapiAsyncClient;
import com.oracle.bmc.usageapi.UsageapiClient;
import com.oracle.bmc.vault.VaultsAsyncClient;
import com.oracle.bmc.vault.VaultsClient;
import com.oracle.bmc.waas.RedirectAsyncClient;
import com.oracle.bmc.waas.RedirectClient;
import com.oracle.bmc.waas.WaasAsyncClient;
import com.oracle.bmc.waas.WaasClient;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;

import javax.inject.Singleton;

@SdkClients({
        AnalyticsClient.class,
        AnalyticsAsyncClient.class,
        AnnouncementClient.class,
        AnnouncementAsyncClient.class,
        ApplianceExportJobClient.class,
        ApplianceExportJobAsyncClient.class,
        ApplicationMigrationClient.class,
        ApplicationMigrationAsyncClient.class,
        AuditClient.class,
        AuditAsyncClient.class,
        AutoScalingClient.class,
        AutoScalingAsyncClient.class,
        BdsClient.class,
        BdsAsyncClient.class,
        BlockstorageClient.class,
        BlockstorageAsyncClient.class,
        BudgetClient.class,
        BudgetAsyncClient.class,
        ComputeClient.class,
        ComputeAsyncClient.class,
        ComputeManagementClient.class,
        ComputeManagementAsyncClient.class,
        ContainerEngineClient.class,
        ContainerEngineAsyncClient.class,
        DataCatalogClient.class,
        DataCatalogAsyncClient.class,
        DataFlowClient.class,
        DataFlowAsyncClient.class,
        DataIntegrationClient.class,
        DataIntegrationAsyncClient.class,
        DataSafeClient.class,
        DataScienceAsyncClient.class,
        DataScienceClient.class,
        DatabaseClient.class,
        DatabaseAsyncClient.class,
        DbBackupsClient.class,
        DbBackupsAsyncClient.class,
        DbSystemClient.class,
        DbSystemAsyncClient.class,
        EmailClient.class,
        EmailAsyncClient.class,
        EsxiHostClient.class,
        EsxiHostAsyncClient.class,
        EventsClient.class,
        EventsAsyncClient.class,
        FileStorageClient.class,
        FileStorageAsyncClient.class,
        FunctionsInvokeClient.class,
        FunctionsInvokeAsyncClient.class,
        FunctionsManagementClient.class,
        FunctionsManagementAsyncClient.class,
        GatewayClient.class,
        GatewayAsyncClient.class,
        HealthChecksClient.class,
        HealthChecksAsyncClient.class,
        IdentityClient.class,
        IdentityAsyncClient.class,
        IncidentClient.class,
        IncidentAsyncClient.class,
        IntegrationInstanceClient.class,
        IntegrationInstanceAsyncClient.class,
        KmsVaultAsyncClient.class,
        KmsVaultClient.class,
        LimitsClient.class,
        LimitsAsyncClient.class,
        LoadBalancerClient.class,
        LoadBalancerAsyncClient.class,
        MarketplaceClient.class,
        MarketplaceAsyncClient.class,
        MonitoringClient.class,
        MonitoringAsyncClient.class,
        MysqlaasClient.class,
        MysqlaasAsyncClient.class,
        NosqlClient.class,
        NosqlAsyncClient.class,
        NotificationControlPlaneClient.class,
        NotificationControlPlaneAsyncClient.class,
        NotificationDataPlaneClient.class,
        NotificationDataPlaneAsyncClient.class,
        ObjectStorageClient.class,
        ObjectStorageAsyncClient.class,
        OceInstanceClient.class,
        OceInstanceAsyncClient.class,
        OdaClient.class,
        OdaAsyncClient.class,
        OsManagementClient.class,
        OsManagementAsyncClient.class,
        QuotasClient.class,
        QuotasAsyncClient.class,
        RedirectClient.class,
        RedirectAsyncClient.class,
        ResourceManagerClient.class,
        ResourceManagerAsyncClient.class,
        ResourceSearchClient.class,
        ResourceSearchAsyncClient.class,
        SddcClient.class,
        SddcAsyncClient.class,
        SecretsClient.class,
        SecretsAsyncClient.class,
        ShippingVendorsClient.class,
        ShippingVendorsAsyncClient.class,
        StreamAdminClient.class,
        StreamAdminAsyncClient.class,
        TransferApplianceClient.class,
        TransferApplianceAsyncClient.class,
        TransferApplianceEntitlementClient.class,
        TransferApplianceEntitlementAsyncClient.class,
        TransferDeviceClient.class,
        TransferDeviceAsyncClient.class,
        TransferJobClient.class,
        TransferJobAsyncClient.class,
        TransferPackageClient.class,
        TransferPackageAsyncClient.class,
        UsageapiClient.class,
        UsageapiAsyncClient.class,
        VaultsClient.class,
        VaultsAsyncClient.class,
        VirtualNetworkClient.class,
        VirtualNetworkAsyncClient.class,
        WaasClient.class,
        WaasAsyncClient.class,
        WorkRequestClient.class,
        WorkRequestAsyncClient.class,
        com.oracle.bmc.workrequests.WorkRequestClient.class,
        com.oracle.bmc.workrequests.WorkRequestAsyncClient.class,
        WorkRequestsClient.class,
        WorkRequestsAsyncClient.class,
        com.oracle.bmc.apigateway.WorkRequestsClient.class,
        com.oracle.bmc.apigateway.WorkRequestsAsyncClient.class

})
@Requires(property = "oci.sdk.compiler", value = StringUtils.TRUE)
@Singleton
@Internal
final class SdkAutomaticFeatureMetadata {
}
