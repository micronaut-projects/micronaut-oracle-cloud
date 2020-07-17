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
package example;

// tag::imports[]

import com.oracle.bmc.database.model.AutonomousDatabaseSummary;
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.oci.clients.rxjava2.database.DatabaseRxClient;
import io.micronaut.oci.core.TenancyIdProvider;
import io.reactivex.Single;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
// end::imports[]

// tag::class[]
@Controller("/db")
public class DatabaseController implements DatabaseOperations {
    private final DatabaseRxClient dbClient;
    private final TenancyIdProvider tenancyIdProvider;

    public DatabaseController(
            DatabaseRxClient dbClient,
            TenancyIdProvider tenancyIdProvider) { // <1>
        this.dbClient = dbClient;
        this.tenancyIdProvider = tenancyIdProvider;
    }
// end::class[]

    @Override
    @Get("/list{/compartmentId}")
    public Single<List<String>> listDatabases(@PathVariable @Nullable String compartmentId) {
        String compartmentOcId = compartmentId != null ? compartmentId : tenancyIdProvider.getTenancyId();
        ListAutonomousDatabasesRequest listAutonomousDatabasesRequest = ListAutonomousDatabasesRequest.builder()
                .compartmentId(compartmentOcId).build();
        return dbClient.listAutonomousDatabases(listAutonomousDatabasesRequest).map(listAutonomousDatabasesResponse ->
                listAutonomousDatabasesResponse.getItems()
                .stream()
                .map(AutonomousDatabaseSummary::getDbName)
                .collect(Collectors.toList()));
    }
// tag::class[]
}
// end::class[]
