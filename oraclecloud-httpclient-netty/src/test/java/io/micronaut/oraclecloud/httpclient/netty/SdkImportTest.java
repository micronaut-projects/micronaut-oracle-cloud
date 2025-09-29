package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.disasterrecovery.DisasterRecoveryClient;
import com.oracle.bmc.disasterrecovery.model.CreateDrPlanDetails;
import com.oracle.bmc.disasterrecovery.model.DrPlan;
import com.oracle.bmc.disasterrecovery.model.DrPlanType;
import com.oracle.bmc.disasterrecovery.requests.CreateDrPlanRequest;
import com.oracle.bmc.disasterrecovery.responses.CreateDrPlanResponse;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.oraclecloud.core.sdk.SdkImport;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SdkImport(DisasterRecoveryClient.class)
@MicronautTest
@Property(name = "oci.config.path", value = "") // Disable reading config file
public class SdkImportTest {

    @Inject
    DisasterRecoveryClient disasterRecoveryClient;

    @Test
    void testImport() {
        Assertions.assertNotNull(disasterRecoveryClient);
        CreateDrPlanResponse drPlan = disasterRecoveryClient.createDrPlan(
            CreateDrPlanRequest.builder()
                .body$(CreateDrPlanDetails.builder()
                    .type(DrPlanType.Failover)
                    .drProtectionGroupId("mygroup")
                    .sourcePlanId("myplan")
                    .build())
                .build()
        );
        DrPlan dp = drPlan.getDrPlan();
        Assertions.assertNotNull(dp);
        Assertions.assertEquals(DrPlanType.Failover, drPlan.getDrPlan().getType());
    }

    @MockBean(RegionProvider.class)
    RegionProvider mockRegionProvider() {
        return () -> Region.AF_JOHANNESBURG_1;
    }

    @MockBean
    BeanCreatedEventListener<DisasterRecoveryClient.Builder> setEndpoint(EmbeddedServer embeddedServer) {
        return event -> event.getBean().endpoint("http://localhost:" + embeddedServer.getPort());
    }

    @MockBean
    @Controller
    static class TestController {
        @Post("/20220125/drPlans")
        HttpResponse<DrPlan> createDrPlan(@Body CreateDrPlanDetails createDrPlanRequest) {
            return HttpResponse.created(DrPlan.builder()
                    .sourcePlanId(createDrPlanRequest.getSourcePlanId())
                    .drProtectionGroupId(createDrPlanRequest.getDrProtectionGroupId())
                    .type(createDrPlanRequest.getType())
                .build());
        }
    }
}
