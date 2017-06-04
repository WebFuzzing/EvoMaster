package org.evomaster.clientJava.controller.db;

import java.util.*;
import java.util.stream.Collectors;

public class DataRow {

    private final List<String> variableNames;
    private final List<Object> values = new ArrayList<>();

    public DataRow(String name, Object value) {
        this(Arrays.asList(name), Arrays.asList(new Object[]{value}));
    }

    public DataRow(List<String> names, List<Object> values) {
        Objects.requireNonNull(names);
        Objects.requireNonNull(values);

        if (names.size() != values.size()) {
            throw new IllegalArgumentException("Size mismatch");
        }

        List<String> tmp = new ArrayList<>();
        tmp.addAll(names);
        this.variableNames = Collections.unmodifiableList(tmp);
        this.values.addAll(values);
    }

    public List<String> getVariableNames(){
        return variableNames;
    }

    public Object getValue(int index) {
        return values.get(index);
    }

    public Object getValueByName(String name) {
        Objects.requireNonNull(name);

        for (int i = 0; i < variableNames.size(); i++) {
            if (variableNames.get(i).equalsIgnoreCase(name)) {
                return values.get(i);
            }
        }

        throw new IllegalArgumentException("No variable called: " + name);
    }

    public String getAsLine(){
        return String.join(",", values.stream().map(obj -> obj.toString()).collect(Collectors.toList()));
    }
}
