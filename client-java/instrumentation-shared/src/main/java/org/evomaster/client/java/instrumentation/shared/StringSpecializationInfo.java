package org.evomaster.client.java.instrumentation.shared;

import java.util.Objects;

public class StringSpecializationInfo {

    private final StringSpecialization stringSpecialization;

    private final String value;


    public StringSpecializationInfo(StringSpecialization stringSpecialization, String value) {
        this.stringSpecialization = Objects.requireNonNull(stringSpecialization);
        this.value = value;
    }

    public StringSpecialization getStringSpecialization() {
        return stringSpecialization;
    }

    public String getValue() {
        return value;
    }
}
