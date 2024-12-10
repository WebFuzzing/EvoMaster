package org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom;

import org.evomaster.client.java.instrumentation.heuristic.validator.javax.ClassConstraintsBean;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ClassConstraintXZValidator implements ConstraintValidator<ClassConstraintXZ, ClassConstraintsBean> {

    @Override
    public boolean isValid (ClassConstraintsBean bean, ConstraintValidatorContext context) {
        return bean.z > bean.x;
    }
}