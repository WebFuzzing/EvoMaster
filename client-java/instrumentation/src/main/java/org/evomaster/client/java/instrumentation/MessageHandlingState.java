package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * How far the SUT got, in the current action, with the messages it consumed from one address,
 * eg a Kafka topic. A message counts as handled once the thread that received it asks the
 * client for more, see
 * {@link org.evomaster.client.java.instrumentation.staticstate.AsyncMessageTracker} for the
 * limits of this. Serializable for external mode.
 */
public class MessageHandlingState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * How many messages from the address the SUT received in the current action.
     */
    private final int received;

    /**
     * How many of those are not handled yet.
     */
    private final int unhandled;

    public MessageHandlingState(int received, int unhandled) {
        if (received < 0 || unhandled < 0) {
            throw new IllegalArgumentException("Negative counts: " + received + ", " + unhandled);
        }
        if (unhandled > received) {
            throw new IllegalArgumentException("More unhandled (" + unhandled + ") messages than received ("
                    + received + ")");
        }
        this.received = received;
        this.unhandled = unhandled;
    }

    public int getReceived() {
        return received;
    }

    public int getUnhandled() {
        return unhandled;
    }

    /**
     * @return whether at least one message was received from the address in the current
     * action, and every thread that received one has since come back for more
     */
    public boolean isHandled() {
        return received > 0 && unhandled == 0;
    }

    @Override
    public String toString() {
        return "MessageHandlingState{received=" + received + ", unhandled=" + unhandled + "}";
    }
}
