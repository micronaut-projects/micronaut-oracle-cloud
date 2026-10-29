package example;

import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProcessorMetricsTest {

    @Test
    void processCpuTimeCanBeRead() {
        var registry = new SimpleMeterRegistry();
        try {
            new ProcessorMetrics().bindTo(registry);
            var processCpuTime = registry.get("process.cpu.time").functionCounter();
            assertDoesNotThrow(processCpuTime::count);
        } finally {
            registry.close();
        }
    }
}
