package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.EmittedMessage;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.staticstate.AsyncMessageTracker;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * What the Kafka replacements share: calling the original, and recording what the SUT received
 * and emitted. kafka-clients is not a dependency of EvoMaster, so records are only read by
 * reflection.
 *
 * Two kinds of calls are never recorded:
 * <ul>
 *     <li>those made on the thread running the driver's executeAsyncApiAction, while it runs, see
 *     {@link ExecutionTracer#isExecutingDriverAction()};</li>
 *     <li>those made from inside another replaced call on the same thread. A producer that wraps
 *     another one, as Spring Kafka's CloseSafeProducer does, sends each record twice through the
 *     {@code Producer} interface, and KafkaProducer.send(record) calls send(record, null) on
 *     itself. Both inner calls are replaced as well, since library code is instrumented too, and
 *     would otherwise record the same message again. The price: a send that the code of a
 *     callback or of a custom producer makes, on the same thread, from inside another send, is
 *     not recorded either.</li>
 * </ul>
 */
final class KafkaReplacementHelper {

    static final String CONSUMER = "org.apache.kafka.clients.consumer.Consumer";

    static final String KAFKA_CONSUMER = "org.apache.kafka.clients.consumer.KafkaConsumer";

    static final String CONSUMER_RECORDS = "org.apache.kafka.clients.consumer.ConsumerRecords";

    static final String PRODUCER = "org.apache.kafka.clients.producer.Producer";

    static final String KAFKA_PRODUCER = "org.apache.kafka.clients.producer.KafkaProducer";

    static final String PRODUCER_RECORD = "org.apache.kafka.clients.producer.ProducerRecord";

    static final String CALLBACK = "org.apache.kafka.clients.producer.Callback";

    /**
     * The id of the replacement of poll, in every consumer replacement class.
     */
    static final String POLL = "poll";

    /**
     * The id of the replacement of send(record), in every producer replacement class.
     */
    static final String SEND = "send";

    /**
     * The id of the replacement of send(record, callback), in every producer replacement class.
     */
    static final String SEND_WITH_CALLBACK = "sendWithCallback";

    /*
        Accessors of ConsumerRecord and ProducerRecord
     */
    private static final String TOPIC = "topic";
    private static final String VALUE = "value";

    /**
     * Most bytes a code point takes in UTF-8. Decoding this many bytes per char, plus one code
     * point, gives more than {@link EmittedMessage#MAX_TEXT_LENGTH} chars for any longer payload,
     * so EmittedMessage can tell it was cut.
     */
    private static final int UTF8_MAX_BYTES_PER_CODE_POINT = 4;

    /**
     * How many bytes of a byte[] payload are decoded at most.
     */
    private static final int MAX_DECODED_BYTES = UTF8_MAX_BYTES_PER_CODE_POINT * (EmittedMessage.MAX_TEXT_LENGTH + 1);

    /**
     * Class of a record -> accessor name -> the public no-input method with that name. A
     * ClassValue, so a cached class does not keep its class loader alive.
     */
    private static final ClassValue<Map<String, Method>> accessors = new ClassValue<Map<String, Method>>() {
        @Override
        protected Map<String, Method> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * How deep the current thread is in replaced poll calls.
     */
    private static final ThreadLocal<int[]> pollDepth = ThreadLocal.withInitial(() -> new int[1]);

    /**
     * How deep the current thread is in replaced send calls. A send made while this is above zero
     * is inside another send on the same thread, and is not recorded.
     */
    private static final ThreadLocal<int[]> sendDepth = ThreadLocal.withInitial(() -> new int[1]);

    private KafkaReplacementHelper() {
    }

    /**
     * @return whether a poll with this timeout waits for records to arrive
     */
    static boolean waits(Duration timeout) {
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }

    /**
     * @return whether a poll with this timeout waits for records to arrive
     */
    static boolean waits(long timeoutMs) {
        return timeoutMs > 0;
    }

    /**
     * Call the original poll, and record what it returned as received by the current thread.
     *
     * @param waits whether the call waits for records, see {@link AsyncMessageTracker#askingForMore(boolean)}
     */
    static Object poll(ThirdPartyMethodReplacementClass singleton, Object consumer, boolean waits, Object... args) {

        Method original = ThirdPartyMethodReplacementClass.getOriginal(singleton, POLL, consumer);

        int[] depth = pollDepth.get();
        if (depth[0] > 0 || ExecutionTracer.isExecutingDriverAction()) {
            return invoke(original, consumer, args);
        }

        depth[0]++;
        try {
            //before the call, which can block for the whole timeout

            AsyncMessageTracker.askingForMore(waits);
            Object records = invoke(original, consumer, args);
            recordReceived(records);
            return records;
        } finally {
            depth[0]--;
        }
    }

    /**
     * Call the original send, and record the record as emitted if the call returned. A future that
     * then fails still counts: the SUT did emit it.
     */
    static Future<?> send(ThirdPartyMethodReplacementClass singleton, String id, Object producer, Object record, Object... args) {

        Method original = ThirdPartyMethodReplacementClass.getOriginal(singleton, id, producer);

        Object[] allArgs = new Object[args.length + 1];
        allArgs[0] = record;
        System.arraycopy(args, 0, allArgs, 1, args.length);

        int[] depth = sendDepth.get();
        if (depth[0] > 0 || ExecutionTracer.isExecutingDriverAction()) {
            return (Future<?>) invoke(original, producer, allArgs);
        }

        depth[0]++;
        try {
            Future<?> result = (Future<?>) invoke(original, producer, allArgs);
            recordEmitted(record);
            return result;
        } finally {
            depth[0]--;
        }
    }

    private static Object invoke(Method original, Object target, Object... args) {
        try {
            return original.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            //neither poll nor send declares a checked exception
            throw new RuntimeException(cause);
        }
    }

    private static void recordReceived(Object records) {
        if (!(records instanceof Iterable)) {
            return;
        }
        try {
            List<String> received = new ArrayList<>();
            for (Object record : (Iterable<?>) records) {
                String topic = (String) call(record, TOPIC);
                if (topic == null) {
                    continue;
                }
                received.add(topic);
            }
            AsyncMessageTracker.received(received);
        } catch (Exception e) {
            //what is recorded is extra information, never a reason to break the SUT
            SimpleLogger.uniqueWarn("Failed to read the records a Kafka consumer returned: " + e);
        }
    }

    private static void recordEmitted(Object record) {
        try {
            String topic = (String) call(record, TOPIC);
            Object value = call(record, VALUE);

            ExecutionTracer.addEmittedMessage(new EmittedMessage(
                    topic,
                    asText(value),
                    value == null ? null : value.getClass().getName()));
        } catch (Exception e) {
            SimpleLogger.uniqueWarn("Failed to read a record given to a Kafka producer: " + e);
        }
    }

    /**
     * @return the payload as text: a String as given, or the first bytes read as UTF-8, enough
     * for {@link EmittedMessage} to keep and to tell it was cut. Matches the wire only with
     * StringSerializer or ByteArraySerializer (spring-kafka's JsonSerializer quotes a String).
     * Null for any other object: its wire form is up to the serializer, and computing a text form
     * could run SUT code from inside the replacement.
     */
    static String asText(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            return new String(bytes, 0, Math.min(bytes.length, MAX_DECODED_BYTES), StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Call a public method with no inputs, made accessible as the class of the object might not
     * be public.
     */
    private static Object call(Object target, String methodName) throws ReflectiveOperationException {
        Map<String, Method> methods = accessors.get(target.getClass());
        Method m = methods.get(methodName);
        if (m == null) {
            m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            methods.put(methodName, m);
        }
        return m.invoke(target);
    }
}
