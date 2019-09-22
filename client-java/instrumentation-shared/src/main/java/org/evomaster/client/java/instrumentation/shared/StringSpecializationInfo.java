package org.evomaster.client.java.instrumentation.shared;

import java.io.Serializable;
import java.util.Objects;

public class StringSpecializationInfo implements Serializable {

    private final StringSpecialization stringSpecialization;

    /**
     * A possible value to provide context to the specialization.
     * For example, if the specialization is a CONSTANT, then the "value" here would
     * the content of the constant
     */
    private final String value;

    private final TaintType type;

    public StringSpecializationInfo(StringSpecialization stringSpecialization, String value) {
        this(stringSpecialization, value, TaintType.FULL_MATCH);
    }

    public StringSpecializationInfo(StringSpecialization stringSpecialization, String value, TaintType taintType) {
        this.stringSpecialization = Objects.requireNonNull(stringSpecialization);
        this.value = value;
        if(taintType == null || taintType == TaintType.NONE){
            throw new IllegalArgumentException("Invalid type: "+taintType);
        }
        this.type = taintType;
    }

    public StringSpecialization getStringSpecialization() {
        return stringSpecialization;
    }

    public String getValue() {
        return value;
    }

    public TaintType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringSpecializationInfo that = (StringSpecializationInfo) o;
        return stringSpecialization == that.stringSpecialization &&
                Objects.equals(value, that.value) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringSpecialization, value, type);
    }
}
