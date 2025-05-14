package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.evomaster.client.java.sql.SqlDataType.*;

/**
 * This class is used to covert InsertionDto to QueryResult and DataRow
 * in order to calculate sql heuristics based on InsertionDto
 */
public class QueryResultTransformer {


    /**
     * @param insertionDtos specifies InsertionDto which indicates data we have been inserted into database or mocked
     * @param columns       specifies WHERE clause using a map from table name to involved column names
     * @param schemaDto     specifies info about schema that is used to parse printable value specified in InsertionDto to Object value
     * @return extracted an array of QueryResult.
     */
    public static QueryResult[] convertInsertionDtosToQueryResults(List<InsertionDto> insertionDtos, Map<SqlTableId, Set<SqlColumnId>> columns, DbInfoDto schemaDto) {

        Map<SqlTableId, List<QueryResult>> maps = new HashMap<>();
        for (SqlTableId tableId : columns.keySet()) {
            List<QueryResult> kresults = new ArrayList<>();
            insertionDtos.stream().filter(d -> d.targetTable.equalsIgnoreCase(tableId.getTableId())).forEach(insertionDto -> {
                QueryResult qr = convertInsertionDtoToQueryResult(insertionDto, tableId, columns.get(tableId), schemaDto, kresults);
                if (qr != null && (!qr.isEmpty()))
                    kresults.add(qr);
            });
            if (!kresults.isEmpty()) {
                maps.put(tableId, kresults);
            }
        }

        // sort maps based on its key, ie, table name
        List<List<QueryResult>> qrPerTable = cartesianProduct(maps.keySet().stream().sorted().map(maps::get).collect(Collectors.toList()));
        if (qrPerTable == null)
            return null;


        return qrPerTable.stream()
                .map(QueryResultTransformer::mergeQueryResultsByCartesianProductDataRows)
                .filter(Objects::nonNull).toArray(QueryResult[]::new);
    }


    private static QueryResult mergeQueryResultsByCartesianProductDataRows(List<QueryResult> queryResults) {
        Objects.requireNonNull(queryResults);
        for (QueryResult qr : queryResults)
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
        if (results != null && (!results.isEmpty())) {
            for (List<DataRow> r : results) {
                List<Object> mergedValues = new ArrayList<>();
                r.forEach(d -> mergedValues.addAll(d.seeValues()));
                DataRow mdatarow = new DataRow(variableDescriptors, mergedValues);
                merged.addRow(mdatarow);
            }
        }
        return merged;
    }


    /**
     * implement Cartesian Product
     * e.g.,  (a,b) * (c,d) = (a,c), (a,d), (b,c), (b,d)
     *
     * @param values specified a list of sets for n-fold Cartesian Product
     * @param <T>    type of values
     * @return results of Cartesian Product of the given sets
     */
    public static <T> List<List<T>> cartesianProduct(List<List<T>> values) {
        if (values.isEmpty()) return Collections.emptyList();
        values.forEach(Objects::requireNonNull);

        int[] counts = values.stream().mapToInt(s -> s.size() - 1).toArray();
        int[] indexes = values.stream().mapToInt(s -> s.isEmpty() ? -1 : 0).toArray();
        List<List<T>> results = new ArrayList<>();
        boolean isLast = false;
        while (!isLast) {
            if (Arrays.equals(counts, indexes))
                isLast = true;

            List<T> row = new ArrayList<>();
            for (int i = 0; i < counts.length; i++) {
                if (indexes[i] >= 0)
                    row.add(values.get(i).get(indexes[i]));
            }
            results.add(row);

            for (int j = indexes.length - 1; !isLast && j >= 0; j--) {
                if (indexes[j] >= 0 && indexes[j] < counts[j]) {
                    indexes[j] = indexes[j] + 1;
                    for (int t = j + 1; t < indexes.length; t++) {
                        if (indexes[t] != -1)
                            indexes[t] = 0;
                    }
                    break;
                }
            }
        }

        return results;
    }


    private static QueryResult convertInsertionDtoToQueryResult(InsertionDto insertionDto,
                                                                SqlTableId tableId,
                                                                Set<SqlColumnId> relatedColumns,
                                                                DbInfoDto dto,
                                                                List<QueryResult> existingQueryResults) {

        List<String> relatedColumnNames = SqlDatabaseDtoUtils.extractColumnNamesUsedInTheInsertion(insertionDto, relatedColumns);
        if (!relatedColumnNames.isEmpty()) {
            final QueryResult existingQueryResult;
            if (!existingQueryResults.isEmpty())
                existingQueryResult = existingQueryResults.stream().filter(qr -> qr.sameVariableNames(relatedColumnNames, tableId.getTableId())).findAny().orElse(null);
            else
                existingQueryResult = null;

            final QueryResult queryResult;
            if (existingQueryResult == null)
                queryResult = new QueryResult(relatedColumnNames, tableId.getTableId());
            else
                queryResult = existingQueryResult;

            Optional<TableDto> foundTableSchema = dto.tables.stream().filter(t -> t.name.equalsIgnoreCase(tableId.getTableId())).findFirst();
            if (foundTableSchema.isPresent()) {
                TableDto tableDto = foundTableSchema.get();

                List<String> printableValue = SqlDatabaseDtoUtils.extractColumnPrintableValues(insertionDto, relatedColumns);
                assert printableValue.size() == relatedColumnNames.size();

                List<Object> values = new ArrayList<>();

                for (int i = 0; i < printableValue.size(); i++) {
                    ColumnDto columnDto = SqlDatabaseDtoUtils.extractColumnInfo(tableDto, relatedColumnNames.get(i));
                    if (columnDto == null)
                        throw new IllegalArgumentException("Cannot find column schema of " + relatedColumnNames.get(i) + " in Table " + tableId);
                    values.add(getColumnValueBasedOnPrintableValue(printableValue.get(i), columnDto));
                }


                queryResult.addRow(relatedColumnNames, tableId.getTableId(), values);

            } else {
                throw new IllegalArgumentException("Cannot find table schema of " + tableId);
            }

            if (existingQueryResult != null) return null;

            return queryResult;
        }
        return null;
    }


    private static Object getColumnValueBasedOnPrintableValue(String printableValue, ColumnDto dto) {
        /*
            might later make org.evomaster.core.sql.schema.ColumnDataType shared with driver side
         */

        if (printableValue == null) {
            return null;
        }

        final String dtoDataTypeName = dto.type;
        SqlDataType dataType = SqlDataType.fromString(dtoDataTypeName);
        if (isBooleanType(dataType)) {
            return Boolean.valueOf(printableValue);
        }

        if (isIntegerType(dataType)) {
            return Integer.valueOf(printableValue);
        }

        if (isShortType(dataType)) {
            return Short.valueOf(printableValue);
        }

        if (isByteType(dataType)) {
            return Byte.valueOf(printableValue);
        }

        if (isLongType(dataType)) {
            return Long.valueOf(printableValue);
        }

        if (isDoubleType(dataType)) {
            return Double.valueOf(printableValue);
        }

        if (isDateTimeType(dataType)) {
            Instant instant = ColumnTypeParser.getAsInstant(printableValue);
            if (instant != null) return instant;
        }

        if (isStringType(dataType)) {
            return String.valueOf(printableValue);
        }

        return printableValue;

    }

    public static QueryResultSet translateInsertionDtos(
            List<InsertionDto> insertionDtos,
            Map<SqlTableId, Set<SqlColumnId>> columns,
            DbInfoDto schema) {

        QueryResultSet queryResultSet = new QueryResultSet();
        for (SqlTableId tableId : columns.keySet()) {
            TableDto tableDto = schema.tables.stream()
                    .filter(t -> t.name.equalsIgnoreCase(tableId.getTableId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Cannot find table schema of " + tableId));

            List<VariableDescriptor> variableDescriptors = tableDto.columns.stream()
                    .map(c -> new VariableDescriptor(c.name, c.name, c.table, c.table))
                    .collect(Collectors.toList());

            QueryResult queryResult = new QueryResult(variableDescriptors);
            for (InsertionDto dto : insertionDtos) {
                if (tableId.getTableId().equalsIgnoreCase(dto.targetTable)) {
                    List<Object> concreteValues = new ArrayList<>();
                    for (VariableDescriptor variableDescriptor : variableDescriptors) {
                        Object concreteValueOrNull = findConcreteValueOrNull(variableDescriptor.getColumnName(), tableDto, dto.data);
                        concreteValues.add(concreteValueOrNull);
                    }
                    DataRow dataRow = new DataRow(variableDescriptors, concreteValues);
                    queryResult.addRow(dataRow);
                }
            }
            queryResultSet.addQueryResult(queryResult);
        }

        return queryResultSet;
    }

    private static Object findConcreteValueOrNull(String columnName, TableDto tableDto, List<InsertionEntryDto> entries) {
        for (InsertionEntryDto entry : entries) {
            if (entry.variableName.equalsIgnoreCase(columnName)) {
                ColumnDto columnDto = SqlDatabaseDtoUtils.extractColumnInfo(tableDto, entry.variableName);
                if (columnDto == null) {
                    throw new IllegalArgumentException("Cannot find column schema of " + entry.variableName + " in Table " + tableDto.name);
                }
                return getColumnValueBasedOnPrintableValue(entry.printableValue, columnDto);
            }
        }
        // return null if not found
        return null;
    }
}
