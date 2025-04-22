package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SqlDatabaseDtoUtils {


    /**
     *
     * @param filter specifies which column should be returned, null means all columns should be returned
     * @return name of columns based on specified filter
     */
    public static List<String> extractColumnNames(InsertionDto dto, Set<String> filter){
        return dto.data.stream().filter(i-> (filter == null) || filter.stream().anyMatch(f-> i.variableName.equalsIgnoreCase(f))).map(i-> i.variableName).collect(Collectors.toList());
    }

    /**
     *
     * @param filter specifies which column should be returned, null means all columns should be returned
     * @return printable value of columns based on specified filter
     */
    public static List<String> extractColumnPrintableValues(InsertionDto dto, Set<String> filter){
        return dto.data.stream().filter(i-> (filter == null) || filter.stream().anyMatch(f-> i.variableName.equalsIgnoreCase(f))).map(i-> i.printableValue).collect(Collectors.toList());
    }


    /**
     *
     * @param columnName specified which ColumnDto should be returned based on its name
     * @return ColumnDto based on specified columnName
     */
    public static ColumnDto extractColumnInfo(TableDto dto, String columnName){
        Optional<ColumnDto> op = dto.columns.stream().filter(c-> columnName.equalsIgnoreCase(c.name)).findAny();
        return op.orElse(null);
    }
}
