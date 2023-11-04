package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.PreparedStatementClassReplacement;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class H2CheckPreparedStatementTest extends DatabaseH2TestInit{

    @Test
    public void testPrepared() throws Exception {

        SqlScriptRunner.execCommand(connection, "CREATE TABLE Foo (x INT, y VARCHAR, z BOOL)");

        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Foo WHERE x=? AND y=? AND z=?");
        stmt.setBoolean(3, false);
        stmt.setInt(1, 42);
        stmt.setString(2, "BAR");

        String res = PreparedStatementClassReplacement.extractSqlFromH2PreparedStatement(stmt);

        assertEquals("SELECT * FROM Foo WHERE x = 42 AND y = 'BAR' AND z = FALSE", res);
    }
}
