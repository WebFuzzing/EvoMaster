package org.evomaster.client.java.sql.internal.constraint;


import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.h2.H2VersionUtils;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class H2ConstraintExtractor extends TableConstraintExtractor {

    private static final String CONSTRAINT_TYPE = "CONSTRAINT_TYPE";
    private static final String CHECK_EXPRESSION = "CHECK_EXPRESSION";
    private static final String COLUMN_LIST = "COLUMN_LIST";
    private static final String UNIQUE = "UNIQUE";
    private static final String REFERENTIAL = "REFERENTIAL";
    private static final String PRIMARY_KEY = "PRIMARY_KEY";
    private static final String PRIMARY_KEY_BLANK = "PRIMARY KEY";
    private static final String CHECK = "CHECK";
    private static final String CHECK_CONSTRAINT = "CHECK_CONSTRAINT";
    private static final String CONSTRAINT_CATALOG = "CONSTRAINT_CATALOG";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String CONSTRAINT_SCHEMA = "CONSTRAINT_SCHEMA";
    private static final String CONSTRAINT_NAME = "CONSTRAINT_NAME";

    /**
     * Expects the schema explained in
     * http://www.h2database.com/html/systemtables.html#information_schema
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto      a DTO schema with retrieved information from the JBDC metadata
     * @throws SQLException if the connection to the H2 database fails
     */
    public List<DbTableConstraint> extract(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {

        final String h2DatabaseVersion = H2VersionUtils.getH2Version(connectionToH2);

        List<DbTableConstraint> columnConstraints = extractColumnConstraints(connectionToH2, schemaDto, h2DatabaseVersion);
        List<DbTableConstraint> tableCheckExpressions = extractTableConstraints(connectionToH2, schemaDto, h2DatabaseVersion);

        List<DbTableConstraint> allConstraints = new ArrayList<>();
        allConstraints.addAll(columnConstraints);
        allConstraints.addAll(tableCheckExpressions);
        return allConstraints;
    }

    /**
     * Logs that a constraint could not be handled by the extractor.
     *
     * @param constraintType the type of SQL constraint
     */
    private static void cannotHandle(String constraintType) {
        SimpleLogger.uniqueWarn("WARNING, EvoMaster cannot extract H2 constraints with type '" + constraintType);
    }

    private List<DbTableConstraint> extractColumnConstraints(Connection connectionToH2, DbSchemaDto schemaDto, String h2DatabaseVersion) throws SQLException {
        if (H2VersionUtils.isVersionGreaterOrEqual(h2DatabaseVersion, H2VersionUtils.H2_VERSION_2_0_0)) {
            return new ArrayList<>();
        } else {
            return extractColumnConstraintsVersion1OrLower(connectionToH2, schemaDto, h2DatabaseVersion);
        }
    }

    private List<DbTableConstraint> extractTableConstraints(Connection connectionToH2, DbSchemaDto schemaDto, String h2DatabaseVersion) throws SQLException {
        if (H2VersionUtils.isVersionGreaterOrEqual(h2DatabaseVersion, H2VersionUtils.H2_VERSION_2_0_0)) {
            return extractTableConstraintsVersionTwoOrHigher(connectionToH2, schemaDto);
        } else {
            return extractTableConstraintsVersionOneOrLower(connectionToH2, schemaDto);
        }
    }


    /**
     * For each table in the schema DTO, this method appends
     * the constraints that are originated in the ALTER TABLE commands
     * for those particular tables.
     * <p>
     * Foreign keys are handled separately in the JDBC metadata
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto      DTO with database schema information
     * @throws SQLException if the connection to the H2 database fails
     */
    private List<DbTableConstraint> extractTableConstraintsVersionTwoOrHigher(Connection connectionToH2,
                                                                              DbSchemaDto schemaDto) throws SQLException {
        List<DbTableConstraint> tableCheckExpressions = new ArrayList<>();

        String tableSchema = schemaDto.name;
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            try (Statement statement = connectionToH2.createStatement()) {
                final String query = String.format("Select CONSTRAINT_CATALOG,CONSTRAINT_SCHEMA,CONSTRAINT_NAME,CONSTRAINT_TYPE From INFORMATION_SCHEMA.TABLE_CONSTRAINTS\n" +
                        " where TABLE_CONSTRAINTS.TABLE_SCHEMA='%s' \n"
                        + " and TABLE_CONSTRAINTS.TABLE_NAME='%s' ", tableSchema, tableName);
                try (ResultSet constraints = statement.executeQuery(query)) {
                    while (constraints.next()) {
                        String constraintCatalog = constraints.getString(CONSTRAINT_CATALOG);
                        String constraintSchema = constraints.getString(CONSTRAINT_SCHEMA);
                        String constraintName = constraints.getString(CONSTRAINT_NAME);
                        String constraintType = constraints.getString(CONSTRAINT_TYPE);
                        switch (constraintType) {
                            case UNIQUE: {
                                DbTableUniqueConstraint constraint = getTableUniqueConstraint(connectionToH2, tableName,
                                        constraintCatalog, constraintSchema, constraintName);
                                tableCheckExpressions.add(constraint);
                                break;
                            }
                            case CHECK: {
                                DbTableCheckExpression constraint = getTableCheckExpression(connectionToH2, tableName, constraintCatalog, constraintSchema, constraintName);
                                tableCheckExpressions.add(constraint);
                                break;
                            }
                            case PRIMARY_KEY:
                            case PRIMARY_KEY_BLANK:
                            case REFERENTIAL:
                                /*
                                 * This type of constraint is already handled by
                                 * JDBC Metadata
                                 **/
                                break;
                            default:
                                cannotHandle(constraintType);
                        }
                    }
                }
            }
        }

        return tableCheckExpressions;
    }

    private DbTableUniqueConstraint getTableUniqueConstraint(Connection connectionToH2,
                                                             String tableName,
                                                             String constraintCatalog,
                                                             String constraintSchema,
                                                             String constraintName) throws SQLException {

        try (Statement columnsUsageStatement = connectionToH2.createStatement()) {
            String columnsUsageQuery = String.format("SELECT TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE "
                            + "WHERE CONSTRAINT_CATALOG='%s' AND CONSTRAINT_SCHEMA='%s' AND CONSTRAINT_NAME='%s' ",
                    constraintCatalog, constraintSchema, constraintName);
            try (ResultSet columnsUsageResultSet = columnsUsageStatement.executeQuery(columnsUsageQuery)) {
                List<String> uniqueColumnNames = new ArrayList<>();
                while (columnsUsageResultSet.next()) {
                    String columnName = columnsUsageResultSet.getString(COLUMN_NAME);
                    uniqueColumnNames.add(columnName);
                }
                return new DbTableUniqueConstraint(tableName, uniqueColumnNames);
            }
        }
    }


    private DbTableCheckExpression getTableCheckExpression(Connection connectionToH2,
                                                           String tableName,
                                                           String constraintCatalog,
                                                           String constraintSchema,
                                                           String constraintName) throws SQLException {

        try (Statement checkClauseStatement = connectionToH2.createStatement()) {
            String checkClauseQuery = String.format("SELECT CHECK_CLAUSE FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS "
                            + "WHERE CONSTRAINT_CATALOG='%s' AND CONSTRAINT_SCHEMA='%s' AND CONSTRAINT_NAME='%s' ",
                    constraintCatalog, constraintSchema, constraintName);
            try (ResultSet checkClauseResultSet = checkClauseStatement.executeQuery(checkClauseQuery)) {
                if (checkClauseResultSet.next()) {
                    String sqlCheckExpression = checkClauseResultSet.getString("CHECK_CLAUSE");
                    return new DbTableCheckExpression(tableName, "(" + sqlCheckExpression + ")");
                } else {
                    throw new IllegalArgumentException(String.format("Cannot find constraint such that CONSTRAINT_CATALOG='%s' AND CONSTRAINT_SCHEMA='%s' AND CONSTRAINT_NAME='%s' ", constraintCatalog, constraintSchema, constraintName));
                }
            }
        }
    }

    /**
     * Extracts DbTableConstraints for version 0.xxx and 1.xxx
     *
     * @param connectionToH2 the connection to the H2 database
     * @param schemaDto      the schema
     * @return the list of constraints in the table
     * @throws SQLException an unexpected exception while executing the queries
     */
    private List<DbTableConstraint> extractTableConstraintsVersionOneOrLower(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {
        List<DbTableConstraint> tableCheckExpressions = new ArrayList<>();
        String tableSchema = schemaDto.name;
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            try (Statement statement = connectionToH2.createStatement()) {
                final String query = String.format("Select CONSTRAINT_TYPE, CHECK_EXPRESSION, COLUMN_LIST From INFORMATION_SCHEMA.CONSTRAINTS\n" +
                        " where CONSTRAINTS.TABLE_SCHEMA='%s' \n"
                        + " and CONSTRAINTS.TABLE_NAME='%s' ", tableSchema, tableName);
                try (ResultSet constraints = statement.executeQuery(query)) {

                    while (constraints.next()) {
                        String constraintType = constraints.getString(CONSTRAINT_TYPE);
                        DbTableConstraint constraint;
                        switch (constraintType) {
                            case UNIQUE: {
                                String columnList = constraints.getString(COLUMN_LIST);
                                List<String> uniqueColumnNames = Arrays.stream(columnList.split(",")).map(String::trim).collect(Collectors.toList());
                                constraint = new DbTableUniqueConstraint(tableName, uniqueColumnNames);
                                tableCheckExpressions.add(constraint);
                                break;
                            }
                            case PRIMARY_KEY:
                            case PRIMARY_KEY_BLANK:
                            case REFERENTIAL:
                                /*
                                 * This type of constraint is already handled by
                                 * JDBC Metadata
                                 **/
                                break;
                            case CHECK:
                                String sqlCheckExpression = constraints.getString(CHECK_EXPRESSION);
                                constraint = new DbTableCheckExpression(tableName, sqlCheckExpression);
                                tableCheckExpressions.add(constraint);
                                break;

                            default:
                                cannotHandle(constraintType);
                        }
                    }
                }
            }
        }

        return tableCheckExpressions;
    }


    /**
     * For each table in the schema DTO, this method appends
     * the constraints that are originated in the CREATE TABLE commands
     * for those particular tables.
     * <p>
     * Unique constraints and Foreign keys are handled separately in the JDBC metadata
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto      DTO with database schema information
     * @throws SQLException if the connection to the database fails
     */
    private List<DbTableConstraint> extractColumnConstraintsVersion1OrLower(Connection connectionToH2, DbSchemaDto schemaDto, String h2DatabaseVersion) throws SQLException {
        if (H2VersionUtils.isVersionGreaterOrEqual(h2DatabaseVersion, H2VersionUtils.H2_VERSION_2_0_0)) {
            throw new IllegalArgumentException("Cannot extract column constraints for H2 version 2 or higher with H2 database version  " + h2DatabaseVersion);
        }
        String tableSchema = schemaDto.name;
        List<DbTableConstraint> columnConstraints = new ArrayList<>();
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;

            try (Statement statement = connectionToH2.createStatement()) {


                final String query = String.format("Select * From INFORMATION_SCHEMA.COLUMNS where COLUMNS.TABLE_SCHEMA='%s' and COLUMNS.TABLE_NAME='%s' ", tableSchema, tableName);

                try (ResultSet columns = statement.executeQuery(query)) {
                    while (columns.next()) {
                        String sqlCheckExpression = columns.getString(CHECK_CONSTRAINT);
                        if (sqlCheckExpression != null && !sqlCheckExpression.equals("")) {
                            DbTableCheckExpression constraint = new DbTableCheckExpression(tableName, sqlCheckExpression);
                            columnConstraints.add(constraint);
                        }
                    }
                }
            }
        }
        return columnConstraints;
    }

}

