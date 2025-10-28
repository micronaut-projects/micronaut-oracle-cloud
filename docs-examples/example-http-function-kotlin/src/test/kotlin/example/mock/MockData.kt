package example.mock

object MockData {

    @JvmField
    val bucketNames: MutableList<String?> = ArrayList()

    @JvmField
    val objectNames: MutableList<String?> = ArrayList()

    @JvmField
    var namespace: String = "test-namespace"

    @JvmField
    var tenancyId: String = "test-tenancyId"

    @JvmField
    var bucketLocation: String = "test-location"

    @JvmStatic
    fun reset() {
        bucketNames.clear()
        objectNames.clear()
        namespace = "test-namespace"
        tenancyId = "test-tenancyId"
        bucketLocation = "test-location"
    }
}
