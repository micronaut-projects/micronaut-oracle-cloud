package io.micronaut.oraclecloud.client

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.model.CreateCompartmentDetails
import com.oracle.bmc.identity.requests.CreateCompartmentRequest
import com.oracle.bmc.identity.requests.DeleteCompartmentRequest
import com.oracle.bmc.identity.requests.GetCompartmentRequest
import com.oracle.bmc.identity.requests.ListCompartmentsRequest
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static com.oracle.bmc.identity.requests.ListCompartmentsRequest.AccessLevel.Accessible

@Requires(property = "vault.secrets.compartment.ocid")
@Requires(bean = AuthenticationDetailsProvider)
@MicronautTest
class OciIdentitySpec extends Specification {

    @Property(name = "vault.secrets.compartment.ocid")
    String compartmentId

    @Inject
    @NonNull
    AuthenticationDetailsProvider authenticationDetailsProvider

    void "test get compartment"() {
        when:
        var client = buildClient()
        var request = GetCompartmentRequest.builder().compartmentId(compartmentId).build()
        var response = client.getCompartment(request)

        then:
        response.compartment.id == compartmentId
        !response.compartment.name.empty
    }

    void "test list compartments"() {
        when:
        var client = buildClient()
        var response = client.getCompartment(GetCompartmentRequest.builder().compartmentId(compartmentId).build())

        then:
        var parentId = response.compartment.compartmentId
        parentId != null

        when:
        var request = ListCompartmentsRequest.builder()
                .limit(100)
                .accessLevel(Accessible)
                .compartmentId(parentId)
                .build()
        response = client.listCompartments(request)

        then:
        response.items.size() > 0
        response.items[0].compartmentId == parentId
        !response.items[0].name.empty
        response.items.any{ c -> c.id == compartmentId }
        response.items.every{ c -> !c.name.empty }
    }

    IdentityClient buildClient() {
        return IdentityClient.builder().build(authenticationDetailsProvider)
    }

}
