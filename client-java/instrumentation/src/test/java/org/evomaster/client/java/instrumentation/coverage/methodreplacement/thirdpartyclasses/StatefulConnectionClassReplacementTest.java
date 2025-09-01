package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.codec.StringCodec;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StatefulConnectionClassReplacementTest {

    private StatefulConnection<String, String> mockConnection;
    private static final String GET = "GET";
    private static final String HGET = "HGET";

    @BeforeEach
    public void setup() {
        ExecutionTracer.reset();
        mockConnection = mock(StatefulConnection.class);
    }

    @Test
    public void testDispatchGet() {
        String key = "foo";

        RedisCommand<String, String, String> lettuceCmd = new Command<>(
                CommandType.GET,
                new ValueOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key)
        );

        StatefulConnectionClassReplacement.dispatch(mockConnection, lettuceCmd);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        org.evomaster.client.java.instrumentation.RedisCommand redisCmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(GET, redisCmd.getType().name());
        assertArrayEquals(new String[]{createKeyArg(key)}, redisCmd.getArgs());
    }

    @Test
    public void testDispatchHGet() {
        String key = "lettuce-key";
        String field = "hashValue";

        RedisCommand<String, String, String> lettuceCmd = new Command<>(
                CommandType.HGET,
                new ValueOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key).addKey(field)
        );

        StatefulConnectionClassReplacement.dispatch(mockConnection, lettuceCmd);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        org.evomaster.client.java.instrumentation.RedisCommand redisCmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(HGET, redisCmd.getType().name());
        assertArrayEquals(new String[]{
                createKeyArg(key),
                createKeyArg(field)
        }, redisCmd.getArgs());
    }

    private String createKeyArg(String key) {
        return "key<" + key + ">";
    }
}