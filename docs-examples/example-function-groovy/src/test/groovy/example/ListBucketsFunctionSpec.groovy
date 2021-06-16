package example;

import com.fnproject.fn.testing.FnTestingRule
import example.mock.MockData
import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ jvm.java8 }) // FDK requires Java 11+
class ListBucketsFunctionSpec extends Specification {

    void 'test function'() {
        given:
        MockData.bucketNames << 'b1' << 'b2'

        FnTestingRule fn = FnTestingRule.createDefault()

        when: 'the function is invoked'
        fn.givenEvent().enqueue()
        fn.addSharedClass MockData
        fn.thenRun ListBucketsFunction, 'handleRequest'

        then: 'the expected result is returned'
        fn.onlyResult.bodyAsString == '["b1","b2"]'
    }

    void cleanup() {
        MockData.reset()
    }
}
