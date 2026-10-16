package org.evomaster.client.java.instrumentation.external;

import org.evomaster.client.java.instrumentation.MessageHandlingState;
import org.evomaster.client.java.instrumentation.staticstate.AsyncMessageTracker;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The controller's side of the connection to the agent of an external SUT, talking to a real
 * {@link AgentController} over a local socket. Both run in this JVM, so the agent answers from
 * the same {@link ExecutionTracer} the test drives.
 */
public class ServerControllerTest {

    /**
     * Generous, as it only bounds how long a broken connection takes to fail the test. Despite
     * the parameter's name, waitForIncomingConnection reads it as milliseconds.
     */
    private static final int TIMEOUT_MS = 10_000;

    private static final String COMPARISON = "ServerControllerTest_comparison";

    private static final String TOPIC = "ServerControllerTest_requests";

    private static final Integer NOT_AN_ADDRESS = 42;

    /**
     * A numeric comparison reports three objectives at once: less than, equal and greater than.
     */
    private static final long OBJECTIVES_PER_COMPARISON = 3;

    /**
     * Can close its end of the connection, which is what ends the agent's thread.
     */
    private static class ClosableServerController extends ServerController {
        void closeConnection() throws IOException {
            out.close();
        }
    }

    private ClosableServerController server;

    @BeforeEach
    public void connect() throws InterruptedException {
        ExecutionTracer.reset();

        server = new ClosableServerController();
        int port = server.startServer();

        //the agent blocks until the server accepts, so it connects from a thread of its own
        Thread agent = new Thread(() -> AgentController.start(port));
        agent.start();

        assertTrue(server.waitForIncomingConnection(TIMEOUT_MS), "the agent did not connect");
        agent.join(TIMEOUT_MS);
    }

    @AfterEach
    public void disconnect() throws IOException {
        server.closeConnection();
        server.closeServer();
        ExecutionTracer.reset();
    }

    @Test
    public void testTheObjectiveUpdateCountComesBackFromTheAgent() {

        assertEquals(0L, server.getObjectiveUpdateCount());

        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);

        assertEquals(OBJECTIVES_PER_COMPARISON, server.getObjectiveUpdateCount());
        //asking does not change it
        assertEquals(OBJECTIVES_PER_COMPARISON, server.getObjectiveUpdateCount());
    }

    @Test
    public void testANewTestResetsTheCountInTheAgent() {

        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);

        assertTrue(server.resetForNewTest());

        assertEquals(0L, server.getObjectiveUpdateCount());
    }

    @Test
    public void testTheCountCanBeAskedForBetweenOtherCommands() {

        /*
            Commands share one connection and are answered in order, so a reply read out of turn
            would show up as the wrong type here.
         */
        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);

        assertTrue(server.setKillSwitch(false));
        assertEquals(OBJECTIVES_PER_COMPARISON, server.getObjectiveUpdateCount());
        assertTrue(server.setExecutingAction(true));
        assertEquals(OBJECTIVES_PER_COMPARISON, server.getObjectiveUpdateCount());
    }

    @Test
    public void testTheMessageHandlingStateComesBackFromTheAgent() {

        MessageHandlingState nothing = server.getMessageHandlingState(TOPIC);
        assertEquals(0, nothing.getReceived());

        AsyncMessageTracker.received(Collections.singletonList(TOPIC));

        MessageHandlingState pending = server.getMessageHandlingState(TOPIC);
        assertEquals(1, pending.getReceived());
        assertEquals(1, pending.getUnhandled());

        //the agent answers on a thread of its own, so this one is still the one that received
        AsyncMessageTracker.askingForMore(true);

        assertTrue(server.getMessageHandlingState(TOPIC).isHandled());
        //and other commands still get their own answers
        assertEquals(0L, server.getObjectiveUpdateCount());
    }

    @Test
    public void testAQuestionTheAgentFailsToAnswerIsAnsweredWithNull() {

        /*
            An address that is not a String makes the agent fail. It still answers, with null,
            rather than leaving the controller waiting until its read times out.
         */
        Object answer = assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MS / 2), () -> {
            assertTrue(server.sendCommand(Command.MESSAGE_HANDLING_STATE));
            assertTrue(server.sendObject(NOT_AN_ADDRESS));
            return server.waitAndGetResponse();
        });

        assertNull(answer);
        //and the connection is still in step
        assertEquals(0L, server.getObjectiveUpdateCount());
    }
}
