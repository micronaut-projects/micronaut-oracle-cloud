/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.http.server.tck.oraclecloud.function;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SelectPackages({
    "io.micronaut.http.server.tck.tests"
})
@SuiteDisplayName("HTTP Server TCK for Oracle Cloud Function")
@ExcludeClassNamePatterns({
    // 18 (10%) tests of 188 fail currently
    "io.micronaut.http.server.tck.tests.hateoas.JsonErrorTest", // Client cannot parse the JsonError type - getBody(VndError.class) returns empty optional
    "io.micronaut.http.server.tck.tests.hateoas.VndErrorTest", // Client cannot parse the VndError type - getBody(VndError.class) returns empty optional
    "io.micronaut.http.server.tck.tests.hateoas.JsonErrorSerdeTest", // Client cannot parse the JsonError type - getBody(VndError.class) returns empty optional
    "io.micronaut.http.server.tck.tests.LocalErrorReadingBodyTest", // https://github.com/micronaut-projects/micronaut-oracle-cloud/issues/921
    "io.micronaut.http.server.tck.tests.VersionTest",
    "io.micronaut.http.server.tck.tests.filter.RequestFilterTest",
    "io.micronaut.http.server.tck.tests.BodyTest",
    "io.micronaut.http.server.tck.tests.CookiesTest",
    "io.micronaut.http.server.tck.tests.MissingBodyAnnotationTest",
    "io.micronaut.http.server.tck.tests.FilterProxyTest",
    "io.micronaut.http.server.tck.tests.HeadersTest",
    "io.micronaut.http.server.tck.tests.constraintshandler.ControllerConstraintHandlerTest"
})
public class OracleCloudFunctionServerTestSuite {
}
