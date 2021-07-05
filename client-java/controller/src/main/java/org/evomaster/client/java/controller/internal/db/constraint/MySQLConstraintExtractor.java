package org.evomaster.client.java.controller.internal.db.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLConstraintExtractor extends TableConstraintExtractor{

    /*
        DEFAULT
     */
    private static final String MYSQL_CONSTRAINT_TYPE_CHECK = "CHECK";

    private static final String MYSQL_CONSTRAINT_TYPE_FOREIGN_KEY = "FOREIGN KEY";

    private static final String MYSQL_CONSTRAINT_TYPE_PRIMARY_KEY = "PRIMARY KEY";

    private static final String MYSQL_CONSTRAINT_TYPE_UNIQUE = "UNIQUE";

    private static final String MYSQL_CONSTRAINT_TYPE_NOT_NULL = "NOT NULL";

    private static final String MYSQL_CONSTRAINT_NAME = "CONSTRAINT_NAME";
    private static final String MYSQL_CONSTRAINT_TYPE = "CONSTRAINT_TYPE";
    private static final String MYSQL_CHECK_CLAUSE = "CHECK_CLAUSE";


    private static void cannotHandle(String constraintType) {
        SimpleLogger.uniqueWarn("WARNING, EvoMaster cannot extract MySQL constraints with type '" + constraintType);
    }

    @Override
    public List<DbTableConstraint> extract(Connection connectionToMySQL, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        List<DbTableConstraint> constraints = new ArrayList<>();

        for (TableDto tableDto : schemaDto.tables){
            try (Statement statement = connectionToMySQL.createStatement()) {
                String tableName = tableDto.name;
                String query = String.format("SELECT *\n" +
                        "       FROM information_schema.table_constraints\n" +
                        "       WHERE table_schema = '%s'\n" +
                        "             AND table_name = '%s';", tableSchema, tableName);
                try (ResultSet columns = statement.executeQuery(query)) {
                    while (columns.next()) {
                        String type = columns.getString(MYSQL_CONSTRAINT_TYPE);
                        switch (type){
                            case MYSQL_CONSTRAINT_TYPE_PRIMARY_KEY:
                            case MYSQL_CONSTRAINT_TYPE_FOREIGN_KEY:
                                break;
                            case MYSQL_CONSTRAINT_TYPE_CHECK:
                                String constraintName = columns.getString(MYSQL_CONSTRAINT_NAME);
                                DbTableCheckExpression check = getCheckConstraint(connectionToMySQL, tableName, constraintName);
                                constraints.add(check);
                                break;
                            case MYSQL_CONSTRAINT_TYPE_NOT_NULL:
                                //TODO
                                cannotHandle(type);
                            case MYSQL_CONSTRAINT_TYPE_UNIQUE:
                                //TODO
                                cannotHandle(type);
                            default:
                                cannotHandle("Unknown constraint type " + type);
                        }
                    }
                }
            }
        }

        return constraints;
    }

    private DbTableCheckExpression getCheckConstraint(Connection connectionToMySQL, String tableName, String constraintName) throws SQLException{
        String query = String.format("SELECT %s \n" +
                "FROM information_schema.CHECK_CONSTRAINTS\n" +
                "WHERE CONSTRAINT_NAME='%s';\n", MYSQL_CHECK_CLAUSE, constraintName);

        try (Statement stmt = connectionToMySQL.createStatement()) {
            try (ResultSet check = stmt.executeQuery(query)) {
                boolean hasChecks = check.next();
                if (!hasChecks) {
                    throw new IllegalStateException("Unexpected missing pg_catalog.pg_attribute data");
                }
                String check_clause = check.getString(MYSQL_CHECK_CLAUSE);
                return new DbTableCheckExpression(tableName, check_clause);
            }
        }
    }
}
