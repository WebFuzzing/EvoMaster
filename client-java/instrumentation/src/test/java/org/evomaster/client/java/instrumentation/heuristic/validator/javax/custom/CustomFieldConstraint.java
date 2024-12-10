package org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CustomFieldValidator.class)
public @interface CustomFieldConstraint {
    String message () default "Your custom message";
    Class<?>[] groups () default {};
    Class<? extends Payload>[] payload () default {};
}