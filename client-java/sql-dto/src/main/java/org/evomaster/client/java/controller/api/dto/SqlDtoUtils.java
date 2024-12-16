package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SqlDtoUtils {


    /**
     * Return a fully qualifying name for input table, which can be used as id.
     */
    public static String getId(TableDto dto){
        if(dto.schema == null){
            return dto.name;
        }
        return dto.schema + "." + dto.name;
    }

    /**
     * @return a table DTO for a particular table name
     */
    public static TableDto getTable(DbInfoDto schema, String tableName) {
        return schema.tables.stream()
                .filter(t -> matchByName(t, tableName))
                .findFirst().orElse(null);
    }

    /**
     * Check if given table dto is matched by name.
     * This can be a partial name, or a full qualifying name.
     */
    public static boolean matchByName(TableDto dto, String name){

        Objects.requireNonNull(dto);
        Objects.requireNonNull(name);

        if(name.isEmpty()){
           throw new IllegalArgumentException("Empty table name");
        }

        String[] tokens = name.split("\\.");
        if(tokens.length > 3){
            throw new IllegalArgumentException("Invalid table name identifier. Too many '.': " + name);
        }
        if(tokens.length == 1){
            return dto.name.equalsIgnoreCase(tokens[0]);
        }
        if(tokens.length == 2){
            boolean mn = dto.name.equalsIgnoreCase(tokens[1]);
            if(!mn){
                return false;
            }

            if(dto.catalog == null && dto.schema == null){
                //there is no default schema, but DTO is unspecified, then false
                return tokens[0].equalsIgnoreCase("public");
            } else if(dto.catalog != null && dto.schema != null){
                //both specified... so look at schema
                return tokens[0].equalsIgnoreCase(dto.schema);
            } else {
                //only one specified, take it
                if(dto.schema != null){
                    return tokens[0].equalsIgnoreCase(dto.schema);
                } else{
                    //this can be the case for MySQL
                    return tokens[0].equalsIgnoreCase(dto.catalog);
                }
            }
        }
        if(tokens.length == 3){
            return tokens[0].equalsIgnoreCase(dto.catalog)
                    && tokens[1].equalsIgnoreCase(dto.schema)
                    && tokens[2].equalsIgnoreCase(dto.name);
        }

        //shouldn't be reached
        return false;
    }

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
