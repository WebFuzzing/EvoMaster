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
        assertTrue(foreignKey.sourceColumns.get(0).equalsIgnoreCase("pid1"));
        assertTrue(foreignKey.sourceColumns.get(1).equalsIgnoreCase("pid2"));
        assertTrue(foreignKey.targetTable.equalsIgnoreCase("Parent"));

        assertEquals(2, foreignKey.targetColumns.size());
        assertTrue(foreignKey.targetColumns.get(0).equalsIgnoreCase("id1"));
        assertTrue(foreignKey.targetColumns.get(1).equalsIgnoreCase("id2"));
    }

    @Test
    public void testOneImplicitCompositeForeignKey() throws Exception {
        /**
         * Implicity foreign keys are not supported by MySQL, so we skip this test for MySQL databases.
         */
        Assumptions.assumeTrue(this.getDbType() != DatabaseType.MYSQL);

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Parent(" +
                "id1 bigint, " +
                "id2 bigint, " +
                "primary key (id1, id2)" +
                ")");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Child(" +
                "id bigint primary key,  " +
                "pid1 bigint not null, " +
                "pid2 bigint not null, " +
                "foreign key (pid1, pid2) references Parent" +
                ")");

        DbInfoDto schema = DbInfoExtractor.extract(getConnection());
        TableDto child = schema.tables.stream().filter(t -> t.id.name.equalsIgnoreCase("Child")).findAny().get();

        assertEquals(1, child.foreignKeys.size());

        ForeignKeyDto foreignKey = child.foreignKeys.get(0);

        assertEquals(2, foreignKey.sourceColumns.size());
        assertTrue(foreignKey.sourceColumns.get(0).equalsIgnoreCase("pid1"));
        assertTrue(foreignKey.sourceColumns.get(1).equalsIgnoreCase("pid2"));
        assertTrue(foreignKey.targetTable.equalsIgnoreCase("Parent"));

        assertEquals(2, foreignKey.targetColumns.size());
        assertTrue(foreignKey.targetColumns.get(0).equalsIgnoreCase("id1"));
        assertTrue(foreignKey.targetColumns.get(1).equalsIgnoreCase("id2"));
    }

    @Test
    public void testTwoCompositeExplicitForeignKeys() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Parent1(" +
                "id1 bigint, " +
                "id2 bigint, " +
                "primary key (id1, id2)" +
                ")");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Parent2(" +
                "id1 bigint, " +
                "id2 bigint, " +
                "primary key (id1, id2)" +
                ")");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE ChildTwoImplicit(" +
                "id bigint primary key,  " +
                "p1_id1 bigint not null, " +
                "p1_id2 bigint not null, " +
                "p2_id1 bigint not null, " +
                "p2_id2 bigint not null, " +
                "foreign key (p1_id1, p1_id2) references Parent1(id1, id2), " +
                "foreign key (p2_id1, p2_id2) references Parent2(id1, id2) " +
                ")");

        DbInfoDto schema = DbInfoExtractor.extract(getConnection());
        TableDto child = schema.tables.stream().filter(t -> t.id.name.equalsIgnoreCase("ChildTwoImplicit")).findAny().get();

        assertEquals(2, child.foreignKeys.size());

        ForeignKeyDto fk1 = child.foreignKeys.stream()
                .filter(fk -> fk.targetTable.equalsIgnoreCase("Parent1"))
                .findFirst().get();

        assertEquals(2, fk1.sourceColumns.size());
        assertTrue(fk1.sourceColumns.get(0).equalsIgnoreCase("p1_id1"));
        assertTrue(fk1.sourceColumns.get(1).equalsIgnoreCase("p1_id2"));
        assertEquals(2, fk1.targetColumns.size());
        assertTrue(fk1.targetColumns.get(0).equalsIgnoreCase("id1"));
        assertTrue(fk1.targetColumns.get(1).equalsIgnoreCase("id2"));

        ForeignKeyDto fk2 = child.foreignKeys.stream()
                .filter(fk -> fk.targetTable.equalsIgnoreCase("Parent2"))
                .findFirst().get();

        assertEquals(2, fk2.sourceColumns.size());
        assertTrue(fk2.sourceColumns.get(0).equalsIgnoreCase("p2_id1"));
        assertTrue(fk2.sourceColumns.get(1).equalsIgnoreCase("p2_id2"));
        assertEquals(2, fk2.targetColumns.size());
        assertTrue(fk2.targetColumns.get(0).equalsIgnoreCase("id1"));
        assertTrue(fk2.targetColumns.get(1).equalsIgnoreCase("id2"));
    }

}
