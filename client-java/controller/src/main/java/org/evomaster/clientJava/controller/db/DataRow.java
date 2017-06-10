package org.evomaster.clientJava.controller.db;

import java.util.*;
import java.util.stream.Collectors;

public class DataRow {

    private final List<VariableDescriptor> variableDescriptors;
    private final List<Object> values;


    public DataRow(String name, Object value) {
        this(Arrays.asList(new VariableDescriptor(name)), Arrays.asList(new Object[]{value}));
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

    public Object getValue(int index) {
        return values.get(index);
    }

    public Object getValueByName(String name) {
        return getValueByName(name, null);
    }

    public Object getValueByName(String name, String table) {
        Objects.requireNonNull(name);
        String n = name.trim();
        String t = (table == null ? null : table.trim());

        //first check aliases, but only if no specify table
        if (t == null || t.isEmpty()) {
            for (int i = 0; i < variableDescriptors.size(); i++) {
                VariableDescriptor desc = variableDescriptors.get(i);
                if (n.equalsIgnoreCase(desc.getAlias())) {
                    return values.get(i);
                }
            }
        }

        //if none, then check column names
        for (int i = 0; i < variableDescriptors.size(); i++) {
            VariableDescriptor desc = variableDescriptors.get(i);
            if (n.equalsIgnoreCase(desc.getColumnName()) &&
                    (t == null || t.isEmpty() || t.equalsIgnoreCase(desc.getTableName()))
                    ) {
                return values.get(i);
            }
        }

        throw new IllegalArgumentException("No variable called '" + name+"' for table '"+table+"'");
    }

    public String getAsLine() {
        return String.join(",", values.stream().map(obj -> obj.toString()).collect(Collectors.toList()));
    }
}
