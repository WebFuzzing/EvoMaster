package org.evomaster.client.java.controller.internal.db.constraint;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class H2Constraints {

    /**
     * Expects the schema explained in
     * http://www.h2database.com/html/systemtables.html#information_schema
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto      a DTO schema with retrieved information from the JBDC metada
     * @throws Exception
     */
    public static void addH2Constraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException, JSQLParserException {

        addH2ColumnConstraints(connectionToH2, schemaDto);

        addH2TableConstraints(connectionToH2, schemaDto);

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
     * @throws SQLException        if the connection to the H2 database fails,
     * @throws JSQLParserException if a conditional expression fails to be parsed
     */
    private static void addH2TableConstraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException, JSQLParserException {

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
                    String[] uniqueColumnNames = columnList.split(",");
                    for (int i = 0; i < uniqueColumnNames.length; i++) {
                        String columnName = uniqueColumnNames[i].trim();
                        ConstraintUtils.addUniqueConstraintToColumn(tableName, tableDto, columnName);
                    }
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
                    addH2CheckConstraint(tableDto, checkExpression);
                } else {
                    throw new RuntimeException("Unknown constraint type : " + constraintType);
                }

            }

            statement.close();

        }

    }

    /**
     * Parsers a conditional expression and adds those constraints to the TableDto
     *
     * @param tableDto
     *
     * @param condExpression
     *
     * @throws JSQLParserException if the parsing of the conditional expression fails
     */
    private static void addH2CheckConstraint(TableDto tableDto, String condExpression) throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression(condExpression);
        CheckExprExtractor exprExtractor = new CheckExprExtractor();
        expr.accept(exprExtractor);
        String tableName = tableDto.name;
        List<SchemaConstraint> constraints = exprExtractor.getConstraints();
        for (SchemaConstraint constraint : constraints) {
            if (constraint instanceof LowerBoundConstraint) {
                LowerBoundConstraint lowerBound = (LowerBoundConstraint) constraint;
                String columnName = lowerBound.getColumnName();
                ColumnDto columnDto = tableDto.columns.stream().filter(c -> c.name.equalsIgnoreCase(columnName)).findFirst().orElse(null);
                if (columnDto == null) {
                    throw new IllegalArgumentException("Column " + columnName + " was not found in table " + tableName);
                }
                columnDto.lowerBound = (int) lowerBound.getLowerBound();

            } else if (constraint instanceof UpperBoundConstraint) {
                UpperBoundConstraint upperBound = (UpperBoundConstraint) constraint;
                String columnName = upperBound.getColumnName();
                ColumnDto columnDto = tableDto.columns.stream().filter(c -> c.name.equalsIgnoreCase(columnName)).findFirst().orElse(null);
                if (columnDto == null) {
                    throw new IllegalArgumentException("Column " + columnName + " was not found in table " + tableName);
                }
                columnDto.upperBound = (int) upperBound.getUpperBound();

            } else if (constraint instanceof RangeConstraint) {
                RangeConstraint rangeConstraint = (RangeConstraint) constraint;
                String columnName = rangeConstraint.getColumnName();
                ColumnDto columnDto = tableDto.columns.stream().filter(c -> c.name.equalsIgnoreCase(columnName)).findFirst().orElse(null);
                if (columnDto == null) {
                    throw new IllegalArgumentException("Column " + columnName + " was not found in table " + tableName);
                }
                columnDto.lowerBound = (int) rangeConstraint.getMinValue();
                columnDto.upperBound = (int) rangeConstraint.getMaxValue();
            } else if (constraint instanceof EnumConstraint) {
                EnumConstraint enumConstraint = (EnumConstraint) constraint;
                String columnName = enumConstraint.getColumnName();
                ColumnDto columnDto = tableDto.columns.stream().filter(c -> c.name.equalsIgnoreCase(columnName)).findFirst().orElse(null);
                if (columnDto == null) {
                    throw new IllegalArgumentException("Column " + columnName + " was not found in table " + tableName);
                }
                columnDto.enumValuesAsStrings = enumConstraint.getValuesAsStrings();

            } else {
                throw new RuntimeException("Unknown constraint type " + constraint.getClass().getName());
            }
        }
    }

    /**
     * Appends all Column constraints (i.e. CHECK contraints) to the DTO
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto
     * @throws SQLException        if the connection to the database fails
     * @throws JSQLParserException if the parsing of a conditional expression fails
     */
    private static void addH2ColumnConstraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException, JSQLParserException {
        String tableSchema = schemaDto.name;
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            Statement statement = connectionToH2.createStatement();
            ResultSet columns = statement.executeQuery("Select * From INFORMATION_SCHEMA.COLUMNS "
                    + " where COLUMNS.TABLE_SCHEMA='" + tableSchema + "' and COLUMNS.TABLE_NAME='" + tableName + "'");
            while (columns.next()) {
                String tableCatalog = columns.getString("TABLE_CATALOG");
                String columnName = columns.getString("COLUMN_NAME");

                String checkConstraint = columns.getString("CHECK_CONSTRAINT");
                if (checkConstraint != null && !checkConstraint.equals("")) {
                    addH2CheckConstraint(tableDto, checkConstraint);
                }
            }

            statement.close();
        }
    }

}
