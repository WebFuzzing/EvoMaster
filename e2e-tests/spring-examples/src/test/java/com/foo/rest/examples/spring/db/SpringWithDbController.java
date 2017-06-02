package com.foo.rest.examples.spring.db;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.clientJava.controller.db.DbCleaner;
import org.evomaster.clientJava.controller.db.SqlScriptRunner;
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

        String url = super.startSut();

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

        return url;
    }

    @Override
    public void resetStateOfSUT() {
        DbCleaner.clearDatabase_H2(connection);
    }
}
