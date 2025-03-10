package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.VariableDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryResultUtils {

    public static QueryResult createUnionRowSet(List<QueryResult> queryResults) {
        final List<VariableDescriptor> variableDescriptorList = queryResults.get(0).seeVariableDescriptors();
        final List<VariableDescriptor> unionVariableDescriptorList = new ArrayList<>();
        final List<String> columnNames = new ArrayList<>();
        for (VariableDescriptor variableDescriptor : variableDescriptorList) {
            final String columnName = variableDescriptor.getColumnName();
            VariableDescriptor vd = new VariableDescriptor(columnName, null, null);
            unionVariableDescriptorList.add(vd);
            columnNames.add(columnName);
        }
        QueryResult unionRowSet = new QueryResult(unionVariableDescriptorList);
        for (QueryResult queryResult : queryResults) {
            for (DataRow row : queryResult.seeRows()) {
                unionRowSet.addRow(columnNames, null, row.seeValues());
            }
        }
        return unionRowSet;
    }

    public static QueryResult createCartesianProduct(QueryResult left, QueryResult right) {

        final QueryResult joinedQueryResult = createEmptyCartesianProduct(left, right);
        for (DataRow leftRow : left.seeRows()) {
            for (DataRow rightRow : right.seeRows()) {
                final DataRow joinedDataRow = createJoinedRow(leftRow, rightRow, joinedQueryResult.seeVariableDescriptors());
                joinedQueryResult.addRow(joinedDataRow);
            }
        }
        return joinedQueryResult;
    }

    public static DataRow createJoinedRow(DataRow leftRow, DataRow rightRow, List<VariableDescriptor> variableDescriptors) {
        List<Object> leftColumnValues = leftRow.seeValues();
        List<Object> rightColumnValues = rightRow.seeValues();
        List<Object> joinedColumnValues = new ArrayList<>();
        joinedColumnValues.addAll(leftColumnValues);
        joinedColumnValues.addAll(rightColumnValues);
        DataRow joinedDataRow = new DataRow(variableDescriptors, joinedColumnValues);
        return joinedDataRow;
    }

    public static QueryResult createEmptyCartesianProduct(QueryResult left, QueryResult right) {
        List<VariableDescriptor> joinedListOfDescriptors = new ArrayList<>();
        joinedListOfDescriptors.addAll(left.seeVariableDescriptors());
        joinedListOfDescriptors.addAll(right.seeVariableDescriptors());
        QueryResult joinedQueryResult = new QueryResult(joinedListOfDescriptors);
        return joinedQueryResult;
    }

    public static DataRow createDataRowOfNullValues(QueryResult queryResult) {
        int numberOfColumns = queryResult.seeVariableDescriptors().size();
        final List<Object> listOfNullValues = Collections.nCopies(numberOfColumns, null);
        DataRow nullDataRow = new DataRow(queryResult.seeVariableDescriptors(), listOfNullValues);
        return nullDataRow;
    }
}
