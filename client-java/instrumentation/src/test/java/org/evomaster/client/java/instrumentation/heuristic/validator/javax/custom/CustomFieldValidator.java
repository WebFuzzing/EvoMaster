package org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CustomFieldValidator  implements ConstraintValidator<CustomFieldConstraint, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value!=null && value.equals("foo");
    }
}
