package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.search.SearchProtocol;

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
    public void testExecuteCommandFtAggregate() {
        String indexName = "myIndex";
        String query = "*";
        CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.AGGREGATE)
                .add(indexName)
                .add(query)
                .add("GROUPBY")
                .add("1")
                .add("@category");
        CommandObject<Object> commandObject = new CommandObject<>(args, null);

        ConnectionClassReplacement.executeCommand(mockConnection, commandObject);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        org.evomaster.client.java.instrumentation.RedisCommand redisCmd =
                infoList.get(0).getRedisCommandData().iterator().next();

        assertEquals("FT_AGGREGATE", redisCmd.getType().name());
        assertArrayEquals(new String[]{indexName, query, "GROUPBY", "1", "@category"}, redisCmd.getArgs());
    }

    @Test
    public void testExecuteCommandFtSearch() {
        String indexName = "myIndex";
        String query = "@title:redis";
        CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.SEARCH)
                .add(indexName)
                .add(query);
        CommandObject<Object> commandObject = new CommandObject<>(args, null);

        ConnectionClassReplacement.executeCommand(mockConnection, commandObject);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        org.evomaster.client.java.instrumentation.RedisCommand redisCmd =
                infoList.get(0).getRedisCommandData().iterator().next();

        assertEquals("FT_SEARCH", redisCmd.getType().name());
        assertArrayEquals(new String[]{indexName, query}, redisCmd.getArgs());
    }
}
