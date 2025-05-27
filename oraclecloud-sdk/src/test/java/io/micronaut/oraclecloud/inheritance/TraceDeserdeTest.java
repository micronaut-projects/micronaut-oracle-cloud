package io.micronaut.oraclecloud.inheritance;

import com.oracle.bmc.generativeaiagentruntime.model.ChatResult;
import com.oracle.bmc.generativeaiagentruntime.model.GenerationTrace;
import com.oracle.bmc.generativeaiagentruntime.model.Message;
import com.oracle.bmc.generativeaiagentruntime.model.MessageContent;
import com.oracle.bmc.generativeaiagentruntime.model.Trace;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest(startApplication = false)
public class TraceDeserdeTest {

    @Test
    void testDeserializeInheritance(JsonMapper jsonMapper) throws IOException {
        ChatResult.Builder message = ChatResult.builder()
            .traces(List.of(GenerationTrace.builder().generation("test").build()))
            .message(Message.builder().content(MessageContent.builder().text("some text").build()).build());
        String value = jsonMapper.writeValueAsString(message.build());

        assertNotNull(value);
        assertEquals("{\"message\":{\"content\":{\"text\":\"some text\"}},\"traces\":[{\"traceType\":\"GENERATION_TRACE\",\"generation\":\"test\"}]}", value);

        ChatResult chatResult = jsonMapper.readValue("{\"message\":{\"content\":{\"text\":\"some text\"}},\"traces\":[{\"traceType\":\"GENERATION_TRACE\",\"generation\":\"test\"}, {}]}", ChatResult.class);
        assertNotNull(chatResult);
        List<Trace> traces = chatResult.getTraces();
        assertNotNull(traces);
        assertEquals(2, traces.size());
        Trace trace = traces.get(0);
        assertNotNull(trace);
        assertInstanceOf(GenerationTrace.class, trace);
    }

}
