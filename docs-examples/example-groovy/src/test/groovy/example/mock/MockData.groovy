package example.mock

import groovy.transform.CompileStatic

@CompileStatic
class MockData {

    public static final List<String> bucketNames = []
    public static String namespace = 'test-namespace'
    public static String tenancyId = 'test-tenancyId'
    public static String bucketLocation = 'test-location'

    static void reset() {
        bucketNames.clear()
        namespace = 'test-namespace'
        tenancyId = 'test-tenancyId'
        bucketLocation = 'test-location'
    }
}
