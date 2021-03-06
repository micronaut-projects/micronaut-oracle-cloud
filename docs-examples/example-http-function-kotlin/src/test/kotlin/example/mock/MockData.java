package example.mock;

import java.util.ArrayList;
import java.util.List;

public class MockData {

    public static final List<String> bucketNames = new ArrayList<>();
    public static final List<String> objectNames = new ArrayList<>();
    public static String namespace = "test-namespace";
    public static String tenancyId = "test-tenancyId";
    public static String bucketLocation = "test-location";

    public static void reset() {
        bucketNames.clear();
        objectNames.clear();
        namespace = "test-namespace";
        tenancyId = "test-tenancyId";
        bucketLocation = "test-location";
    }
}
