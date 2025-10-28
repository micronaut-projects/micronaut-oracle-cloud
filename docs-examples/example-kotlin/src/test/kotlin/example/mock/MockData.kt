package example.mock

object MockData {
    @JvmField
    val bucketNames: MutableList<String?> = ArrayList()
    @JvmField
    var namespace: String = "test-namespace"
    @JvmField
    var tenancyId: String = "test-tenancyId"
    @JvmField
    var bucketLocation: String = "test-location"

    fun reset() {
        bucketNames.clear()
        namespace = "test-namespace"
        tenancyId = "test-tenancyId"
        bucketLocation = "test-location"
    }
}
