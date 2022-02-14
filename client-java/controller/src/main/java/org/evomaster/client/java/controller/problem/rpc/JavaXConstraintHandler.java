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
        if (cons.getSimpleName().equals("NotNull")){
            namedTypedValue.setNullable(false);
            return;
        }

        boolean solved = false;
        if (namedTypedValue instanceof PrimitiveOrWrapperParam){
            solved = handlePrimitiveOrWrapperParam((PrimitiveOrWrapperParam) namedTypedValue, annotation);
        } else if (namedTypedValue instanceof StringParam){
            solved = handleStringParam((StringParam) namedTypedValue, annotation);
        } else if (namedTypedValue instanceof CollectionParam){
            solved = handleCollection((CollectionParam) namedTypedValue, annotation);
        }  else if (namedTypedValue instanceof MapParam){
            solved = handleMapParam((MapParam) namedTypedValue, annotation);
        }

        if (!solved){
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its constraint "+ cons.getName());
//            throw new RuntimeException("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its constraint "+ cons.getName());
        }
    }

    private static boolean handlePrimitiveOrWrapperParam(PrimitiveOrWrapperParam param, Annotation annotation){
        Long max = handleMax(annotation);
        if (max != null){
            param.setMax(max);
            return true;
        }
        Long min = handleMin(annotation);
        if (min != null){
            param.setMin(min);
            return true;
        }
        return false;
    }

    private static boolean handleCollection(CollectionParam param, Annotation annotation){
        if (handleNotEmpty(annotation)){
            param.setNullable(false);
            param.setMinSize(1);
            return true;
        }

        Integer[] size = handleSize(annotation);
        if (size != null){
            //TODO if set size, should the value still be nullable?
            param.setMinSize(size[0]);
            param.setMaxSize(size[1]);
            return true;
        }
        return false;

    }

    private static boolean handleMapParam(MapParam param, Annotation annotation){
        if (handleNotEmpty(annotation)){
            param.setNullable(false);
            param.setMinSize(1);
            return true;
        }

        Integer[] size = handleSize(annotation);
        if (size != null){
            param.setMinSize(size[0]);
            param.setMaxSize(size[1]);
            return true;
        }

        return false;

    }

    private static boolean handleStringParam(StringParam param, Annotation annotation) {
        if (handleNotBlank(annotation)){
            param.setNullable(false);
            param.setMinSize(1);
            return true;
        }

        Integer[] size = handleSize(annotation);
        if (size != null){
            param.setMinSize(size[0]);
            param.setMaxSize(size[1]);
            return true;
        }

        Long max = handleMax(annotation);
        if (max != null){
            param.setMax(max);
            return true;
        }
        Long min = handleMin(annotation);
        if (min != null){
            param.setMin(min);
            return true;
        }

        String pattern = handlePattern(annotation);
        if (pattern != null){
            param.setPattern(pattern);
            return true;
        }

        return false;
    }

    private static boolean handleNotBlank(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        return cons.getSimpleName().equals("NotBlank");
    }

    private static boolean handleNotEmpty(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        return cons.getSimpleName().equals("NotEmpty");
    }

    private static Integer[] handleSize(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        /*
            based on https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Size.html
            null elements are considered valid.
         */
        if (cons.getSimpleName().equals("Size")){
            Integer[] size = new Integer[2];

            try {
                size[0] = (Integer) annotation.annotationType().getDeclaredMethod("min").invoke(annotation);
                size[1] = (Integer) annotation.annotationType().getDeclaredMethod("max").invoke(annotation);
                return size;
            } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
                throw new RuntimeException("ERROR: fail to process size");
            }

        }
        return null;
    }

    private static String handlePattern(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        /*
            based on https://docs.oracle.com/javaee/7/api/javax/validation/constraints/Pattern.html
            null elements are considered valid.
         */
        if (cons.getSimpleName().equals("Pattern")){

            try {
                return (String) annotation.annotationType().getDeclaredMethod("regexp").invoke(annotation);
            } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
                throw new RuntimeException("ERROR: fail to process regexp");
            }

        }
        return null;
    }

    private static Long handleMax(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        /*
            based on https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Max.html
            null elements are considered valid.
         */
        if (cons.getSimpleName().equals("Max")){
            try {
                return (Long) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);
            } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
                throw new RuntimeException("ERROR: fail to process max");
            }
        }
        return null;
    }

    private static Long handleMin(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        /*
            based on https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Min.html
            null elements are considered valid.
         */
        if (cons.getSimpleName().equals("Min")){
            try {
                return (Long) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);
            } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
                throw new RuntimeException("ERROR: fail to process min");
            }
        }
        return null;
    }


}
