package com.foo.spring.rest.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class MongoController extends EmbeddedSutController {

    private static final int MONGODB_PORT = 27017;
    private MongoClient mongoClient;

    private final GenericContainer<?> mongodb = new GenericContainer<>("mongo:6.0")
            .withTmpFs(Collections.singletonMap("/data/db", "rw"))
            .withExposedPorts(MONGODB_PORT);
    private ConfigurableApplicationContext ctx;

    private final String databaseName;

    private final Class<?> mongoAppClass;

    protected MongoController(String databaseName, Class<?> mongoAppClass) {
        this.databaseName  = databaseName;
        this.mongoAppClass = mongoAppClass;
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {
        mongodb.start();

        String host = mongodb.getHost();
        int port = mongodb.getMappedPort(MONGODB_PORT);

        mongoClient = MongoClients.create("mongodb://localhost:" + port + "/" + databaseName);

        SpringApplicationBuilder app = new SpringApplicationBuilder(mongoAppClass);

        app.properties(
                "--server.port=0",
                "spring.data.mongodb.host=" + host,
                "spring.data.mongodb.port=" + port,
                "spring.data.mongodb.database=" + databaseName
        );

        ctx = app.run();

        return "http://localhost:" + getSutPort();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        mongodb.stop();
    }

    @Override
    public void resetStateOfSUT() {
        mongoClient.getDatabase(databaseName).drop();
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
    public MongoClient getMongoConnection() {
        return mongoClient;
    }
}
