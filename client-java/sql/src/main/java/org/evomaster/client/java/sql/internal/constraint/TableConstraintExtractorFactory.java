package org.evomaster.client.java.sql.internal.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

/**
 * This factory returns the corresponding TableConstraintExtractor for a given database.
 * As constraints are handled differently by each database implementation, it is required
 * to provide a suitable procedure for extracting those constraints.
 */
public class TableConstraintExtractorFactory {

    private TableConstraintExtractorFactory() {
    }

    public static TableConstraintExtractor buildConstraintExtractor(DatabaseType dt) {
        switch (dt) {
            case H2:
                return new H2ConstraintExtractor();
            case POSTGRES:
                return new PostgresConstraintExtractor();
            case MYSQL:
                return new MySQLConstraintExtractor();
            case DERBY:
            case OTHER:
                return null;
            default:
                throw new IllegalArgumentException("Unknown database type " + dt);

        }

    }

}
