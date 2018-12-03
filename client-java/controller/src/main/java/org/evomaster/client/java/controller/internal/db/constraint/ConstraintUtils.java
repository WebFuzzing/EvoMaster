package org.evomaster.client.java.controller.internal.db.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;

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
}
