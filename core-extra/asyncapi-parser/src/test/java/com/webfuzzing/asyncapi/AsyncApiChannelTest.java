package com.webfuzzing.asyncapi;

import com.webfuzzing.asyncapi.models.AsyncApiChannel;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsyncApiChannelTest {

    @Test
    public void testACollectionThatIsRequiredCannotBeNull() {

        /*
            A document may leave these out, and the parser then passes an empty collection: for
            servers, empty means every server. Null is never a document state, only a caller's
            mistake, and it fails here naming the field rather than later inside the constructor.
         */
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> AsyncApiChannel.builder("c").servers(null));
        assertEquals("servers", e.getMessage());

        e = assertThrows(NullPointerException.class,
                () -> AsyncApiChannel.builder("c").messageKeys(null));
        assertEquals("messageKeys", e.getMessage());

        e = assertThrows(NullPointerException.class,
                () -> AsyncApiChannel.builder("c").bindings(null));
        assertEquals("bindings", e.getMessage());
    }

    @Test
    public void testTheNameIsRequired() {

        NullPointerException e = assertThrows(NullPointerException.class,
                () -> AsyncApiChannel.builder(null));
        assertEquals("name", e.getMessage());
    }

    @Test
    public void testEmptyIsNotNull() {

        //what the parser passes for a channel that declares no servers and no messages
        AsyncApiChannel channel = AsyncApiChannel.builder("c")
                .servers(Collections.<String>emptyList())
                .messageKeys(Collections.<String, String>emptyMap())
                .build();

        assertTrue(channel.getServers().isEmpty());
        assertTrue(channel.getMessageIds().isEmpty());
    }
}
