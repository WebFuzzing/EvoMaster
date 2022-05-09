package org.evomaster.client.java.controller.problem.rpc;

import java.util.Arrays;

public enum JavaXConstraintSupportType {
    NOT_NULL("NotNull"),
    NOT_EMPTY("NotEmpty"),
    NOT_BLANK("NotBlank"),
    SIZE("Size"),
    PATTERN("Pattern"),
    MAX("Max"),
    MIN("Min"),
    DECIMAL_MAX("DecimalMax"),
    DECIMAL_MIN("DecimalMin"),
    DIGITS("Digits"),
    POSITIVE("Positive"),
    POSITIVEORZERO("PositiveOrZero"),
    NEGATIVE("Negative"),
    NEGATIVEORZERO("NegativeOrZero"),
    ASSERTFALSE("AssertFalse"),
    ASSERTTRUE("AssertTrue"),
    NULL("Null")
    ;

    /*
    TODO https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-frame.html

    Email
    Future
    FutureOrPresent
    Past
    PastOrPresent
    *.List

     */


    public final String annotation;

    private JavaXConstraintSupportType(String annotation) {
        this.annotation = annotation;
    }

    public static JavaXConstraintSupportType getSupportType(String annotation){
        return Arrays.stream(values()).filter(s-> s.annotation.equals(annotation)).findAny().orElse(null);
    }

}
