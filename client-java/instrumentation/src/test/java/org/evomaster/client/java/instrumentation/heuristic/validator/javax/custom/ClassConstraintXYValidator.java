package org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom;

import org.evomaster.client.java.instrumentation.heuristic.validator.javax.ClassConstraintsBean;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ClassConstraintXYValidator implements ConstraintValidator<ClassConstraintXY, ClassConstraintsBean> {

    @Override
    public boolean isValid (ClassConstraintsBean bean, ConstraintValidatorContext context) {
        return bean.x > 0 && bean.x == (2 * bean.y);
    }
}