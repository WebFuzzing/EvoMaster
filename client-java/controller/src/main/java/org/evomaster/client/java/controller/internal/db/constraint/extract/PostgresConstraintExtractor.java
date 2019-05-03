package org.evomaster.client.java.controller.internal.db.constraint.extract;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.internal.db.constraint.TableConstraint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

public class PostgresConstraintExtractor extends ConstraintExtractor {

    public static final String CONSTRAINT_TYPE_CHECK = "c";

    public static final String CONSTRAINT_TYPE_FOREIGN_KEY = "f";

    public static final String CONSTRAINT_TYPE_PRIMARY_KEY = "p";

    public static final String CONSTRAINT_TYPE_UNIQUE = "u";

    public static final String CONSTRAINT_TYPE_TRIGGER = "t";

    public static final String CONSTRAINT_TYPE_EXCLUSION = "x";


    public List<TableConstraint> extractConstraints(Connection connectionToPostgres, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        List<TableConstraint> constraints = new LinkedList<>();
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

                            checkConstraint = checkConstraint.replace("::text", "");
                            List<TableConstraint> tableConstraints = this.translateToConstraints(tableDto, checkConstraint);
                            constraints.addAll(tableConstraints);

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
