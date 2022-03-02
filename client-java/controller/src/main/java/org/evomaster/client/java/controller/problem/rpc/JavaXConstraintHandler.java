package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

/**
 * this class is to handle constraints defined with javax.validation.constraints
 * following the link
 *         https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html
 */
public class JavaXConstraintHandler {

    /**
     * set constraints of the param based on the given annotation
     * @param namedTypedValue is the extracted param
     * @param annotation is the annotation to be handled
     */
    public static void handleParam(NamedTypedValue namedTypedValue, Annotation annotation){
        Class<?> cons = annotation.annotationType();

        JavaXConstraintSupportType supportType = JavaXConstraintSupportType.getSupportType(cons.getSimpleName());
        if (supportType == null){
            SimpleLogger.error("ERROR: Not handle constraints with a specified annotation:"+ cons.getName());
            return;
        }

        boolean solved = false;
        switch (supportType){
            case NOT_NULL: solved = handleNotNull(namedTypedValue); break;
            case NOT_EMPTY: solved = handleNotEmpty(namedTypedValue); break;
            case NOT_BLANK: solved = handleNotBlank(namedTypedValue); break;
            case SIZE: solved = handleSize(namedTypedValue, annotation); break;
            case PATTERN: solved = handlePattern(namedTypedValue, annotation); break;
            case MAX: solved = handleMax(namedTypedValue, annotation); break;
            case MIN: solved = handleMin(namedTypedValue, annotation); break;
        }

        if (!solved){
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its constraint "+ cons.getName());
//            throw new RuntimeException("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its constraint "+ cons.getName());
        }
    }


    private static boolean handleNotNull(NamedTypedValue namedTypedValue){
        namedTypedValue.setNullable(false);
        return true;
    }

    private static boolean handleNotEmpty(NamedTypedValue namedTypedValue){

        namedTypedValue.setNullable(false);

        if (namedTypedValue instanceof  CollectionParam){
            ((CollectionParam) namedTypedValue).setMinSize(1);
        } else if (namedTypedValue instanceof MapParam){
            ((MapParam) namedTypedValue).setMinSize(1);
        } else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its NotEmpty");
            return false;
        }

        return true;
    }

    private static boolean handleNotBlank(NamedTypedValue namedTypedValue){
        namedTypedValue.setNullable(false);

        if (namedTypedValue instanceof StringParam){
            ((StringParam)namedTypedValue).setMinSize(1);
        } else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its NotBlank");
            return false;
        }
        return true;
    }

    private static boolean handleSize(NamedTypedValue namedTypedValue, Annotation annotation){
         /*
            based on https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Size.html
            null elements are considered valid.
         */
        Integer[] size = new Integer[2];

        try {
            size[0] = (Integer) annotation.annotationType().getDeclaredMethod("min").invoke(annotation);
            size[1] = (Integer) annotation.annotationType().getDeclaredMethod("max").invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            throw new RuntimeException("ERROR: fail to process Size");
        }

        if (size[0] == null){
            SimpleLogger.error("ERROR: Size min is null");
            return false;
        }

        if (size[1] == null){
            SimpleLogger.error("ERROR: Size max is null");
            return false;
        }

        if (namedTypedValue instanceof  CollectionParam){
            ((CollectionParam) namedTypedValue).setMinSize(size[0]);
            ((CollectionParam) namedTypedValue).setMaxSize(size[1]);
        } else if (namedTypedValue instanceof MapParam){

            ((MapParam) namedTypedValue).setMinSize(size[0]);
            ((MapParam) namedTypedValue).setMaxSize(size[1]);
        } else if(namedTypedValue instanceof StringParam) {
            ((StringParam)namedTypedValue).setMinSize(size[0]);
            ((StringParam)namedTypedValue).setMaxSize(size[1]);
        } else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its Size");
            return false;
        }

        return true;
    }

    private static boolean handlePattern(NamedTypedValue namedTypedValue, Annotation annotation)  {
        /*
            based on https://docs.oracle.com/javaee/7/api/javax/validation/constraints/Pattern.html
            null elements are considered valid.
         */

        String pattern = null;
        try {
            pattern = (String) annotation.annotationType().getDeclaredMethod("regexp").invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            throw new RuntimeException("ERROR: fail to process regexp");
        }

        if (pattern == null){
            SimpleLogger.error("ERROR: Pattern regexp is null");
            return false;
        }

        if (namedTypedValue instanceof StringParam){
            ((StringParam)namedTypedValue).setPattern(pattern);
        }  else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its Size");
            return false;
        }
        return true;
    }

    private static boolean handleMax(NamedTypedValue namedTypedValue, Annotation annotation){

         /*
            based on https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Max.html
            null elements are considered valid.
         */
        Long max = null;
        try {
            max = (Long) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            throw new RuntimeException("ERROR: fail to process max");
        }

        if (max == null){
            SimpleLogger.error("ERROR: Max value is null");
            return false;
        }

        if (namedTypedValue instanceof PrimitiveOrWrapperParam){
            ((PrimitiveOrWrapperParam)namedTypedValue).setMax(max);
        } else if (namedTypedValue instanceof StringParam){
            ((StringParam)namedTypedValue).setMax(max);
        } else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its Max");
            return false;
        }

        return true;
    }

    private static boolean handleMin(NamedTypedValue namedTypedValue, Annotation annotation){

        Long min = null;
        try {
            min = (Long) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            throw new RuntimeException("ERROR: fail to process min");
        }

        if (min == null){
            SimpleLogger.error("ERROR: Min value is null");
            return false;
        }

        if (namedTypedValue instanceof PrimitiveOrWrapperParam){
            ((PrimitiveOrWrapperParam)namedTypedValue).setMin(min);
        } else if (namedTypedValue instanceof StringParam){
            ((StringParam)namedTypedValue).setMin(min);
        } else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its Min");
            return false;
        }

        return true;
    }
}
