package com.foo.spring.rest.neo4j;

import com.neo4j.AbstractNeo4jRest;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;

public abstract class Neo4jController extends EmbeddedSutController {

    private static final int NEO4J_BOLT_PORT = 7687;

    private final GenericContainer<?> neo4jContainer = new GenericContainer<>("neo4j:5")
            .withExposedPorts(NEO4J_BOLT_PORT)
            .withEnv("NEO4J_AUTH", "none");

    private ConfigurableApplicationContext ctx;

    private final Class<?> neo4jAppClass;

    private Driver driver;

    protected Neo4jController(Class<?> neo4jAppClass) {
        this.neo4jAppClass = neo4jAppClass;
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {
        neo4jContainer.start();

        String uri = "bolt://" + neo4jContainer.getHost() + ":" + neo4jContainer.getMappedPort(NEO4J_BOLT_PORT);
        System.setProperty(AbstractNeo4jRest.NEO4J_URI_PROPERTY, uri);

        driver = GraphDatabase.driver(uri, AuthTokens.none());
        driver.verifyConnectivity();

        SpringApplicationBuilder app = new SpringApplicationBuilder(neo4jAppClass);
        app.properties("--server.port=0");
        ctx = app.run();
        resetStateOfSUT();

        return "http://localhost:" + getSutPort();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        driver.close();
        neo4jContainer.stop();
    }

    @Override
    public void resetStateOfSUT() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
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
    public Object getNeo4jConnection() {
        return driver;
    }
}
