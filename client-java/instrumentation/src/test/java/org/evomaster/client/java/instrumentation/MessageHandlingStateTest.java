package org.evomaster.client.java.instrumentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageHandlingStateTest {

    @Test
    public void testHandledNeedsSomethingReceivedAndNothingPending() {

        assertFalse(new MessageHandlingState(0, 0).isHandled(), "nothing received");
        assertFalse(new MessageHandlingState(2, 1).isHandled(), "one still pending");
        assertTrue(new MessageHandlingState(2, 0).isHandled());
    }

    @Test
    public void testCountsThatCannotBeAreRejected() {

        assertThrows(IllegalArgumentException.class, () -> new MessageHandlingState(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MessageHandlingState(1, -1));
        assertThrows(IllegalArgumentException.class, () -> new MessageHandlingState(1, 2));
    }
}
