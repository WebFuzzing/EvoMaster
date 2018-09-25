package org.evomaster.clientJava.controller.internal.db;

import org.evomaster.clientJava.controllerApi.dto.database.schema.TableDto;

import java.util.List;

public class TableDtoUtils {


    public static boolean containsTable(List<TableDto> tables, String tableName) {
        for (TableDto tableDto : tables) {
            if (tableDto.name.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

    public static TableDto getTable(List<TableDto> tables, String tableName) {
        for (TableDto tableDto : tables) {
            if (tableDto.name.equalsIgnoreCase(tableName)) {
                return tableDto;
            }
        }
        return null;
    }
}
