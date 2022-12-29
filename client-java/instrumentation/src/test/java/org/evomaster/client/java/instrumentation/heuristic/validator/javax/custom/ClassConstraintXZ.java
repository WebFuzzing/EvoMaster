package org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ClassConstraintXZValidator.class)
public @interface ClassConstraintXZ {
    String message() default "Your custom message";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}