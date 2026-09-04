package com.webfuzzing.asyncapi.models;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The {@code reply:} of a request/reply operation -- the construct AsyncAPI 3.0 added that
 * makes an asynchronous interaction observable without instrumentation. 2.x has no equivalent.
 */
public class AsyncApiReply {

    private final String channelName;

    /**
     * Ids of the messages the reply may be, in declaration order. More than one means the
     * contract enumerates distinct outcomes, such as a result and an error.
     */
    private final List<String> messageIds;

    private final String addressLocation;

    /**
     * {@code channelName} and {@code addressLocation} are both nullable: a reply may name no
     * usable channel as long as it announces an address, and vice versa.
     */
    public AsyncApiReply(String channelName, List<String> messageIds, String addressLocation) {
        Objects.requireNonNull(messageIds, "messageIds");
        this.channelName = channelName;
        this.messageIds = Collections.unmodifiableList(messageIds);
        this.addressLocation = addressLocation;
    }

    /**
     * Key of the channel the reply arrives on, quite often the very channel the request went
     * out on, since a WebSocket connection is duplex.
     *
     * Null when the operation declares a reply without naming a usable channel, which is
     * legitimate only alongside an {@link #getAddressLocation()}.
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Ids of the messages the reply may be. More than one means the contract enumerates
     * distinct outcomes -- a result and an error, say -- which is what would give a black-box
     * search something to tell apart.
     */
    public List<String> getMessageIds() {
        return messageIds;
    }

    /**
     * {@code reply.address.location} verbatim, when the reply address is not fixed but is
     * announced by the requester inside the request itself. Its presence is what allows the
     * reply channel to have no address of its own.
     */
    public String getAddressLocation() {
        return addressLocation;
    }
}
