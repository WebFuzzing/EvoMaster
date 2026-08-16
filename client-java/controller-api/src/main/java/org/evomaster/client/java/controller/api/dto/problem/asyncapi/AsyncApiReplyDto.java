package org.evomaster.client.java.controller.api.dto.problem.asyncapi;

import java.util.LinkedHashMap;
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
     */
    public boolean published;

    /**
     * Whether a reply arrived and was recognised as answering this message.
     */
    public boolean replyReceived;

    /**
     * Whether the driver waited for a reply at all. False for a fire-and-forget operation, so
     * that the absence of a reply is not mistaken for silence in answer to a promise.
     */
    public boolean replyExpected;

    /**
     * The reply body, as it arrived.
     */
    public String replyPayload;

    /**
     * The reply's headers, for a transport that has them.
     */
    public Map<String, String> replyHeaders = new LinkedHashMap<>();

    /**
     * Whether the reply carried back the correlation id that was stamped on the request.
     *
     * This is the honest answer to "did correlation work", which cannot be read off a contract:
     * echoing the id is the service's own behaviour. A reply that arrives without it is
     * recorded rather than treated as a fault, since from outside there is no telling a defect
     * from a service that correlates by some business key instead.
     */
    public boolean correlationMatched;

    /**
     * How long the driver waited, in milliseconds, whether or not anything arrived. Reported
     * because the verdict on silence is only meaningful alongside how long it was waited for.
     */
    public Long waitedMs;

    /**
     * Why publishing failed, when it did.
     */
    public String errorMessage;
}
