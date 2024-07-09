package org.evomaster.client.java.sql.advanced.driver;

import org.apache.commons.dbutils.QueryRunner;
import org.evomaster.client.java.sql.advanced.driver.cache.Cache;
import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.advanced.driver.Schema.createSchema;
import static org.evomaster.client.java.sql.advanced.driver.row.RowHandler.createRowHandler;

public class SqlDriver {

    private Connection connection;
    private Schema schema;
    private Cache cache;

    private SqlDriver(Connection connection, Schema schema, Cache cache) {
        this.connection = connection;
        this.schema = schema;
        this.cache = cache;
    }

    public static SqlDriver createSqlDriver(Connection connection, Schema schema, Cache cache) {
        return new SqlDriver(connection, schema, cache);
    }

    public static SqlDriver createSqlDriver(Connection connection, Cache cache) {
        return createSqlDriver(connection, createSchema(connection), cache);
    }

    public void execute(String sql) {
        try {
            SimpleLogger.trace(format("Executing: %s", sql));
            QueryRunner queryRunner = new QueryRunner();
            queryRunner.execute(connection, sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Row> query(String query) {
        try {
            List<Row> result;
            if(cache.isCached(query)) {
                SimpleLogger.trace(format("Query: %s was cached", query));
                result = cache.get(query);
            } else {
                SimpleLogger.trace(format("Querying: %s", query));
                QueryRunner queryRunner = new QueryRunner();
                result = queryRunner.query(connection, query, createRowHandler());
                cache.cache(query, result);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }

    public Cache getCache(){
        return cache;
    }

    public void setCache(Cache cache){
        this.cache = cache;
    }

    @Override
    public String toString() {
        return "Cache:\n" + cache + "\nSchema:\n" + schema;
    }
}
