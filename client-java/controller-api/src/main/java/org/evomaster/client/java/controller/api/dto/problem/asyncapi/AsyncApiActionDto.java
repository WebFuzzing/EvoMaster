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
     * The language to render {@link AsyncApiReplyDto#testScript} in. Only Java and Kotlin are
     * ever asked for.
     *
     * Null when no script is wanted: either the core is not generating tests, or it is
     * generating a format this enum cannot name, which is what a Python run is.
     */
    public SutInfoDto.OutputFormat outputFormat;

    /**
     * The name the rendered lines must leave the reply payload in, as text, when a reply is
     * expected. Named by the core so two actions in one test cannot collide, and set whenever
     * tests are being generated, whether or not a script was asked for. Null when they are not.
     */
    public String replyVariable;
}
