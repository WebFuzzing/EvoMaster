package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto;
import org.evomaster.client.java.sql.heuristic.BooleanLiteralsHelper;
import org.evomaster.client.java.sql.internal.SqlNameContext;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Clob;
import java.util.*;
import java.util.stream.Collectors;

import static org.evomaster.client.java.sql.heuristic.SqlStringUtils.nullSafeEqualsIgnoreCase;

/**
 * A row of data in the table results of a Select query.
 * Must include information on its columns.
 */
public class DataRow {

    /**
     * Descriptors for the columns
     */
    private final List<VariableDescriptor> variableDescriptors;

    /**
     * The actual data values. This list must be aligned with variableDescriptors
     */
    private final List<Object> values;

    private final static String NULL_VALUE = "NULL";

    public DataRow(String tableName, List<String> columnNames, List<Object> values) {
        this(columnNames.stream().map(c -> new VariableDescriptor(c, null, tableName)).collect(Collectors.toList()),
                values);
    }

    public DataRow(String columnName, Object value, String tableName) {
        this(Arrays.asList(new VariableDescriptor(columnName, null, tableName)), Arrays.asList(value));
    }

    public DataRow(List<VariableDescriptor> descriptors, List<Object> values) {
        Objects.requireNonNull(descriptors);
        Objects.requireNonNull(values);

        if (descriptors.size() != values.size()) {
            throw new IllegalArgumentException("Size mismatch");
        }
        List<VariableDescriptor> list = new ArrayList<>();
        list.addAll(descriptors);
        this.variableDescriptors = Collections.unmodifiableList(list);

        List<Object> valList = new ArrayList<>();
        valList.addAll(values);
        this.values = Collections.unmodifiableList(valList);
    }


    public List<VariableDescriptor> getVariableDescriptors() {
        return variableDescriptors;
    }

    public List<Object> seeValues() {
        return values;
    }

    public Object getValue(int index) {
        Object value = values.get(index);
        if (value instanceof Clob) {
            Clob clob = (Clob) value;
            try {
                return clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                SimpleLogger.error("Failed to retrieve CLOB data");
                return null;
            }
        }
        return value;
    }

    public Object getValueByName(String name) {
        return getValueByName(name, null);
    }

    public boolean hasValueByName(String columnName, String baseTableName) {
        try {
            this.getValueByName(columnName, baseTableName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public Object getValueByName(String name, String table) {
        String n = (name == null ? null : name.trim());
        String t = (table == null ? null : table.trim());

        //true/false are reserved keywords
        /*
         * There are test cases where some columns are
         * called "y", therefore, we cannot use
         * BooleanLiteralHelper.isBooleanLiteral() here
         * since 'y','n','on','off', 'yes' and 'no'
         * are also considered boolean literals.
         */
        if (n!=null && n.equalsIgnoreCase("true")) {
            return true;
        }
        if (n!= null && n.equalsIgnoreCase("false")) {
            return false;
        }

        //first check aliases, but only if no specify table
        if (t == null || t.isEmpty()) {
            for (int i = 0; i < variableDescriptors.size(); i++) {
                VariableDescriptor desc = variableDescriptors.get(i);
                if (nullSafeEqualsIgnoreCase(n, desc.getAliasColumnName())) {
                    return getValue(i);
                }
            }
        }

        List<Integer> candidates = new ArrayList<>();

        //if none, then check column names
        for (int i = 0; i < variableDescriptors.size(); i++) {
            VariableDescriptor desc = variableDescriptors.get(i);

            if (!n.equalsIgnoreCase(desc.getColumnName())){
                continue;
            }
            //no defined table, or exact match
            if(t == null || t.isEmpty() || t.equalsIgnoreCase(desc.getTableName())){
                return getValue(i);
            }
            /*
                TODO: this does not cover all possible cases, as in theory
                there can be many unnamed tables (eg results of sub-selects)
                with same column names. At this moment, we would not
                be able to distinguish them
             */
            if(t.equalsIgnoreCase(SqlNameContext.UNNAMED_TABLE)){
                candidates.add(i);
            }
            /*
                We just specified the name without schema... if unique, we would be fine
             */
            if(!t.contains(".") && desc.getTableName().toLowerCase().endsWith("."+t.toLowerCase())){
                candidates.add(i);
            if ((nullSafeEqualsIgnoreCase(n, desc.getColumnName()) || nullSafeEqualsIgnoreCase(n, desc.getAliasColumnName())) &&
                    (t == null || t.isEmpty()
                            || nullSafeEqualsIgnoreCase(t, desc.getTableName())
                            /*
                                TODO: this does not cover all possible cases, as in theory
                                there can be many unnamed tables (eg results of sub-selects)
                                with same column names. At this moment, we would not
                                be able to distinguish them
                             */
                            || nullSafeEqualsIgnoreCase(t, SqlNameContext.UNNAMED_TABLE)
                    )
            ) {
                return getValue(i);
            }
        }
        if(candidates.size() > 1){
            SimpleLogger.uniqueWarn("More than one table candidate for: " + t);
        }

        if(candidates.size() >= 1){
            return getValue(candidates.get(0));
        }

        throw new IllegalArgumentException("No variable called '" + name + "' for table '" + table + "'");
    }

    public String getAsLine() {
        return values.stream().map(obj -> (obj != null) ? obj.toString() : NULL_VALUE).collect(Collectors.joining(","));
    }

    public DataRowDto toDto() {

        DataRowDto dto = new DataRowDto();
        dto.columnData = values.stream().map(obj -> (obj != null) ? obj.toString() : NULL_VALUE).collect(Collectors.toList());

        return dto;
    }

    public Object getValueByName(String columnName, String baseTableName, String aliasTableName) {
        Objects.requireNonNull(aliasTableName);

        for (int i = 0; i < this.variableDescriptors.size(); i++) {
            VariableDescriptor desc = variableDescriptors.get(i);
            if ((nullSafeEqualsIgnoreCase(columnName, desc.getColumnName()) || nullSafeEqualsIgnoreCase(columnName, desc.getAliasColumnName()))
                    && (nullSafeEqualsIgnoreCase(baseTableName, desc.getTableName()) && nullSafeEqualsIgnoreCase(aliasTableName, desc.getAliasTableName())))
                return values.get(i);
        }

        throw new IllegalArgumentException("No variable called '" + columnName + "' for table '" + baseTableName + "/" + aliasTableName + "'");
    }
}
