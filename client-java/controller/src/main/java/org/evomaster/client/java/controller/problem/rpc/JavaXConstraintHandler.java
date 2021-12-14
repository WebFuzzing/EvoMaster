package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.problem.rpc.schema.params.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

/**
 * https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html
 */
public class JavaXConstraintHandler {

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

        if (!solved)
            throw new RuntimeException("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its constraint "+ cons.getName());
    }

    public static boolean handlePrimitiveOrWrapperParam(PrimitiveOrWrapperParam param, Annotation annotation){
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

    public static boolean handleCollection(CollectionParam param, Annotation annotation){
        if (handleNotEmpty(annotation)){
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

    public static boolean handleMapParam(MapParam param, Annotation annotation){
        if (handleNotEmpty(annotation)){
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

    public static boolean handleStringParam(StringParam param, Annotation annotation) {
        if (handleNotBlank(annotation)){
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

    public static boolean handleNotBlank(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        if (cons.getSimpleName().equals("NotBlank")){
            return true;
        }
        return false;
    }

    public static boolean handleNotEmpty(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        if (cons.getSimpleName().equals("NotEmpty")){
            return true;
        }
        return false;
    }

    public static Integer[] handleSize(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
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

    public static Long handleMax(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
        if (cons.getSimpleName().equals("Max")){
            try {
                return (Long) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);
            } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
                throw new RuntimeException("ERROR: fail to process max");
            }
        }
        return null;
    }

    public static Long handleMin(Annotation annotation)  {
        Class<?> cons = annotation.annotationType();
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
