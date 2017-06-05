package com.foo.rest.examples.spring.db;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.clientJava.controller.db.DbCleaner;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;

public class SpringWithDbController extends SpringController {

    protected Connection connection;


    protected SpringWithDbController(Class<?> applicationClass) {
        super(applicationClass);
    }


    @Override
    public String startSut() {

        ctx = SpringApplication.run(applicationClass, new String[]{
                "--server.port=0",
                "--spring.datasource.url=jdbc:p6spy:h2:mem:testdb;DB_CLOSE_DELAY=-1;",
                "--spring.datasource.driver-class-name=com.p6spy.engine.spy.P6SpyDriver"
        });


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

    @Override
    public void resetStateOfSUT() {
        DbCleaner.clearDatabase_H2(connection);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String getDatabaseDriverName() {
        return "org.h2.Driver";
    }
}
