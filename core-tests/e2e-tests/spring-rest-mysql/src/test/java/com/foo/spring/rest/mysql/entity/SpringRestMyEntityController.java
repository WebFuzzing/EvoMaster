package com.foo.spring.rest.mysql.entity;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.hibernate.dialect.MySQL8Dialect;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpringRestMyEntityController extends EmbeddedSutController {

    private static final String MYSQL_DB_NAME = "test";
    private static final int MYSQL_PORT = 3306;
    private static final String MYSQL_VERSION = "8.0.27";

    private ConfigurableApplicationContext ctx = null;
    private GenericContainer<?> mysql = new GenericContainer<>("mysql:" + MYSQL_VERSION)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", MYSQL_DB_NAME)
            .withEnv("MYSQL_USER", "test")
            .withEnv("MYSQL_PASSWORD", "test")
            .withExposedPorts(MYSQL_PORT);

    private Connection dbConnection;

    public SpringRestMyEntityController() {
        super.setControllerPort(0);
    }

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.";
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

    protected int getSutPort() {
        return (Integer) ((Map<?, ?>) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }


    @Override
    public String startSut() {
        mysql.start();

        String host = mysql.getContainerIpAddress();
        int port = mysql.getMappedPort(MYSQL_PORT);
        String url = "jdbc:mysql://" + host + ":" + port + "/" + MYSQL_DB_NAME;


        try {
            dbConnection = DriverManager.getConnection(url, "test", "test");


            ctx = SpringApplication.run(MyEntityApplication.class,
                    "--server.port=0",
                    "--spring.datasource.url=" + url,
                    "--spring.datasource.username=test",
                    "--spring.datasource.password=test",

                    "--spring.jpa.database-platform=" + MySQL8Dialect.class.getName(),
                    "--spring.jpa.hibernate.ddl-auto=validate",
                    "--spring.jpa.properties.hibernate.show_sql=true",

                    "--spring.flyway.locations=" + pathToFlywayFiles(),
                    "--spring.jmx.enabled=false"
            );

            dbConnection.close();

            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            dbConnection = jdbc.getDataSource().getConnection();

            return "http://localhost:" + getSutPort();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String pathToFlywayFiles() {
        return "classpath:/schema/entity";
    }

    @Override
    public void stopSut() {
        if (ctx != null) {
            ctx.stop();
            ctx.close();
        }
        mysql.stop();
    }

    @Override
    public void resetStateOfSUT() {
        //TODO
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return Collections.singletonList(
                new DbSpecification(DatabaseType.MYSQL, dbConnection)
                        .withSchemas(MYSQL_DB_NAME));
    }


}
