package org.evomaster.client.java.controller.internal.db.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PostgresTableConstraintExtractor extends TableConstraintExtractor {

    public static final String CONSTRAINT_TYPE_CHECK = "c";

    public static final String CONSTRAINT_TYPE_FOREIGN_KEY = "f";

    public static final String CONSTRAINT_TYPE_PRIMARY_KEY = "p";

    public static final String CONSTRAINT_TYPE_UNIQUE = "u";

    public static final String CONSTRAINT_TYPE_TRIGGER = "t";

    public static final String CONSTRAINT_TYPE_EXCLUSION = "x";


    public List<DbTableConstraint> extract(Connection connectionToPostgres, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        List<DbTableConstraint> constraints = new ArrayList<>();
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            String query = "SELECT con.*\n" +
                    "       FROM pg_catalog.pg_constraint con\n" +
                    "            INNER JOIN pg_catalog.pg_class rel\n" +
                    "                       ON rel.oid = con.conrelid\n" +
                    "            INNER JOIN pg_catalog.pg_namespace nsp\n" +
                    "                       ON nsp.oid = connamespace\n" +
                    "       WHERE nsp.nspname = '" + tableSchema + "'\n" +
                    "             AND rel.relname = '" + tableName + "';";

            Statement statement = connectionToPostgres.createStatement();
            ResultSet columns = statement.executeQuery(query);
            while (columns.next()) {
                String constraintType = columns.getString("contype");
                switch (constraintType) {
                    case CONSTRAINT_TYPE_CHECK: {
                        String checkConstraint = columns.getString("consrc");
                        if (checkConstraint != null && !checkConstraint.equals("")) {

                            DbTableCheckExpression tableCheckExpression = new DbTableCheckExpression(tableName, checkConstraint);
                            constraints.add(tableCheckExpression);

                        }
                        break;
                    }
                    case CONSTRAINT_TYPE_FOREIGN_KEY:
                    case CONSTRAINT_TYPE_PRIMARY_KEY:
                    case CONSTRAINT_TYPE_UNIQUE:
                    case CONSTRAINT_TYPE_TRIGGER:
                    case CONSTRAINT_TYPE_EXCLUSION: {
                        // ignore
                    }
                }

            }

            statement.close();
        }
        return constraints;
    }
}
