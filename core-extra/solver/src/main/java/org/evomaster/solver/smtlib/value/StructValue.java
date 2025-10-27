package org.evomaster.solver.smtlib.value;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Struct value class to represent a struct with multiple fields
public class StructValue extends SMTLibValue {
    private final Map<String, SMTLibValue> fields;

    public StructValue(Map<String, SMTLibValue> fields) {
        this.fields = fields;
    }

    public Set<String> getFields() {
        return fields.keySet();
    }

    public SMTLibValue getField(String field) {
        return fields.get(field);
    }

    @Override
    public Map<String, SMTLibValue> getValue() {
        return fields;
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StructValue that = (StructValue) o;
        return Objects.equals(getFields(), that.getFields());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getFields());
    }
}