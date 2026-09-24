package org.evomaster.client.java.controller.api.dto.problem.asyncapi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What came of publishing one message.
 *
 * The four outcomes are deliberately distinguished, because they mean different things and only
 * one of them is a fault:
 *
 * <ul>
 *   <li>published, with no reply expected -- a fire-and-forget operation did what it could;</li>
 *   <li>published, and a reply arrived -- the only case with something to classify;</li>
 *   <li>published, and nothing arrived within the window -- the contract promised a reply and
 *       did not deliver one, though a slow service and a stuck one look alike from outside;</li>
 *   <li>could not be published at all -- a broken setup rather than a finding about the
 *       service, which is why it is reported separately from silence.</li>
 * </ul>
 */
public class AsyncApiReplyDto {

    /**
     * The index of the action this answers, echoing what was asked.
     */
    public Integer index;

    /**
     * Whether the message reached the broker. False means the driver could not publish, and
     * {@link #errorMessage} says why.
     *
     * Null means the driver did not say, which is read the same way as false: without an answer
     * here there is no knowing whether the message went out, and the rest of the test would
     * mean nothing.
     */
    public Boolean published;

    /**
     * Whether a reply arrived and was recognised as answering this message.
     *
     * Null means the driver did not say, and is read as no reply having arrived.
     */
    public Boolean replyReceived;

    /**
     * Whether the driver waited for a reply at all. False for a fire-and-forget operation, so
     * that the absence of a reply is not mistaken for silence in answer to a promise.
     *
     * Null means the driver did not say, and is read as not having waited.
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
     * Whether the reply carried back the correlation id that was stamped on the request.
     *
     * This is the honest answer to "did correlation work", which cannot be read off a contract:
     * echoing the id is the service's own behaviour. A reply that arrives without it is
     * recorded rather than treated as a fault, since from outside there is no telling a defect
     * from a service that correlates by some business key instead.
     *
     * Null means the driver does not track correlation at all, which is not the same as having
     * checked and found the id missing, and is recorded as neither.
     */
    public Boolean correlationMatched;

    /**
     * How long the driver waited, in milliseconds, whether or not anything arrived. Reported
     * because the verdict on silence is only meaningful alongside how long it was waited for.
     *
     * Null when the driver did not wait at all: no reply was expected, or nothing was published.
     */
    public Long waitedMs;

    /**
     * Why publishing failed, when it did.
     */
    public String errorMessage;

    /**
     * The publish and await this action just did, rendered as source lines for a generated test,
     * in the language {@link AsyncApiActionDto#outputFormat} asked for. Null when none was asked
     * for, or when the driver does not render them.
     *
     * A generated test talks to the broker directly rather than through the driver, so these
     * lines stand up a client of the transport, publish the same message and read the reply.
     * Only the driver can write them: it is the one side that knows the transport, and the core
     * pastes them without reading them.
     *
     * Where a reply is expected, the lines must leave it in the variable the core named in
     * {@link AsyncApiActionDto#replyVariable}, which is what the assertions the core appends are
     * written against.
     */
    public List<String> testScript;
}
