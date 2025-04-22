package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.VariableDescriptor;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
     */
    public static QueryResult[] convertInsertionDtosToQueryResults(List<InsertionDto> insertionDtos, Map<String, Set<String>> columns, DbInfoDto schemaDto){

        Map<String, List<QueryResult>> maps = new HashMap<>();
        for (String key : columns.keySet()){
            List<QueryResult> kresults = new ArrayList<>();
            insertionDtos.stream().filter(d-> d.targetTable.equalsIgnoreCase(key)).forEach(insertionDto -> {
                QueryResult qr = convertInsertionDtoToQueryResult(insertionDto, key, columns.get(key), schemaDto, kresults);
                if (qr != null && (!qr.isEmpty()))
                    kresults.add(qr);
            });
            if(!kresults.isEmpty()){
                maps.put(key, kresults);
            }
        }

        // sort maps based on its key, ie, table name
        List<List<QueryResult>> qrPerTable =  cartesianProduct(maps.keySet().stream().sorted().map(maps::get).collect(Collectors.toList()));
        if (qrPerTable == null)
            return null;


        return qrPerTable.stream()
                .map(QueryResultTransformer::mergeQueryResultsByCartesianProductDataRows)
                .filter(Objects::nonNull).toArray(QueryResult[]::new);
    }


    private static QueryResult mergeQueryResultsByCartesianProductDataRows(List<QueryResult> queryResults){
        Objects.requireNonNull(queryResults);
        for (QueryResult qr: queryResults)
            Objects.requireNonNull(qr);

        if (queryResults.isEmpty()) return null;
        if (queryResults.size() == 1) return queryResults.get(0);

        List<VariableDescriptor> variableDescriptors = new ArrayList<>();

        List<List<DataRow>> datarowList = new ArrayList<>();
        for (QueryResult qr : queryResults) {
            if (!qr.isEmpty()) {
                variableDescriptors.addAll(qr.seeVariableDescriptors());
                datarowList.add(qr.seeRows());
            }
        }

        QueryResult merged = new QueryResult(variableDescriptors);

        List<List<DataRow>> results = cartesianProduct(datarowList);
        if (results != null && (!results.isEmpty())){
            for (List<DataRow> r : results){
                List<Object> mergedValues = new ArrayList<>();
                r.forEach(d-> mergedValues.addAll(d.seeValues()));
                DataRow mdatarow = new DataRow(variableDescriptors, mergedValues);
                merged.addRow(mdatarow);
            }
        }
        return merged;
    }


    /**
     * implement Cartesian Product
     * e.g.,  (a,b) * (c,d) = (a,c), (a,d), (b,c), (b,d)
     * @param values specified a list of sets for n-fold Cartesian Product
     * @return results of Cartesian Product of the given sets
     * @param <T> type of values
     */
    public static <T> List<List<T>> cartesianProduct(List<List<T>> values){
        if (values.isEmpty()) return Collections.emptyList();
        values.forEach(Objects::requireNonNull);

        int[] counts = values.stream().mapToInt(s-> s.size() -1).toArray();
        int[] indexes = values.stream().mapToInt(s-> s.isEmpty()?-1:0).toArray();
        List<List<T>> results = new ArrayList<>();
        boolean isLast = false;
        while (!isLast){
            if (Arrays.equals(counts, indexes))
                isLast = true;

            List<T> row = new ArrayList<>();
            for (int i = 0; i < counts.length ; i++){
                if (indexes[i] >= 0)
                    row.add(values.get(i).get(indexes[i]));
            }
            results.add(row);

            for (int j = indexes.length-1; !isLast && j >=0 ; j--){
                if (indexes[j] >=0 && indexes[j] < counts[j]){
                    indexes[j] = indexes[j] + 1;
                    for (int t = j+1; t < indexes.length ; t++){
                        if(indexes[t] != -1)
                            indexes[t] = 0;
                    }
                    break;
                }
            }
        }

        return results;
    }


    private static QueryResult convertInsertionDtoToQueryResult(InsertionDto insertionDto, String tableName, Set<String> relatedColumns, DbInfoDto dto, List<QueryResult> existingQueryResults){
        List<String> relatedColumnNames = SqlDatabaseDtoUtils.extractColumnNames(insertionDto, relatedColumns);
        if (!relatedColumnNames.isEmpty()){
            QueryResult found = null;
            if (!existingQueryResults.isEmpty())
                found = existingQueryResults.stream().filter(e-> e.sameVariableNames(relatedColumnNames, tableName)).findAny().orElse(null);

            QueryResult qr = found;
            if (found == null)
                qr = new QueryResult(relatedColumnNames, tableName);

            Optional<TableDto> foundTableSchema = dto.tables.stream().filter(t-> t.name.equalsIgnoreCase(tableName)).findFirst();
            if (foundTableSchema.isPresent()){
                TableDto tableDto = foundTableSchema.get();

                List<String> printableValue = SqlDatabaseDtoUtils.extractColumnPrintableValues(insertionDto, relatedColumns);
                assert printableValue.size() == relatedColumnNames.size();

                List<Object> values = new ArrayList<>();

                for (int i = 0; i < printableValue.size(); i++){
                    ColumnDto columnDto = SqlDatabaseDtoUtils.extractColumnInfo(tableDto, relatedColumnNames.get(i));
                    if (columnDto == null)
                        throw new IllegalArgumentException("Cannot find column schema of "+ relatedColumnNames.get(i) + " in Table "+ tableName);
                    values.add(getColumnValueBasedOnPrintableValue(printableValue.get(i), columnDto));
                }

                qr.addRow(relatedColumnNames, tableName, values);

            }else {
                throw new IllegalArgumentException("Cannot find table schema of "+ tableName);
            }

            if (found != null) return null;

            return qr;
        }
        return null;
    }


    private static Object getColumnValueBasedOnPrintableValue(String printableValue, ColumnDto dto){
        /*
            might later make org.evomaster.core.sql.schema.ColumnDataType shared with driver side
         */

        if (printableValue == null) return null;

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

        if(dto.type.equalsIgnoreCase("TIMESTAMP")
                || dto.type.equalsIgnoreCase("TIMESTAMPZ")
                || dto.type.equalsIgnoreCase("TIMETZ")
                || dto.type.equalsIgnoreCase("DATE")
                || dto.type.equalsIgnoreCase("DATETIME")
                || dto.type.equalsIgnoreCase("TIME")
        ){
            Instant instant = ColumnTypeParser.getAsInstant(printableValue);
            if (instant != null) return instant;
        }


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
