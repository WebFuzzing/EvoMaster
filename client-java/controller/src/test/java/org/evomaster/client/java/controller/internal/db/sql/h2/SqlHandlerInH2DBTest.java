package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.SqlHandlerInDBTest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SqlHandlerInH2DBTest extends DatabaseH2TestInit implements SqlHandlerInDBTest {

    /**
     * When creating an object in a table which includes an auto-incremental id,
     * then the select currval is used to calculate the id for the new object
     * @throws Exception
     */
    @Test
    public void givenASelectNextValueInASequenceThenTheQueryIsIgnoredToCalculateHeuristics() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE SEQUENCE foo_id_seq;");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE foo (id integer NOT NULL DEFAULT nextval('foo_id_seq'));");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {

            ExecutionDto dto = executeCommand(starter, "SELECT nextval('foo_id_seq')", true);

            assertNotNull(dto);
            assertNotNull(dto.queriedData);
            assertEquals(0, dto.queriedData.size());

            ExecutionDto dto2 = executeCommand(starter, "SELECT currval('foo_id_seq')", true);
            assertNotNull(dto2);
            assertNotNull(dto2.queriedData);
            assertEquals(0, dto2.queriedData.size());


        } finally {
            starter.stop();
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeH2SutController(connection);
    }

}
