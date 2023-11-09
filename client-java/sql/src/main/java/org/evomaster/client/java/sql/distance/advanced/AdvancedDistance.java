package org.evomaster.client.java.sql.distance.advanced;

import org.evomaster.client.java.sql.distance.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.distance.advanced.query_distance.Distance;
import org.evomaster.client.java.sql.distance.advanced.query_distance.QueryDistanceCalculator;
import org.evomaster.client.java.utils.SimpleLogger;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.Distance.INF_DISTANCE;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.QueryDistanceCalculator.createQueryDistanceCalculator;

public class AdvancedDistance {

    private SqlDriver sqlDriver;

    public AdvancedDistance(SqlDriver sqlDriver) {
        this.sqlDriver = sqlDriver;
    }

    public Distance calculate(String query) {
        try {
            QueryDistanceCalculator queryDistanceCalculator = createQueryDistanceCalculator(query, sqlDriver);
            return queryDistanceCalculator.calculate();
        } catch (Exception e) {
            SimpleLogger.error(format(
                "Error occurred while calculating distance for query: %s and state: %s", query, sqlDriver.dump()), e);
            return INF_DISTANCE;
        }
    }
}
