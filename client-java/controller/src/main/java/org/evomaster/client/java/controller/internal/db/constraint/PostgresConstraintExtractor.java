package org.evomaster.client.java.controller.internal.db.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PostgresConstraintExtractor extends TableConstraintExtractor {

    private static final String CONSTRAINT_TYPE_CHECK = "c";

    private static final String CONSTRAINT_TYPE_FOREIGN_KEY = "f";

    private static final String CONSTRAINT_TYPE_PRIMARY_KEY = "p";

    private static final String CONSTRAINT_TYPE_UNIQUE = "u";

    private static final String CONSTRAINT_TYPE_TRIGGER = "t";

    private static final String CONSTRAINT_TYPE_EXCLUSION = "x";

    private static final String CONTYPE = "contype";

    private static final String CONSRC = "consrc";


    /**
     * Logs that a constraint could not be handled by the extractor.
     *
     * @param constraintType
     */
    private static void cannotHandle(String constraintType) {
        SimpleLogger.uniqueWarn("WARNING, EvoMaster cannot extract Postgres constraints with type '" + constraintType);
    }

    public List<DbTableConstraint> extract(Connection connectionToPostgres, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        List<DbTableConstraint> constraints = new ArrayList<>();
        for (TableDto tableDto : schemaDto.tables) {
            try (Statement statement = connectionToPostgres.createStatement()) {
                String tableName = tableDto.name;
                String query = String.format("SELECT con.*\n" +
                        "       FROM pg_catalog.pg_constraint con\n" +
                        "            INNER JOIN pg_catalog.pg_class rel\n" +
                        "                       ON rel.oid = con.conrelid\n" +
                        "            INNER JOIN pg_catalog.pg_namespace nsp\n" +
                        "                       ON nsp.oid = connamespace\n" +
                        "       WHERE nsp.nspname = '%s'\n" +
                        "             AND rel.relname = '%s';", tableSchema, tableName);

                try (ResultSet columns = statement.executeQuery(query)) {

                    while (columns.next()) {
                        String constraintType = columns.getString(CONTYPE);
                        switch (constraintType) {
                            case CONSTRAINT_TYPE_CHECK: {
                                String checkConstraint = columns.getString(CONSRC);
                                DbTableCheckExpression tableCheckExpression = new DbTableCheckExpression(tableName, checkConstraint);
                                constraints.add(tableCheckExpression);
                            }
                            break;
                            case CONSTRAINT_TYPE_UNIQUE: {
                                Array array = columns.getArray("conkey");
                                Integer[] columnIds = (Integer[]) array.getArray();
                                List<String> uniqueColumnNames = new ArrayList<>();
                                for (int columnId : columnIds) {
                                    String qry = String.format("SELECT att.* " +
                                            " FROM pg_catalog.pg_attribute att " +
                                            " INNER JOIN pg_catalog.pg_class rel\n " +
                                            "    ON rel.oid = att.attrelid\n " +
                                            " INNER JOIN pg_catalog.pg_namespace nsp\n " +
                                            "    ON nsp.oid = rel.relnamespace\n " +
                                            " WHERE nsp.nspname = '%s'\n" +
                                            "   AND rel.relname = '%s' \n" +
                                            "   AND att.attnum =  %s;", tableSchema, tableName, columnId);

                                    try (Statement stmt = connectionToPostgres.createStatement()) {
                                        try (ResultSet rs = stmt.executeQuery(qry)) {
                                            if (rs.next()) {
                                                String uniqueColumnName = rs.getString("attname");
                                                uniqueColumnNames.add(uniqueColumnName);
                                            } else {

                                            }
                                        }
                                    }
                                }
                                DbTableUniqueConstraint uniqueConstraint = new DbTableUniqueConstraint(tableName, uniqueColumnNames);
                                constraints.add(uniqueConstraint);
                            }
                            break;
                            case CONSTRAINT_TYPE_FOREIGN_KEY:
                            case CONSTRAINT_TYPE_PRIMARY_KEY:
                                /**
                                 * These types of constraints are already handled by
                                 * JDBC Metadata
                                 **/
                                break;

                            case CONSTRAINT_TYPE_TRIGGER:
                                cannotHandle("TRIGGER CONSTRAINT");
                                break;

                            case CONSTRAINT_TYPE_EXCLUSION:
                                cannotHandle("EXCLUSION CONSTRAINT");
                                break;

                            default:
                                cannotHandle("Unknown constraint type " + constraintType);
                                break;

                        }
                    }
                }
            }
        }
        return constraints;
    }

}
