package org.evomaster.client.java.instrumentation.external;

import org.evomaster.client.java.instrumentation.TargetInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The driver side of a SUT restart: the ids that one SUT process reports have to reach the next process
 * as soon as it connects. The SUT processes are played by fake agents talking the agent protocol.
 */
public class ServerControllerIdMappingTest {

    private final ServerController server = new ServerController();

    @AfterEach
    public void close() {
        server.closeServer();
    }

    /**
     * Connects to the driver like a new SUT process, reads the id mapping the driver sends first and,
     * if [reported] is not null, answers one request of target info with it.
     *
     * @return the id mapping received on connection
     */
    private CompletableFuture<Map<String, Integer>> fakeAgent(int port, List<TargetInfo> reported) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Socket socket = new Socket("localhost", port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Map<String, Integer> mapping = (Map<String, Integer>) in.readObject();

                if (reported != null) {
                    assertEquals(Command.TARGETS_INFO, in.readObject());
                    assertTrue(in.readObject() instanceof TargetInfoRequestDto);
                    out.writeObject(reported);
                    out.flush();
                }
                return mapping;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRestartedSutGetsTheIdsOfThePreviousOne() throws Exception {

        int port = server.startServer();
        CompletableFuture<Map<String, Integer>> first = fakeAgent(port, Arrays.asList(
                new TargetInfo(5, "Line_A", 1d, 0),
                new TargetInfo(7, "Line_B", 0.5, 0)));
        assertTrue(server.waitForIncomingConnection(10_000));

        List<TargetInfo> infos = server.getTargetsInfo(Collections.emptyList(), false, true);
        assertEquals(2, infos.size());
        assertTrue(first.get(10, TimeUnit.SECONDS).isEmpty());

        // the SUT is restarted: a new process connects to a new server socket
        port = server.startServer();
        CompletableFuture<Map<String, Integer>> second = fakeAgent(port, null);
        assertTrue(server.waitForIncomingConnection(10_000));

        Map<String, Integer> mapping = second.get(10, TimeUnit.SECONDS);
        assertEquals(2, mapping.size());
        assertEquals(5, mapping.get("Line_A"));
        assertEquals(7, mapping.get("Line_B"));
    }

    @Test
    public void testTargetsWithoutDescriptiveIdAreNotRecorded() throws Exception {

        int port = server.startServer();
        CompletableFuture<Map<String, Integer>> first = fakeAgent(port, Arrays.asList(
                new TargetInfo(5, "Line_A", 1d, 0),
                TargetInfo.notReached(9)));
        assertTrue(server.waitForIncomingConnection(10_000));
        server.getTargetsInfo(Collections.emptyList(), false, true);
        first.get(10, TimeUnit.SECONDS);

        port = server.startServer();
        CompletableFuture<Map<String, Integer>> second = fakeAgent(port, null);
        assertTrue(server.waitForIncomingConnection(10_000));

        assertEquals(Collections.singletonMap("Line_A", 5), second.get(10, TimeUnit.SECONDS));
    }
}
