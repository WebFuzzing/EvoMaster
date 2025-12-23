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

    private final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_DB_PORT);
    private ConfigurableApplicationContext ctx;

    private final String databaseName;

    private final Class<?> redisAppClass;

    private String host;
    private int port;

    protected RedisController(String databaseName, Class<?> redisAppClass) {
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
        redisContainer.stop();
        ctx.stop();
        ctx.close();
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
        return new ReflectionBasedRedisClient(this.host, this.port);
    }
}
