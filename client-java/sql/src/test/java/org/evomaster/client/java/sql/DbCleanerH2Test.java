package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

/**
 * Created by arcuri82 on 08-Apr-19.
 */
public class DbCleanerH2Test  extends DbCleanerTestBase{


    private static Connection connection;


    @BeforeAll
    public static void initClass() throws Exception {

        connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
    }

    @BeforeEach
    public void initTest() throws Exception {
        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;");
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

    @Override
    protected void clearDatabase(List<String> tablesToSkip, List<String> tableToClean) {
        DbCleaner.clearDatabase_H2(connection, "PUBLIC", tablesToSkip, tableToClean);
    }

    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.H2;
    }
}
