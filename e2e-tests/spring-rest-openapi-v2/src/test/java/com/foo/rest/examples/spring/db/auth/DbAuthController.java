package com.foo.rest.examples.spring.db.auth;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.SqlScriptRunner;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class DbAuthController extends SpringWithDbController {

    private static final String userId = "foo";
    private static final String password = "123";

    public DbAuthController() {
        super(AuthApp.class);
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {

       return Arrays.asList(AuthUtils.getForBasic("example", userId, password));
    }


    @Override
    public void resetStateOfSUT() {
        DbCleaner.clearDatabase_H2(sqlConnection);

        try {
            SqlScriptRunner.execInsert(sqlConnection,
                    "insert into Auth_User_Entity(user_id,password) values('"+userId+"','"+password+"');");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
