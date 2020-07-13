package io.micronaut.oci.objectstorage;

import com.oracle.bmc.apigateway.GatewayAsyncClient;
import com.oracle.bmc.apigateway.GatewayClient;
import com.oracle.bmc.bds.BdsAsyncClient;
import com.oracle.bmc.bds.BdsClient;
import com.oracle.bmc.budget.BudgetAsyncClient;
import com.oracle.bmc.budget.BudgetClient;
import com.oracle.bmc.filestorage.FileStorageAsyncClient;
import com.oracle.bmc.filestorage.FileStorageClient;
import com.oracle.bmc.functions.FunctionsInvokeAsync;
import com.oracle.bmc.functions.FunctionsInvokeAsyncClient;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// test that each SDK client is setup and injectable
@MicronautTest
public class SdkTest {

    @Test
    void testFunctionClient(FunctionsInvokeClient client, FunctionsInvokeAsyncClient asyncClient) {
        assertNotNull(client);
        assertNotNull(asyncClient);
    }

    @Test
    void testBdsClient(BdsClient client, BdsAsyncClient asyncClient) {
        assertNotNull(client);
        assertNotNull(asyncClient);
    }

    @Test
    void testBudgetClient(BudgetClient client, BudgetAsyncClient asyncClient) {
        assertNotNull(client);
        assertNotNull(asyncClient);
    }

    @Test
    void testFileStorageClient(FileStorageClient client, FileStorageAsyncClient asyncClient) {
        assertNotNull(client);
        assertNotNull(asyncClient);
    }

    @Test
    void testApiGatewayClient(GatewayClient client, GatewayAsyncClient asyncClient) {
        assertNotNull(client);
        assertNotNull(asyncClient);
    }
}
