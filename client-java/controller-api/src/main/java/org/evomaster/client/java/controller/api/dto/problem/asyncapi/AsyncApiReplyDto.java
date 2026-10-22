package org.evomaster.client.java.controller.api.dto.problem.asyncapi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What came of publishing one message.
 *
 * These are reported separately rather than as one outcome because they mean different things:
 * a message that could not be published is a broken setup, one published with nothing expected
 * back did all it could, and a promised reply that never came is the only one of the three that
 * says anything about the service.
 */
public class AsyncApiReplyDto {

    /**
     * The index of the action this answers, echoing what was asked.
     */
    public Integer index;

    /**
     * Whether the message reached the broker. False means the driver could not publish, and
     * {@link #errorMessage} says why. Null is read as false.
     */
    public Boolean published;

    /**
     * Whether a reply arrived and was recognised as answering this message. Null is read as no
     * reply having arrived.
     */
    public Boolean replyReceived;

    /**
     * Whether the driver waited for a reply at all. False for a fire-and-forget operation, so
     * that having none is not mistaken for silence in answer to a promise. Null is read as not
     * having waited.
     */
    public Boolean replyExpected;

    /**
     * The reply body, as it arrived.
     */
    public String replyPayload;

    /**
     * The reply's headers, for a transport that has them.
     * Key is the header name, value is what arrived under it, as text.
     */
    public Map<String, String> replyHeaders = new LinkedHashMap<>();

    /**
     * Whether the reply carried back the correlation id stamped on the request. One that arrives
     * without it is recorded rather than failed: from outside, a service correlating by a key of
     * its own looks the same as one that lost the id.
     *
     * Null means the driver does not track correlation, which is not the same as having checked
     * and found it missing.
     */
    public Boolean correlationMatched;

    /**
     * How long the driver waited, in milliseconds, whether or not anything arrived: a verdict on
     * silence means little without it. Null when it did not wait at all, because no reply was
     * expected or nothing was published.
     */
    public Long waitedMs;

    /**
     * Why publishing failed, when it did.
     */
    public String errorMessage;

    /**
     * The publish and await this action just did, rendered as source lines for a generated test,
     * in the language {@link AsyncApiActionDto#outputFormat} asked for. Null when none was asked
     * for, or when the driver renders none.
     *
     * A generated test reaches the broker itself rather than through the driver, and only the
     * driver knows the transport, so it writes these lines and the core pastes them unread.
     * Where a reply is expected they must leave it in {@link AsyncApiActionDto#replyVariable},
     * which the assertions the core appends are written against.
     */
    public List<String> testScript;
}
