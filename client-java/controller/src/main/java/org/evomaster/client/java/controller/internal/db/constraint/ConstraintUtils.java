package org.evomaster.client.java.controller.internal.db.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.internal.db.constraint.extract.H2ConstraintExtractor;
import org.evomaster.client.java.controller.internal.db.constraint.extract.PostgresConstraintExtractor;
import org.evomaster.client.java.controller.internal.db.constraint.parser.SqlConditionParserException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/*
    JDBC MetaData is quite limited.
    To check constraints, we need to do SQL queries on the system tables.
    Unfortunately, this is database-dependent
*/
public class ConstraintUtils {

    /**
     * Adds a unique constriant to the correspondinding ColumnDTO for the selected table.column pair.
     * Requires the ColumnDTO to be contained in the TableDTO.
     * If the column DTO is not contained, a IllegalArgumentException is thrown.
     **/
    public static void addUniqueConstraintToColumn(String tableName, TableDto tableDto, String columnName) {

        ColumnDto columnDto = tableDto.columns.stream()
                .filter(c -> c.name.equals(columnName)).findAny().orElse(null);

        if (columnDto == null) {
            throw new IllegalArgumentException("Missing column DTO for column:" + tableName + "." + columnName);
        }

        columnDto.unique = true;
    }


    /**
     * Appends constraints that are database specific.
     *
     * @param connection
     * @param dt
     * @param schemaDto
     * @throws Exception
     */
    public static void addConstraints(Connection connection, DatabaseType dt, DbSchemaDto schemaDto) throws SQLException, SqlConditionParserException {
        switch (dt) {
            case H2: {
                List<TableConstraint> h2ConstraintList = new H2ConstraintExtractor().extractConstraints(connection, schemaDto);
                addConstraints(schemaDto, h2ConstraintList);
                break;
            }
            case DERBY: {
                // TODO Derby
                break;
            }
            case POSTGRES: {
                List<TableConstraint> postgresConstraintList = new PostgresConstraintExtractor().extractConstraints(connection, schemaDto);
                addConstraints(schemaDto, postgresConstraintList);
                break;
            }
            case OTHER: {
                // TODO Other
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown database type " + dt);
            }
        }
    }

    private static void addConstraints(DbSchemaDto schemaDto, List<TableConstraint> constraintList) {
        for (TableConstraint constraint : constraintList) {
            String tableName = constraint.getTableName();
            TableDto tableDto = schemaDto.tables.stream().filter(t -> t.name.equalsIgnoreCase(tableName)).findFirst().orElse(null);

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
            } else if (constraint instanceof UniqueConstraint) {

                UniqueConstraint uniqueConstraint = (UniqueConstraint) constraint;
                for (String columnName : uniqueConstraint.getUniqueColumnNames()) {
                    addUniqueConstraintToColumn(tableName, tableDto, columnName);
                }

            } else {
                throw new RuntimeException("Unknown constraint type " + constraint.getClass().getName());
            }
        }

    }
}
