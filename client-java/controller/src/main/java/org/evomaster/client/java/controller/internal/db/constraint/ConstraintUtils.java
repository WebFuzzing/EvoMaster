package org.evomaster.client.java.controller.internal.db.constraint;

import org.apache.calcite.sql.parser.SqlParseException;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;

import java.sql.Connection;
import java.sql.SQLException;

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
    public static void addConstraints(Connection connection, DatabaseType dt, DbSchemaDto schemaDto) throws SQLException, SqlParseException {
        switch (dt) {
            case H2: {
                H2Constraints.addH2Constraints(connection, schemaDto);
                break;
            }
            case DERBY: {
                // TODO Derby
                break;
            }
            case POSTGRES:{
                //PostgresConstraints.addPostgresConstraints(connection, schemaDto);
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
}
