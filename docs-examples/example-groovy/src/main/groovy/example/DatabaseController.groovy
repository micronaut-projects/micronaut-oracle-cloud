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
package example

// tag::imports[]
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.oraclecloud.clients.rxjava2.database.DatabaseRxClient
import io.micronaut.oraclecloud.core.TenancyIdProvider
import io.reactivex.Single
// end::imports[]

// tag::class[]
@CompileStatic
@Controller('/db')
class DatabaseController implements DatabaseOperations {

    private final DatabaseRxClient dbClient
    private final TenancyIdProvider tenancyIdProvider

    DatabaseController(DatabaseRxClient dbClient,
                       TenancyIdProvider tenancyIdProvider) { // <1>
        this.dbClient = dbClient
        this.tenancyIdProvider = tenancyIdProvider
    }
// end::class[]

    @Override
    @Get('/list{/compartmentId}')
    Single<List<String>> listDatabases(@PathVariable @Nullable String compartmentId) {
        String compartmentOcId = compartmentId ?: tenancyIdProvider.tenancyId
        ListAutonomousDatabasesRequest listAutonomousDatabasesRequest = ListAutonomousDatabasesRequest.builder()
                .compartmentId(compartmentOcId).build()
        return dbClient.listAutonomousDatabases(listAutonomousDatabasesRequest).map(listAutonomousDatabasesResponse ->
                        listAutonomousDatabasesResponse.items*.dbName)
    }
// tag::class[]
}
// end::class[]
