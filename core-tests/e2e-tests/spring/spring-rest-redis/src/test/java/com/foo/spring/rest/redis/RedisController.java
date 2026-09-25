package com.foo.spring.rest.redis;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.evomaster.client.java.controller.problem.RestProblem;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;

public abstract class RedisController extends EmbeddedSutController {

    private static final int REDIS_DB_PORT = 6379;
    private Jedis redisClient;

    protected static final String DEFAULT_REDIS_IMAGE = "redis:7.0";

    /**
     * Plain Redis has no RediSearch module, so SUTs using FT.* commands need Redis Stack.
     */
    protected static final String REDIS_STACK_IMAGE = "redis/redis-stack-server:7.4.0-v7";

    private final GenericContainer<?> redisContainer;
    private ConfigurableApplicationContext ctx;

    private final String databaseName;

    private final Class<?> redisAppClass;

    private String host;
    private int port;

    private ReflectionBasedRedisClient reflectionRedisClient;

    protected RedisController(String databaseName, Class<?> redisAppClass) {
        this(databaseName, redisAppClass, DEFAULT_REDIS_IMAGE);
    }

    protected RedisController(String databaseName, Class<?> redisAppClass, String redisImage) {
        this.redisContainer = new GenericContainer<>(redisImage).withExposedPorts(REDIS_DB_PORT);
        this.databaseName  = databaseName;
        this.redisAppClass = redisAppClass;
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {
        redisContainer.start();

        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(REDIS_DB_PORT);

        System.setProperty("spring.redis.host", host);
        System.setProperty("spring.redis.port", String.valueOf(port));
        this.host = host;
        this.port = port;

        redisClient = new Jedis(host, port);
        reflectionRedisClient = new ReflectionBasedRedisClient(host, port, 0);

        SpringApplicationBuilder app = new SpringApplicationBuilder(redisAppClass);
        app.properties(
                "--server.port=0",
                "spring.data.redis.host=" + host,
                "spring.data.redis.port=" + port
        );
        ctx = app.run();
        resetStateOfSUT();

        return "http://localhost:" + getSutPort();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        redisContainer.stop();
    }

    @Override
    public void resetStateOfSUT() {
        redisClient.flushDB();
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return null;
    }

    protected int getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

    @Override
    public ReflectionBasedRedisClient getRedisConnection() {
        return reflectionRedisClient; // ← instancia cacheada, no new
    }
}
