package com.foo.rpc.examples.spring.db;

import com.foo.rpc.examples.spring.SpringController;
import kotlin.random.Random;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.hibernate.dialect.H2Dialect;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public abstract class SpringWithDbController extends SpringController {

    static {
        /**
         * To avoid issues with non-determinism checks (in particular in the handling of taint-analysis),
         * we must disable the cache in H2
         */
        System.setProperty("h2.objectCache", "false");
    }

    protected Connection sqlConnection;

    protected SpringWithDbController(Class<?> applicationClass) {
        super(applicationClass);
    }

    @Override
    public String startSut() {

        int rand = Random.Default.nextInt();

        ctx = SpringApplication.run(applicationClass, new String[]{
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:testdb_"+rand+";DB_CLOSE_DELAY=-1;",
                "--spring.jpa.database-platform=" + H2Dialect.class.getName(),
                "--spring.datasource.username=sa",
                "--spring.datasource.password",
                "--spring.jpa.properties.hibernate.show_sql=true"
        });


        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

        try {
            sqlConnection = jdbc.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return startClient();
    }

    @Override
    public void resetStateOfSUT() {
//        if(connection != null) {
//            DbCleaner.clearDatabase_H2(connection);
//        }
    }

    @Override
    public void stopSut() {
        super.stopSut();
//        connection = null;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return Arrays.asList(new DbSpecification(DatabaseType.H2, sqlConnection));
    }

}
