package org.evomaster.client.java.controller.api.dto.problem.asyncapi;

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
     * Key of the operation in the AsyncAPI document. Sent along so the driver can report and
     * log in terms the user will recognise from their own contract.
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
     */
    public Map<String, String> headers = new LinkedHashMap<>();

    /**
     * The value stamped into this message so that a reply can be recognised as answering it.
     *
     * It is minted fresh by the core for every execution rather than being part of the message
     * the search varies: pairing needs a value unique to the execution, and the service only
     * echoes it back.
     */
    public String correlationId;

    /**
     * Where the correlation id has to be written, as the document declares it. One of
     * {@link #CORRELATION_IN_HEADER} or {@link #CORRELATION_IN_PAYLOAD}, or null when the
     * document says nothing, in which case it is up to the driver to decide -- a transport with
     * native correlation should use it.
     */
    public String correlationLocation;

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
     * How long to wait for a reply before giving up, in milliseconds.
     *
     * There is no right answer here: a slow service and a stuck one look the same from outside,
     * so this is a tuning parameter with no equivalent in a synchronous protocol. It is set
     * generously and reported with the result.
     */
    public Long replyTimeoutMs;

    public static final String CORRELATION_IN_HEADER = "HEADER";

    public static final String CORRELATION_IN_PAYLOAD = "PAYLOAD";
}
