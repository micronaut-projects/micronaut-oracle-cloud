package example

import com.fnproject.fn.testing.FnTestingRule
import example.mock.MockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListBucketsFunctionTest {

    var fn: FnTestingRule = FnTestingRule.createDefault()

    @Test
    fun testFunction() {
        MockData.bucketNames.clear()
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")

        fn.givenEvent().enqueue()
            .addSharedClass(MockData::class.java)
            .thenRun(ListBucketsFunction::class.java, "handleRequest")

        val body = fn.onlyResult.bodyAsString
        assertEquals("[\"b1\",\"b2\"]", body)
    }
}
