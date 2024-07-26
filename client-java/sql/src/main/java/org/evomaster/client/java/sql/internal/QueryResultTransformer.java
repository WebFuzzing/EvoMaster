package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.QueryResult;

import java.util.*;

/**
 * This class is used to covert InsertionDto to QueryResult and DataRow
 * in order to calculate sql heuristics based on InsertionDto
 */
public class QueryResultTransformer {


    /**
     * @param insertionDtos specifies InsertionDto which indicates data we have been inserted into database or mocked
     * @param columns       specifies WHERE clause using a map from table name to involved column names
     * @param schemaDto     specifies info about schema that is used to parse printable value specified in InsertionDto to Object value
     * @return  extracted an array of QueryResult.
     * note that, although for the same table, its insertion might be applied to different columns.
     * To simply conversion from InsertionDtos to QueryResults, we create a QueryResult per InsertionDto
     */
    public static QueryResult[] convertInsertionDtosToQueryResults(List<InsertionDto> insertionDtos, Map<String, Set<String>> columns, DbSchemaDto schemaDto){
        List<QueryResult> results = new ArrayList<>();

        for (String key: columns.keySet()){

            insertionDtos.stream().filter(d-> d.targetTable.equalsIgnoreCase(key)).forEach(insertionDto -> {
                QueryResult qr = convertInsertionDtoToQueryResult(insertionDto, key, columns.get(key), schemaDto);
                if (qr != null && (!qr.isEmpty()))
                    results.add(qr);
            });

        }

        return results.toArray(new QueryResult[results.size()]);
    }

    private static QueryResult convertInsertionDtoToQueryResult(InsertionDto insertionDto, String tableName, Set<String> relatedColumns, DbSchemaDto dto){
        List<String> relatedColumnNames = insertionDto.extractColumnNames(relatedColumns);
        if (!relatedColumnNames.isEmpty()){
            QueryResult qr = new QueryResult(relatedColumnNames, tableName);
            Optional<TableDto> foundTableSchema = dto.tables.stream().filter(t-> t.name.equalsIgnoreCase(tableName)).findFirst();
            if (foundTableSchema.isPresent()){
                TableDto tableDto = foundTableSchema.get();

                List<String> printableValue = insertionDto.extractColumnPrintableValues(relatedColumns);
                assert printableValue.size() == relatedColumnNames.size();

                List<Object> values = new ArrayList<>();

                for (int i = 0; i < printableValue.size(); i++){
                    ColumnDto columnDto = tableDto.extractColumnInfo(relatedColumnNames.get(i));
                    if (columnDto == null)
                        throw new IllegalArgumentException("Cannot find column schema of "+ relatedColumnNames.get(i) + " in Table "+ tableName);
                    values.add(getColumnValueBasedOnPrintableValue(printableValue.get(i), columnDto));
                }

                qr.addRow(relatedColumnNames, tableName, values);

            }else {
                throw new IllegalArgumentException("Cannot find table schema of "+ tableName);
            }

            return qr;
        }
        return null;
    }


    private static Object getColumnValueBasedOnPrintableValue(String printableValue, ColumnDto dto){
        /*
            might later make org.evomaster.core.sql.schema.ColumnDataType shared with driver side
         */

        if (dto.type.equalsIgnoreCase("BOOL") || dto.type.equalsIgnoreCase("BOOLEAN"))
            return Boolean.valueOf(printableValue);

        if (dto.type.equalsIgnoreCase("INT") || dto.type.equalsIgnoreCase("INTEGER") || dto.type.equalsIgnoreCase("INT4"))
            return Integer.valueOf(printableValue);


        if (dto.type.equalsIgnoreCase("INT2") || dto.type.equalsIgnoreCase("SMALLINT") )
            return Short.valueOf(printableValue);


        if (dto.type.equalsIgnoreCase("TINYINT") )
            return Byte.valueOf(printableValue);

        if(dto.type.equalsIgnoreCase("INT8") || dto.type.equalsIgnoreCase("BIGINT") || dto.type.equalsIgnoreCase("BIGSERIAL"))
            return Long.valueOf(printableValue);


        if (dto.type.equalsIgnoreCase("DOUBLE")
                || dto.type.equalsIgnoreCase("DOUBLE_PRECISION")
                || dto.type.equalsIgnoreCase("FLOAT")
                || dto.type.equalsIgnoreCase("REAL")
                || dto.type.equalsIgnoreCase("FLOAT4")
                || dto.type.equalsIgnoreCase("FLOAT8")
                || dto.type.equalsIgnoreCase("DEC")
                || dto.type.equalsIgnoreCase("DECIMAL")
                || dto.type.equalsIgnoreCase("NUMERIC")
        )
            return Double.valueOf(printableValue);

        if (dto.type.equalsIgnoreCase("CHAR")
                || dto.type.equalsIgnoreCase("CHARACTER")
                || dto.type.equalsIgnoreCase("CHARACTER_LARGE_OBJECT")
                || dto.type.equalsIgnoreCase("TINYTEXT")
                || dto.type.equalsIgnoreCase("TEXT")
                || dto.type.equalsIgnoreCase("LONGTEXT")
                || dto.type.equalsIgnoreCase("VARCHAR")
                || dto.type.equalsIgnoreCase("CHARACTER_VARYING")
                || dto.type.equalsIgnoreCase("VARCHAR_IGNORECASE")
                || dto.type.equalsIgnoreCase("CLOB")
                || dto.type.equalsIgnoreCase("MEDIUMTEXT")
                || dto.type.equalsIgnoreCase("LONGBLOB")
                || dto.type.equalsIgnoreCase("MEDIUMBLOB")
                || dto.type.equalsIgnoreCase("TINYBLOB")
        )
                return String.valueOf(printableValue);

        return printableValue;

    }
}
