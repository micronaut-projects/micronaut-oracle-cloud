package io.micronaut.oraclecloud.client

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.CommitMultipartUploadDetails
import com.oracle.bmc.objectstorage.model.CommitMultipartUploadPartDetails
import com.oracle.bmc.objectstorage.model.CreateBucketDetails
import com.oracle.bmc.objectstorage.model.CreateMultipartUploadDetails
import com.oracle.bmc.objectstorage.requests.*
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Requires(property = "test.compartment.id")
@Requires(bean = ConfigFileAuthenticationDetailsProvider)
@MicronautTest
@Stepwise
class OciObjectStorageSerdeSpec extends Specification {

    @Shared
    @Property(name = "test.compartment.id")
    String compartmentId

    @Shared
    @Inject
    @NonNull
    ConfigFileAuthenticationDetailsProvider authenticationDetailsProvider

    @Shared
    String namespace

    @Shared
    String bucketName

    @Shared
    ObjectStorageClient client

    static final String content = "content"
    static final String objectName = "micronaut/test/file.txt"

    @spock.lang.Requires({ instance.compartmentId && instance.authenticationDetailsProvider })
    void "test get namespace"() {
        given:
        client = buildClient()

        var getNamespaceRequest = GetNamespaceRequest.builder().compartmentId(compartmentId).build()

        when:
        namespace = client.getNamespace(getNamespaceRequest).value

        then:
        namespace != null
        !namespace.isBlank()
    }

    void "test create bucket"() {
        given:
        bucketName = "micronaut_test_bucket_" + new Random().nextInt(0, Integer.MAX_VALUE)

        var body = CreateBucketDetails.builder()
            .compartmentId(compartmentId)
            .name(bucketName)
            .publicAccessType(CreateBucketDetails.PublicAccessType.NoPublicAccess)
            .versioning(CreateBucketDetails.Versioning.Disabled)
            .build()

        when:
        var request = CreateBucketRequest.builder().createBucketDetails(body).namespaceName(namespace).build()
        var response = client.createBucket(request)

        then:
        response.bucket.compartmentId == compartmentId
        response.bucket.name == bucketName
        response.bucket.isReadOnly == false
        response.bucket.id != null
    }

    void "test get bucket"() {
        when:
        var request = GetBucketRequest.builder().namespaceName(namespace).bucketName(bucketName).build()
        var response = client.getBucket(request)

        then:
        response.bucket.compartmentId == compartmentId
        response.bucket.name == bucketName
        response.bucket.isReadOnly == false
        response.bucket.id != null
    }

    void "test create object"() {
        when:
        var body = CreateMultipartUploadDetails.builder()
                .object(objectName)
                .contentType("text/plain")
                .build()
        var request = CreateMultipartUploadRequest.builder()
                .bucketName(bucketName).namespaceName(namespace)
                .createMultipartUploadDetails(body)
                .build()
        var response = client.createMultipartUpload(request)

        then:
        response.__httpStatusCode__ < 300
        var uploadId = response.multipartUpload.uploadId

        when:
        var uploadRequest = UploadPartRequest.builder()
            .bucketName(bucketName).namespaceName(namespace)
            .uploadId(uploadId)
            .uploadPartNum(1)
            .contentLength((long) content.length())
            .objectName(objectName)
            .uploadPartBody(new ByteArrayInputStream(content.getBytes()))
            .build()
        var uploadResponse = client.uploadPart(uploadRequest)

        then:
        uploadResponse.__httpStatusCode__ < 300
        var etag = uploadResponse.ETag

        when:
        var commitBody = CommitMultipartUploadDetails.builder()
                .partsToCommit([CommitMultipartUploadPartDetails.builder().etag(etag).partNum(1).build()])
                .build()
        var commitRequest = CommitMultipartUploadRequest.builder()
            .bucketName(bucketName).namespaceName(namespace)
            .uploadId(uploadId)
            .objectName(objectName)
            .commitMultipartUploadDetails(commitBody)
            .build()
        var commitResponse = client.commitMultipartUpload(commitRequest)

        then:
        commitResponse.__httpStatusCode__ < 300
    }

    void "test object details"() {
        when:
        var request = GetObjectRequest.builder()
                .bucketName(bucketName).namespaceName(namespace)
                .objectName(objectName)
                .build()
        var response = client.getObject(request)

        then:
        response.__httpStatusCode__ < 300
        response.contentLength == (long) content.length()
    }

    void "test delete object"() {
        when:
        var request = DeleteObjectRequest.builder()
            .bucketName(bucketName).namespaceName(namespace)
            .objectName(objectName)
            .build()
        var response = client.deleteObject(request)

        then:
        response.__httpStatusCode__ < 300
    }

    void "test delete bucket"() {
        when:
        Thread.sleep(1000)
        var request = DeleteBucketRequest.builder().namespaceName(namespace).bucketName(bucketName).build()
        var response = client.deleteBucket(request)

        then:
        response.__httpStatusCode__ <= 300
    }

    ObjectStorageClient buildClient() {
        return ObjectStorageClient.builder().build(authenticationDetailsProvider)
    }

}
