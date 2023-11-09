package org.evomaster.client.java.sql.distance.advanced.driver;

import org.apache.commons.dbutils.QueryRunner;
import org.evomaster.client.java.sql.distance.advanced.driver.cache.Cache;
import org.evomaster.client.java.sql.distance.advanced.driver.cache.NoCache;
import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;
import org.evomaster.client.java.sql.distance.advanced.driver.row.RowHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;

public class SqlDriver {

    private Connection connection;
    private Cache cache;

    private SqlDriver(Connection connection, Cache cache) {
        this.connection = connection;
        this.cache = cache;
    }

    public static SqlDriver createSqlDriver(Connection connection, Cache cache) {
        return new SqlDriver(connection, cache);
    }

    public static SqlDriver createSqlDriver(String url, String username, String password) {
        try {
            return createSqlDriver(DriverManager.getConnection(url, username, password), new NoCache());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(String sql) {
        try {
            SimpleLogger.debug(format("Executing: %s", sql));
            QueryRunner queryRunner = new QueryRunner();
            queryRunner.execute(connection, sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Row> query(String sql) {
        try {
            List<Row> result;
            if(cache.isCached(sql)) {
                SimpleLogger.debug(format("Query: %s was cached", sql));
                result = cache.get(sql);
            } else {
                SimpleLogger.debug(format("Querying: %s", sql));
                QueryRunner queryRunner = new QueryRunner();
                result = queryRunner.query(connection, sql, new RowHandler());
                cache.cache(sql, result);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
