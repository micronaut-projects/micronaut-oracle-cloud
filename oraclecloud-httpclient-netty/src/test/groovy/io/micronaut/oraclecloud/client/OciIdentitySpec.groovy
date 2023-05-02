package io.micronaut.oraclecloud.client

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
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

@Requires(property = "test.compartment.id")
@Requires(bean = ConfigFileAuthenticationDetailsProvider)
@MicronautTest
class OciIdentitySpec extends Specification {

    @Property(name = "test.compartment.id")
    String compartmentId

    @Inject
    @NonNull
    ConfigFileAuthenticationDetailsProvider authenticationDetailsProvider

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

    void "test create and delete compartment"() {
        when:
        var client = buildClient()
        var name = "__micronaut_test_" + new Random().nextInt(0, Integer.MAX_VALUE)
        var description = "Compartment created by test"
        var body = CreateCompartmentDetails.builder().name(name).description(description).compartmentId(compartmentId).build()
        var request = CreateCompartmentRequest.builder().createCompartmentDetails(body).build()
        var response = client.createCompartment(request)

        then:
        var childId = response.compartment.id
        childId != null
        response.compartment.compartmentId == compartmentId

        when:
        request = GetCompartmentRequest.builder().compartmentId(childId).build()
        Thread.sleep(10000)
        response = client.getWaiters().forCompartment(request, Compartment.LifecycleState.Active).execute()

        then:
        response.compartment.name == name
        response.compartment.description == description
        response.compartment.compartmentId == compartmentId
        response.compartment.id == childId
        response.compartment.lifecycleState == Compartment.LifecycleState.Active

        when:
        response = client.deleteCompartment(DeleteCompartmentRequest.builder().compartmentId(childId).build())

        then:
        response != null
    }

    IdentityClient buildClient() {
        return IdentityClient.builder().build(authenticationDetailsProvider)
    }

}
