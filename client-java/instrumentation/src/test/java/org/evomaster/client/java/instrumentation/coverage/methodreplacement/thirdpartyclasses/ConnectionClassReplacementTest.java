package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ConnectionClassReplacementTest {

    private Connection mockConnection;

    private static final String GET = "GET";

    @BeforeEach
    public void setup() {
        ExecutionTracer.reset();
        mockConnection = mock(Connection.class);
    }

    private enum FakeJsonCommand implements ProtocolCommand {
        GET("JSON.GET");

        private final byte[] raw;

        FakeJsonCommand(String alt) {
            raw = SafeEncoder.encode(alt);
        }

        @Override
        public byte[] getRaw() {
            return raw;
        }
    }

    @Test
    public void testExecuteCommandCoreGet() {
        String key = "foo";
        CommandArguments args = new CommandArguments(Protocol.Command.GET).key(key);
        CommandObject<String> commandObject = new CommandObject<>(args, null);

        ConnectionClassReplacement.executeCommand(mockConnection, commandObject);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        org.evomaster.client.java.instrumentation.RedisCommand redisCmd =
                infoList.get(0).getRedisCommandData().iterator().next();

        assertEquals(GET, redisCmd.getType().name());
        assertArrayEquals(new String[]{key}, redisCmd.getArgs());
    }

    @Test
    public void testExecuteCommandJsonGet() {
        String key = "mykey";
        CommandArguments args = new CommandArguments(FakeJsonCommand.GET)
                .add(RawableFactory.from(key));
        CommandObject<Object> commandObject = new CommandObject<>(args, null);

        ConnectionClassReplacement.executeCommand(mockConnection, commandObject);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        org.evomaster.client.java.instrumentation.RedisCommand redisCmd =
                infoList.get(0).getRedisCommandData().iterator().next();

        assertEquals("JSON_GET", redisCmd.getType().name());
        assertArrayEquals(new String[]{key}, redisCmd.getArgs());
    }
}
