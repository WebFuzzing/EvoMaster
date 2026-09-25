package com.foo.spring.rest.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.cassandra.CassandraCleaner;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class CassandraController extends EmbeddedSutController {

    private static final int CASSANDRA_PORT = 9042;
    private static final String KEYSPACE = "evomaster_e2e";

    private final GenericContainer<?> cassandraContainer = new GenericContainer<>("cassandra:4.1")
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private ConfigurableApplicationContext ctx;

    private final Class<?> cassandraAppClass;

    protected CassandraController(Class<?> cassandraAppClass) {
        this.cassandraAppClass = cassandraAppClass;
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {
        cassandraContainer.start();

        SpringApplicationBuilder app = new SpringApplicationBuilder(cassandraAppClass);
        app.properties(
                "--server.port=0",
                "cassandra.host=" + cassandraContainer.getHost(),
                "cassandra.port=" + cassandraContainer.getMappedPort(CASSANDRA_PORT)
        );
        ctx = app.run();
        resetStateOfSUT();

        return "http://localhost:" + getSutPort();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        cassandraContainer.stop();
    }

    @Override
    public void resetStateOfSUT() {
        CassandraCleaner.clearKeyspaces(getCassandraConnection(), Collections.singletonList(KEYSPACE));
    }

    /**
     * The very session the SUT uses, which is what the heuristics need to read the rows of a table
     * a query of the SUT came back empty from.
     */
    @Override
    public Object getCassandraConnection() {
        return ctx.getBean(CqlSession.class);
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
}