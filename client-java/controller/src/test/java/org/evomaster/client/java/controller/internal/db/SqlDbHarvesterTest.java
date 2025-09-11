package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseFakeMySQLSutController;
import org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseMySQLTestInit;
import org.evomaster.client.java.sql.DbInfoExtractor;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.internal.SqlDbHarvester;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlDbHarvesterTest extends DatabaseMySQLTestInit implements DatabaseTestTemplate {

    @BeforeAll
    public static void createTable() throws Exception {
        SqlScriptRunner.runScriptFromResourceFile(connection, "/db_schemas/order.sql");
        SqlScriptRunner.runScriptFromResourceFile(connection, "/db_schemas/order_init.sql");
    }

    @Test
    public void testInitTableSize() throws Exception{
        DbInfoDto dbInfoDto = DbInfoExtractor.extract(getConnection());
        assertEquals(dbInfoDto.tables.size(), 10);

    }

    @Test
    public void testSortTablesByDependency() throws Exception{
        DbInfoDto dbInfoDto = DbInfoExtractor.extract(getConnection());
        List<TableDto> tables = SqlDbHarvester.sortTablesByDependency(dbInfoDto);
        Arrays.asList("coupons","categories","users","products").stream().allMatch(
                s -> tables.subList(0,4).contains(s)
        );
        Arrays.asList("orders","user_addresses").stream().allMatch(
                s -> tables.subList(4,6).contains(s)
        );

        Arrays.asList("user_coupons","reviews","order_items","payments").stream().allMatch(
                s -> tables.subList(6,10).contains(s)
        );
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
