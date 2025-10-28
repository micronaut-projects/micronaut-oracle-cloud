package example.mock

object MockData {

    @JvmField
    val bucketNames: MutableList<String?> = ArrayList()

    @JvmField
    var namespace: String = "test-namespace"

    @JvmField
    var tenancyId: String = "test-tenancyId"
}
