package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.InitSqlScriptWithSmartDbCleanTest;

import java.sql.Connection;
import java.util.Arrays;

public class MySQLInitSqlScriptWithSmartDbCleanTest extends DatabaseMySQLTestInit implements InitSqlScriptWithSmartDbCleanTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection, getInitSqlScript());
    }

    @Override
    public String getInitSqlScript() {
        return String.join("\n",
                Arrays.asList(
                        "INSERT INTO `Bar` (id, valueColumn) VALUES (0, 0);",
                        "INSERT INTO `Bar` (id, valueColumn) VALUES (1, 0);",
                        "INSERT INTO `Bar` (id, valueColumn) VALUES (2, 0);",
                        "INSERT INTO Foo (id, valueColumn, refer_foo, bar_id) VALUES (0, 0, NULL, 0);",
                        "INSERT INTO Foo (id, valueColumn, refer_foo, bar_id) VALUES (1, 0, 0, 1);",
                        "INSERT INTO Foo (id, valueColumn, refer_foo, bar_id) VALUES (2, 0, 1, 2);",
                        "UPDATE Foo SET valueColumn = 2 WHERE id = 2;",
                        "INSERT INTO Abc (id, valueColumn, foo_id) VALUES (0, 0, 0);",
                        "INSERT INTO Abc (id, valueColumn, foo_id) VALUES (1, 0, 1);",
                        "INSERT INTO Abc (id, valueColumn, foo_id) VALUES (2, 0, 2);",
                        "INSERT INTO Xyz (id, valueColumn, abc_id) VALUES (0, 0, 0);",
                        "INSERT INTO Xyz (id, valueColumn, abc_id) VALUES (1, 0, 1);"

                )
        );
    }
}
