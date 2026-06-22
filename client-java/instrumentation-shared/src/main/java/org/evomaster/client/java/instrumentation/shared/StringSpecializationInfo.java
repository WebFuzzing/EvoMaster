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

    /**
     * External regex flags bitmask, as accepted by java.util.regex.Pattern.compile(String, int).
     * Only meaningful when stringSpecialization is a regex type.
     * Defaults to 0 (no external flags).
     */
    private final int regexFlags;

    public StringSpecializationInfo(StringSpecialization stringSpecialization, String value) {
        this(stringSpecialization, value, TaintType.FULL_MATCH);
    }

    public StringSpecializationInfo(StringSpecialization stringSpecialization, String value, TaintType taintType) {
        this(stringSpecialization, value, taintType, 0);
    }

    public StringSpecializationInfo(StringSpecialization stringSpecialization, String value, TaintType taintType, int regexFlags) {
        this.stringSpecialization = Objects.requireNonNull(stringSpecialization);
        this.value = value;
        if(taintType == null || taintType == TaintType.NONE){
            throw new IllegalArgumentException("Invalid type: "+taintType);
        }
        this.type = taintType;
        this.regexFlags = regexFlags;
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

    public int getRegexFlags() { return regexFlags; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringSpecializationInfo that = (StringSpecializationInfo) o;
        return stringSpecialization == that.stringSpecialization &&
                Objects.equals(value, that.value) &&
                type == that.type &&
                regexFlags == that.regexFlags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringSpecialization, value, type, regexFlags);
    }
}
