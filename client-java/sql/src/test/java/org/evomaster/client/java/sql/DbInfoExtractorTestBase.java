package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ForeignKeyDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public abstract class DbInfoExtractorTestBase {

    protected abstract DatabaseType getDbType();
    protected abstract Connection getConnection();

    @Test
    public void testCompositeForeignKey() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Parent(" +
                "id1 bigint, " +
                "id2 bigint, " +
                "primary key (id1, id2)" +
                ")");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Child(" +
                "id bigint primary key,  " +
                "pid1 bigint not null, " +
                "pid2 bigint not null " +
                ")");
        SqlScriptRunner.execCommand(getConnection(), "ALTER TABLE Child add constraint compositeKey foreign key (pid1, pid2) references Parent(id1, id2)");

        DbInfoDto schema = DbInfoExtractor.extract(getConnection());
        TableDto parent = schema.tables.stream().filter(t -> t.id.name.equalsIgnoreCase("Parent")).findAny().get();
        TableDto child = schema.tables.stream().filter(t -> t.id.name.equalsIgnoreCase("Child")).findAny().get();

        assertEquals(0, parent.foreignKeys.size());
        assertEquals(1, child.foreignKeys.size());

        ForeignKeyDto foreignKey = child.foreignKeys.get(0);

        assertEquals(2, foreignKey.sourceColumns.size());
        assertTrue(foreignKey.sourceColumns.stream().anyMatch(c -> c.equalsIgnoreCase("pid1")));
        assertTrue(foreignKey.sourceColumns.stream().anyMatch(c -> c.equalsIgnoreCase("pid2")));
        assertTrue(foreignKey.targetTable.equalsIgnoreCase("Parent"));

        assertEquals(2, foreignKey.targetColumns.size());
        assertTrue(foreignKey.targetColumns.stream().anyMatch(c -> c.equalsIgnoreCase("id1")));
        assertTrue(foreignKey.targetColumns.stream().anyMatch(c -> c.equalsIgnoreCase("id2")));
    }

}
