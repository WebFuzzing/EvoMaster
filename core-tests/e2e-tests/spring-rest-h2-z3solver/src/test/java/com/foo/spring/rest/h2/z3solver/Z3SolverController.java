package com.foo.spring.rest.h2.z3solver;

import kotlin.random.Random;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.hibernate.dialect.H2Dialect;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Z3SolverController extends SpringController {

    private static final String CREATE_TABLES_SQL = "CREATE TABLE products (\n" +
            "  id INTEGER NOT NULL,\n" +
            "  name VARCHAR(255) NOT NULL,\n" +
            "  price DECIMAL(10,2) NOT NULL,\n" +
            "  PRIMARY KEY (id)\n" +
            ");";

    public Z3SolverController() {
        this(Z3SolverApplication.class);
    }

    public static void main(String[] args) {
        Z3SolverController controller = new Z3SolverController();
        controller.setControllerPort(40100);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

    static {
        /*
         * To avoid issues with non-determinism checks (in particular in the handling of taint-analysis),
         * we must disable the cache in H2
         */
        System.setProperty("h2.objectCache", "false");
    }

    protected Connection sqlConnection;

    protected Z3SolverController(Class<?> applicationClass) {
        super(applicationClass);
    }

    @Override
    public String startSut() {

        // lot of a problem if using same H2 instance. see:
        // https://github.com/h2database/h2database/issues/227
        int rand = Random.Default.nextInt();

        ctx = SpringApplication.run(applicationClass, "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:testdb_" + rand + ";DB_CLOSE_DELAY=-1;",
                "--spring.jpa.database-platform=" + H2Dialect.class.getName(),
                "--spring.datasource.username=sa",
                "--spring.datasource.password",
                "--spring.jpa.properties.hibernate.show_sql=true");

        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

        try {
            sqlConnection = Objects.requireNonNull(jdbc.getDataSource()).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // execute create table
        try {
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return "http://localhost:" + getSutPort();
    }

    private void createTables() throws SQLException {
        PreparedStatement stmt = sqlConnection.prepareStatement(CREATE_TABLES_SQL);
        stmt.execute();
    }

    @Override
    public void stopSut() {
        super.stopSut();
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return Collections.singletonList(new DbSpecification(DatabaseType.H2, sqlConnection));
    }
}
