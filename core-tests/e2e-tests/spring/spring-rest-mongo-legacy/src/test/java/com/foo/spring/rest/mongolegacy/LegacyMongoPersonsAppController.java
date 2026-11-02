package com.foo.spring.rest.mongolegacy;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongolegacy.persons.LegacyMongoPersonsApp;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LegacyMongoPersonsAppController extends EmbeddedSutController {

    private static final int MONGODB_PORT = 27017;

    private static final String DATABASE_NAME = "legacymongo";

    private MongoClient mongoClient;

    /*
        MongoDB 4.4 is a server version supported by the legacy 3.8 driver wire protocol,
        and it has images for both amd64 and arm64
     */
    private final GenericContainer<?> mongodb = new GenericContainer<>("mongo:4.4")
            .withTmpFs(Collections.singletonMap("/data/db", "rw"))
            .withExposedPorts(MONGODB_PORT);

    private ConfigurableApplicationContext ctx;

    public LegacyMongoPersonsAppController() {
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {
        mongodb.start();

        String host = mongodb.getHost();
        int port = mongodb.getMappedPort(MONGODB_PORT);

        mongoClient = MongoClients.create("mongodb://" + host + ":" + port + "/" + DATABASE_NAME);

        SpringApplicationBuilder app = new SpringApplicationBuilder(LegacyMongoPersonsApp.class);

        app.properties(
                "--server.port=0",
                "spring.data.mongodb.host=" + host,
                "spring.data.mongodb.port=" + port,
                "spring.data.mongodb.database=" + DATABASE_NAME
        );

        ctx = app.run();

        return "http://localhost:" + getSutPort();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        mongoClient.close();
        mongodb.stop();
    }

    @Override
    public void resetStateOfSUT() {
        mongoClient.getDatabase(DATABASE_NAME).drop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongolegacy.persons";
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
