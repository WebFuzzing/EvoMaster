package com.foo.asyncapi.ncs;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NcsKafkaControllerTest {

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void testPositioningFailureClosesConsumer() {
        IllegalStateException failure = new IllegalStateException("metadata unavailable");
        MockConsumer<String, String> consumer = new MockConsumer<String, String>(OffsetResetStrategy.LATEST) {
            @Override
            public List<PartitionInfo> partitionsFor(String topic) {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> NcsKafkaController.positionAtEnd(consumer, "reply")));
        assertTrue(consumer.closed());
    }

    @Test
    void testAbsentOrNullCorrelationIsUnknown() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("reply", 0, 0, null, "{}");
        AsyncApiActionDto dto = new AsyncApiActionDto();
        assertNull(NcsKafkaController.correlationOf(record, dto));
        record.headers().add("correlationId", null);
        assertNull(NcsKafkaController.correlationOf(record, dto));
        record.headers().add("correlationId", "id".getBytes(StandardCharsets.UTF_8));
        assertEquals("id", NcsKafkaController.correlationOf(record, dto));
    }

    @Test
    void testUnsupportedCorrelationRejectedBeforePublishing() {
        try (MockProducer<String, String> producer = new MockProducer<>(true, new StringSerializer(), new StringSerializer())) {
            NcsKafkaController controller = new NcsKafkaController(producer);
            AsyncApiActionDto dto = new AsyncApiActionDto();
            dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.PAYLOAD;
            assertThrows(IllegalArgumentException.class,
                    () -> controller.executeAsyncApiAction(dto, new AsyncApiReplyDto()));
            assertTrue(producer.history().isEmpty());
            dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.HEADER;
            dto.correlationPointer = "/otherHeader";
            assertThrows(IllegalArgumentException.class,
                    () -> controller.executeAsyncApiAction(dto, new AsyncApiReplyDto()));
        }
    }

    @Test
    void testInterruptedPublishPreservesInterrupt() {
        try (MockProducer<String, String> producer = new MockProducer<>(false, new StringSerializer(), new StringSerializer())) {
            NcsKafkaController controller = new NcsKafkaController(producer);
            AsyncApiActionDto dto = new AsyncApiActionDto();
            dto.address = "request";
            dto.payload = "{}";
            dto.correlationId = "id";
            AsyncApiReplyDto reply = new AsyncApiReplyDto();
            Thread.currentThread().interrupt();
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> controller.executeAsyncApiAction(dto, reply));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            assertNotEquals(Boolean.TRUE, reply.published);
        }
    }
}
