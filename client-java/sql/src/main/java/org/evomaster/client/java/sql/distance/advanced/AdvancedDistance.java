package org.evomaster.client.java.sql.distance.advanced;

import org.evomaster.client.java.sql.distance.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.distance.advanced.query_distance.Distance;
import org.evomaster.client.java.sql.distance.advanced.query_distance.QueryDistanceCalculator;
import org.evomaster.client.java.utils.SimpleLogger;

import static java.lang.Double.MAX_VALUE;
import static java.lang.String.format;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.QueryDistanceCalculator.createQueryDistanceCalculator;

public class AdvancedDistance {

    private SqlDriver sqlDriver;

    public AdvancedDistance(SqlDriver sqlDriver) {
        this.sqlDriver = sqlDriver;
    }

    public Double calculate(String query) {
        try {
            QueryDistanceCalculator queryDistanceCalculator = createQueryDistanceCalculator(query, sqlDriver);
            Distance distance = queryDistanceCalculator.calculate();
            return distance.getValue();
        } catch (Exception e) {
            SimpleLogger.error(format("Error occurred while calculating distance for query: %s", query), e);
            return MAX_VALUE;
        }
    }
}
