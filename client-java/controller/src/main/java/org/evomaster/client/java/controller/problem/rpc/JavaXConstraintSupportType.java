package org.evomaster.client.java.controller.problem.rpc;

import java.util.Arrays;

public enum JavaXConstraintSupportType {
    NOT_NULL("NotNull"),
    NOT_EMPTY("NotEmpty"),
    NOT_BLANK("NotBlank"),
    SIZE("Size"),
    PATTERN("Pattern"),
    MAX("Max"),
    MIN("Min");

    public final String annotation;

    private JavaXConstraintSupportType(String annotation) {
        this.annotation = annotation;
    }

    public static JavaXConstraintSupportType getSupportType(String annotation){
        return Arrays.stream(values()).filter(s-> s.annotation.equals(annotation)).findAny().orElse(null);
    }

}
