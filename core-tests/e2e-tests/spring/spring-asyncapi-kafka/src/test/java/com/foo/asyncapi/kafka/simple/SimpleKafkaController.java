package com.foo.asyncapi.kafka.simple;

import com.foo.asyncapi.kafka.KafkaController;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.List;

import static com.foo.asyncapi.kafka.simple.SimpleRequestConsumer.*;

/**
 * The driver for {@link SimpleKafkaApplication}, probed at start-up with a request on foo.
 */
public class SimpleKafkaController extends KafkaController {

    public SimpleKafkaController() {
        super(SimpleKafkaApplication.class, "/asyncapi/simple-kafka.yaml");
    }

    /** Enable or suppress replies in the service, after its readiness probe has completed. */
    public void setRepliesEnabled(boolean enabled) {
        ctx.getBean(SimpleRequestConsumer.class).setRepliesEnabled(enabled);
    }

    @Override
    protected List<String> getTopics() {
        return Arrays.asList(FOO_REQUEST, FOO_REPLY, BAR_REQUEST, BAR_REPLY);
    }

    @Override
    protected ProducerRecord<String, String> getProbeRequest() {
        return new ProducerRecord<>(FOO_REQUEST, "{\"x\":0}");
    }

    @Override
    protected String getProbeReplyTopic() {
        return FOO_REPLY;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.asyncapi.kafka.simple.";
    }
}
