package org.evomaster.client.java.controller.internal.db.constraint.extract;


import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.internal.db.constraint.TableConstraint;
import org.evomaster.client.java.controller.internal.db.constraint.UniqueConstraint;
import org.evomaster.client.java.controller.internal.db.constraint.parser.SqlConditionParserException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class H2ConstraintExtractor extends ConstraintExtractor {

    /**
     * Expects the schema explained in
     * http://www.h2database.com/html/systemtables.html#information_schema
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto      a DTO schema with retrieved information from the JBDC metada
     * @throws Exception
     */
    public List<TableConstraint> extractConstraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {

        List<TableConstraint> columnConstraints = extractColumnConstraints(connectionToH2, schemaDto);

        List<TableConstraint> tableConstraints = extractTableConstraints(connectionToH2, schemaDto);

        List<TableConstraint> allConstraints = new LinkedList<>();
        allConstraints.addAll(columnConstraints);
        allConstraints.addAll(tableConstraints);
        return allConstraints;
    }

    /**
     * For each table in the schema DTO, this method appends
     * the constraints that are originated in the ALTER TABLE commands
     * for those particular tables.
     * <p>
     * Foreign keys are handled separately in the JDBC metadata
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto
     * @throws SQLException                if the connection to the H2 database fails,
     * @throws SqlConditionParserException if a conditional expression fails to be parsed
     */
    private List<TableConstraint> extractTableConstraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {

        List<TableConstraint> tableConstraints = new LinkedList<>();

        String tableSchema = schemaDto.name;
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            Statement statement = connectionToH2.createStatement();
            ResultSet constraints = statement.executeQuery("Select * From INFORMATION_SCHEMA.CONSTRAINTS "
                    + " where CONSTRAINTS.TABLE_SCHEMA='" + tableSchema + "' and CONSTRAINTS.TABLE_NAME='" + tableName + "'");

            while (constraints.next()) {
                String tableCatalog = constraints.getString("TABLE_CATALOG");
                String constraintCatalog = constraints.getString("CONSTRAINT_CATALOG");
                String constraintSchema = constraints.getString("CONSTRAINT_SCHEMA");
                String constraintName = constraints.getString("CONSTRAINT_NAME");
                String constraintType = constraints.getString("CONSTRAINT_TYPE");
                String uniqueIndexName = constraints.getString("UNIQUE_INDEX_NAME");
                String checkExpression = constraints.getString("CHECK_EXPRESSION");
                String columnList = constraints.getString("COLUMN_LIST");

                if (constraintType.equals("UNIQUE")) {
                    assert (checkExpression == null);
                    List<String> uniqueColumnNames = Arrays.stream(columnList.split(",")).map(String::trim).collect(Collectors.toList());
                    UniqueConstraint uniqueConstraint = new UniqueConstraint(tableName, uniqueColumnNames);
                    tableConstraints.add(uniqueConstraint);

                } else if (constraintType.equals("REFERENTIAL")) {
                    /**
                     * This type of constraint is already handled by
                     * JDBC Metadata
                     **/
                    continue;
                } else if (constraintType.equals("PRIMARY KEY") || constraintType.equals("PRIMARY_KEY")) {
                    /**
                     * This type of constraint is already handled by
                     * JDBC Metadata
                     **/
                    continue;
                } else if (constraintType.equals("CHECK")) {
                    assert (columnList == null);

                    TableConstraint checkConstraint = translateToConstraint(tableDto, checkExpression);
                    tableConstraints.add(checkConstraint);

                } else {
                    throw new RuntimeException("Unknown constraint type : " + constraintType);
                }

            }

            statement.close();

        }

        return tableConstraints;
    }


    /**
     * Appends all Column constraints (i.e. CHECK contraints) to the DTO
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto
     * @throws SQLException                if the connection to the database fails
     * @throws SqlConditionParserException if the parsing of a conditional expression fails
     */
    private List<TableConstraint> extractColumnConstraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        List<TableConstraint> columnConstraints = new LinkedList<>();
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            Statement statement = connectionToH2.createStatement();
            ResultSet columns = statement.executeQuery("Select * From INFORMATION_SCHEMA.COLUMNS "
                    + " where COLUMNS.TABLE_SCHEMA='" + tableSchema + "' and COLUMNS.TABLE_NAME='" + tableName + "'");
            while (columns.next()) {
                String checkConstraint = columns.getString("CHECK_CONSTRAINT");
                if (checkConstraint != null && !checkConstraint.equals("")) {
                    TableConstraint constraint = this.translateToConstraint(tableDto, checkConstraint);
                    columnConstraints.add(constraint);
                }
            }

            statement.close();
        }
        return columnConstraints;
    }

}
