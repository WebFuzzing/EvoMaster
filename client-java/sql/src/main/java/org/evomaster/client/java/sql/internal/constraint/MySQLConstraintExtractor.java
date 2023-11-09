package org.evomaster.client.java.sql.internal.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private static final String MYSQL_CONSTRAINT_NAME = "CONSTRAINT_NAME";
    private static final String MYSQL_CONSTRAINT_TYPE = "CONSTRAINT_TYPE";
    private static final String MYSQL_CHECK_CLAUSE = "CHECK_CLAUSE";
    private static final String MYSQL_COLUMN_NAME = "COLUMN_NAME";

    private static final String MYSQL_ENUM_COLUMN_TYPE = "COLUMN_TYPE";


    private static void cannotHandle(String constraintType) {
        SimpleLogger.uniqueWarn("WARNING, EvoMaster cannot extract MySQL constraints with type '" + constraintType);
    }

    @Override
    public List<DbTableConstraint> extract(Connection connectionToMySQL, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        List<DbTableConstraint> constraints = new ArrayList<>();

        for (TableDto tableDto : schemaDto.tables){
            String tableName = tableDto.name;
            try (Statement statement = connectionToMySQL.createStatement()) {
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
                            case MYSQL_CONSTRAINT_TYPE_UNIQUE:
                                String uniqueConstraintName = columns.getString(MYSQL_CONSTRAINT_NAME);
                                DbTableUniqueConstraint uniqueConstraint = getUniqueConstraint(connectionToMySQL, tableSchema, tableName, uniqueConstraintName);
                                constraints.add(uniqueConstraint);
                                break;
                            default:
                                cannotHandle("Unknown constraint type " + type);
                        }
                    }
                }
            }

            // handle enum column
            for (ColumnDto column: tableDto.columns){
                if (column.type.equalsIgnoreCase("enum")){
                    DbTableCheckExpression enumConstraint = handleEnum(connectionToMySQL, tableSchema, tableName, column.name);
                    constraints.add(enumConstraint);
                }
            }
        }

        return constraints;
    }

    private DbTableUniqueConstraint getUniqueConstraint(Connection connectionToMySQL, String tableSchema, String tableName, String constraintName) throws SQLException{
        String query = String.format("SELECT %s \n" +
                "   FROM information_schema.KEY_COLUMN_USAGE\n" +
                "   WHERE TABLE_SCHEMA = '%s'\n" +
                "       AND TABLE_NAME = '%s'\n" +
                "       AND CONSTRAINT_NAME='%s';\n", MYSQL_COLUMN_NAME, tableSchema, tableName, constraintName);

        try (Statement stmt = connectionToMySQL.createStatement()) {
            try (ResultSet columns = stmt.executeQuery(query)) {
                List<String> uniqueColumnNames = new ArrayList<>();
                while(columns.next()){
                    uniqueColumnNames.add(columns.getString(MYSQL_COLUMN_NAME));
                }
                if (uniqueColumnNames.isEmpty()) {
                    throw new IllegalStateException("Unexpected missing column names");
                }
                return new DbTableUniqueConstraint(tableName, uniqueColumnNames);
            }
        }
    }

    private DbTableCheckExpression getCheckConstraint(Connection connectionToMySQL, String tableName, String constraintName) throws SQLException{
        String query = String.format("SELECT %s \n" +
                "FROM information_schema.CHECK_CONSTRAINTS\n" +
                "WHERE CONSTRAINT_NAME='%s';\n", MYSQL_CHECK_CLAUSE, constraintName);

        try (Statement stmt = connectionToMySQL.createStatement()) {
            try (ResultSet check = stmt.executeQuery(query)) {
                boolean hasChecks = check.next();
                if (!hasChecks) {
                    throw new IllegalStateException("Unexpected missing check scripts");
                }
                String check_clause = postCheckConstraintHandling(check.getString(MYSQL_CHECK_CLAUSE));
                return new DbTableCheckExpression(tableName, check_clause);
            }
        }
    }

    // Man: Shall I move this into TableCheckExpressionDto e.g., formatCheckExpression
    private String postCheckConstraintHandling(String check_exp){
        return check_exp
                .replaceAll("`", "")
                .replaceAll("_utf8mb4", "") // mysql https://dev.mysql.com/doc/refman/8.0/en/charset-unicode-utf8mb4.html
                .replaceAll("\\\\'","'");
    }

    private DbTableCheckExpression handleEnum(Connection connectionToMySQL, String schemaName, String tableName, String columnName) throws SQLException{
        String query = String.format("SELECT %s\n" +
                "       FROM information_schema.COLUMNS\n" +
                "       WHERE TABLE_SCHEMA='%s'\n" +
                "           AND TABLE_NAME='%s'\n" +
                "           AND COLUMN_NAME='%s';", MYSQL_ENUM_COLUMN_TYPE, schemaName, tableName, columnName);

        try (Statement stmt = connectionToMySQL.createStatement()) {
            try (ResultSet literals = stmt.executeQuery(query)) {
                boolean hasLiterals = literals.next();
                if (!hasLiterals) {
                    throw new IllegalStateException("Unexpected missing literals of enum");
                }
                String literalsValue = literals.getString(MYSQL_ENUM_COLUMN_TYPE);
                return new DbTableCheckExpression(tableName, String.format(
                        "%s %s", columnName, literalsValue
                ));
            }
        }

    }
}
