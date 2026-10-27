package org.evomaster.client.java.instrumentation.external;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
    void testUnresponsiveAgentIsDisconnectedWithinTheReadBudget() throws Exception {
        ServerController server = new ServerController();
        int port = server.startServer();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch release = new CountDownLatch(1);
        Future<?> agent = executor.submit(() -> {
            try (Socket socket = new Socket("localhost", port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                assertEquals(Command.OBJECTIVE_UPDATE_COUNT, in.readObject());
                assertTrue(release.await(10, TimeUnit.SECONDS));
                //The server must close the stream, so no late response can be read by another command.
                assertEquals(-1, in.read());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        try {
            assertTrue(server.waitForIncomingConnection(10_000));
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                assertNull(server.getObjectiveUpdateCount(50));
                assertFalse(server.isConnectionOn());
                assertNull(server.getObjectiveUpdateCount(50));
            });
            release.countDown();
            agent.get(2, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            server.closeServer();
            executor.shutdownNow();
        }
    }
}
