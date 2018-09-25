package org.evomaster.clientJava.controller.internal.db;

import org.evomaster.clientJava.controller.db.DatabaseTestTemplate;
import org.evomaster.clientJava.controller.db.SqlScriptRunner;
import org.evomaster.clientJava.controllerApi.dto.database.schema.DatabaseType;
import org.evomaster.clientJava.controllerApi.dto.database.schema.DbSchemaDto;
import org.junit.jupiter.api.Test;

import static org.evomaster.clientJava.controller.internal.db.TableDtoUtils.containsTable;
import static org.evomaster.clientJava.controller.internal.db.TableDtoUtils.getTable;
import static org.junit.jupiter.api.Assertions.*;

public class NewsSchemaExtractorTest extends DatabaseTestTemplate {


    @Test
    public void testCreateAndExtractSchema() throws Exception {

        String command = "create sequence hibernate_sequence start with 1 increment by 1;" +
                "create table news_entity (id bigint not null, author_id varchar(32) not null, country varchar(255) not null, creation_time timestamp not null, text varchar(1024) not null, primary key (id));";

        SqlScriptRunner.execCommand(getConnection(), command);

        DbSchemaDto schema = SchemaExtractor.extract(getConnection());
        assertNotNull(schema);

        assertAll(() -> assertEquals("public", schema.name.toLowerCase()),
                () -> assertEquals(DatabaseType.H2, schema.databaseType),
                () -> assertEquals(1, schema.tables.size()),
                () -> assertTrue(containsTable(schema.tables, "news_entity")),
                () -> assertEquals(5, getTable(schema.tables, "news_entity").columns.size())
        );


    }


}