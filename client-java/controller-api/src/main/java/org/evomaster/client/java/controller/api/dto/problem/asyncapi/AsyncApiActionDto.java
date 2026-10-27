package org.evomaster.client.java.controller.api.dto.problem.asyncapi;

import org.evomaster.client.java.controller.api.dto.SutInfoDto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One message for the driver to publish, and what to do about a reply.
 *
 * Everything here is decided by the core: which operation, where it goes, what it says. The
 * driver's job is to put it on the wire and, when a reply is expected, wait for the one that
 * answers it.
 */
public class AsyncApiActionDto {

    /**
     * The two places a correlation id can travel, as the document declares them.
     */
    public enum CorrelationLocation {
        HEADER,
        PAYLOAD
    }

    /**
     * How the controller decides that the service is done with a message, and the action can
     * be closed, within the {@link #settleMs} window.
     */
    public enum MessageCompletionStrategy {

        /**
         * Wait for the whole window.
         */
        FIXED,

        /**
         * Stop early once the SUT has gone {@link #stableMs} without reaching any coverage
         * objective for the first time, or improving one. A heuristic: a handler that only runs
         * code already covered earlier in the same test leaves coverage stable too.
         */
        COVERAGE_STABLE,

        /**
         * Stop early once the SUT has received at least one message from {@link #address}, and
         * every thread that received one has gone back to its client for more, ie its handler
         * returned. Only for broker clients the instrumentation tracks. A heuristic: work handed
         * off to another thread looks done as soon as it is handed off.
         */
        HANDLER_RETURN,

        /**
         * Stop early once the driver says that the SUT's consumers have caught up with every
         * message published so far, eg on Kafka, their committed offsets reached the end of the
         * partitions. A driver that cannot tell makes this {@link #FIXED}. A consumer that
         * commits before it is done with a message looks caught up too early.
         */
        CONSUMER_LAG
    }

    /**
     * Key of the operation in the AsyncAPI document, so a driver can log in terms the user will
     * recognise from their own contract.
     */
    public String operationId;

    /**
     * Key of the channel the message is published on.
     */
    public String channelName;

    /**
     * Where the message actually goes on the wire: a topic, a queue, a routing key. Already
     * resolved by the core, including any binding that overrides the channel's address.
     */
    public String address;

    /**
     * Id of the message being published, as the document names it.
     */
    public String messageId;

    /**
     * The message body, serialised. Its content type is in {@link #contentType}.
     */
    public String payload;

    /**
     * What the document declares the payload is encoded as, eg "application/json".
     */
    public String contentType;

    /**
     * Headers to publish alongside the body, for a transport that has them.
     * Key is the header name as the document declares it, value is what to send under it, as text.
     */
    public Map<String, String> headers = new LinkedHashMap<>();

    /**
     * The value to stamp into this message so a reply can be recognised as answering it. Minted
     * fresh for every execution rather than varied by the search, since pairing needs a value
     * unique to the execution and the service only echoes it back.
     */
    public String correlationId;

    /**
     * Where the correlation id has to be written, as the document declares it. Null when the
     * document says nothing, in which case it is up to the driver to decide -- a transport with
     * native correlation should use it.
     */
    public CorrelationLocation correlationLocation;

    /**
     * JSON Pointer to the field the correlation id goes in, within whatever
     * {@link #correlationLocation} names. Null when there is no declared location.
     */
    public String correlationPointer;

    /**
     * Where a reply is expected to arrive, when the operation declares one. Null for a
     * fire-and-forget operation, in which case the driver publishes and returns.
     */
    public String replyAddress;

    /**
     * How long to wait for a reply before giving up, in milliseconds. Set generously, since a
     * slow service and a stuck one look alike from outside, and reported back with the result.
     * Null when no reply is expected.
     */
    public Long replyTimeoutMs;

    /**
     * How long to keep the action open once the driver has returned, in milliseconds. The SUT
     * handles a message on a thread of its own, possibly after the driver saw the publish
     * acknowledged or the reply arrive, and what it covers meanwhile must be credited to this
     * action, not to the next one or to none.
     *
     * The wait is done by the controller, so drivers need not implement it. Null, zero or
     * negative mean no wait. Only set when the SUT is instrumented.
     */
    public Long settleMs;

    /**
     * How the controller decides, within {@link #settleMs}, that the action can be closed. Null
     * is read as {@link MessageCompletionStrategy#FIXED}. Meaningless without a {@link #settleMs}.
     */
    public MessageCompletionStrategy completion;

    /**
     * For {@link MessageCompletionStrategy#COVERAGE_STABLE}: how long, in milliseconds, the SUT has to go
     * without improving any coverage objective to end the wait early. Null, zero or negative
     * make it a {@link MessageCompletionStrategy#FIXED} wait. Null for any other completion.
     */
    public Long stableMs;

    /**
     * The language to render {@link AsyncApiReplyDto#testScript} in. Only Java and Kotlin are
     * ever asked for.
     *
     * Null when no script is wanted, ie the core is not generating tests.
     */
    public SutInfoDto.OutputFormat outputFormat;

    /**
     * The name the rendered lines must leave the reply payload in, as text, when a reply is
     * expected. Named by the core so two actions in one test cannot collide, and set whenever
     * tests are being generated, whether or not a script was asked for. Null when they are not.
     */
    public String replyVariable;
}
