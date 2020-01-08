package com.foo.spring.rest.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
import com.p6spy.engine.spy.P6SpyDriver;
import org.bson.Document;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SpringRestMongodbController extends EmbeddedSutController {

    private final Class<?> applicationClass;

    private ConfigurableApplicationContext ctx;
    private Connection connection;
    private MongoClient mongoClient;

    public SpringRestMongodbController(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    private final static int MONGO_PORT = 27017; //https://docs.mongodb.com/manual/reference/default-mongodb-port/

    private final GenericContainer mongodb = new GenericContainer("mongo:3.2")
            .withExposedPorts(MONGO_PORT);

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String getDatabaseDriverName() {
        //return "mongodb.jdbc.MongoDriver";
        return "org.h2.Driver";
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
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }

    @Override
    public String startSut() {

        mongodb.start();

        mongoClient = new MongoClient(mongodb.getContainerIpAddress(),
                mongodb.getMappedPort(MONGO_PORT));

        String host = mongodb.getContainerIpAddress();
        int port = mongodb.getMappedPort(MONGO_PORT);

        ArrayList<String> l = new ArrayList<>();
        for (String dbName : mongoClient.listDatabaseNames()) {
            l.add(dbName);
        }

        ArrayList<String> c = new ArrayList<>();
        for (String collName : mongoClient.getDatabase("local").listCollectionNames()) {
            c.add(collName);
        }

        ctx = SpringApplication.run(applicationClass,
                "--server.port=0",
                "--spring.data.mongodb.host=" + host,
                "--spring.data.mongodb.port=" + port,
                "--spring.data.mongodb.database=testdb",
                "--spring.datasource.url=jdbc:p6spy:h2:mem:testdb;DB_CLOSE_DELAY=-1;",
                "--spring.datasource.driver-class-name=" + P6SpyDriver.class.getName(),
                "--spring.mongodb.embedded.version=3.2",
                "--spring.main.allow-bean-definition-overriding=true"); // start app with mongodb connected

        for (String collectionName :mongoClient.getDatabase("testdb").listCollectionNames()) {
            MongoCursor<Document> cursor = mongoClient.getDatabase("testdb").getCollection(collectionName).find().iterator() ;
            while (cursor.hasNext()) {
                Document doc = cursor.next();
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

        try {
            connection = jdbc.getDataSource().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "http://localhost:" + getSutPort();

    }

    private Integer getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        mongodb.stop();
    }

    @Override
    public void resetStateOfSUT() {
        mongoClient.getDatabase("testdb").drop();
        //DbCleaner.clearDatabase_H2(connection);
    }
}
