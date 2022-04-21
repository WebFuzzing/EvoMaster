package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;

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
            case DECIMAL_MAX:
            case MAX: solved = handleMax(namedTypedValue, annotation, supportType); break;
            case DECIMAL_MIN:
            case MIN: solved = handleMin(namedTypedValue, annotation, supportType); break;
            case DIGITS: solved = handleDigits(namedTypedValue, annotation); break;
            case POSITIVE:
            case POSITIVEORZERO:
            case NEGATIVE:
            case NEGATIVEORZERO: solved = handlePositiveOrNegative(namedTypedValue, supportType); break;
            default:
                SimpleLogger.error("ERROR: Not handle "+ supportType.annotation);
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

        //https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/NotEmpty.html

        if (namedTypedValue instanceof  CollectionParam){
            ((CollectionParam) namedTypedValue).setMinSize(1);
        } else if (namedTypedValue instanceof MapParam){
            ((MapParam) namedTypedValue).setMinSize(1);
        } else if(namedTypedValue instanceof StringParam) {
            ((StringParam) namedTypedValue).setMinSize(1);
        }else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its NotEmpty");
            return false;
        }

        return true;
    }

    private static boolean handleNotBlank(NamedTypedValue namedTypedValue){
        namedTypedValue.setNullable(false);

        /*
            based on https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/NotBlank.html
            NotBlank is applied to CharSequence
         */
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
            throw new RuntimeException("ERROR: fail to process Size "+e.getMessage());
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
            throw new RuntimeException("ERROR: fail to process regexp "+e.getMessage());
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

    private static boolean handleMax(NamedTypedValue namedTypedValue, Annotation annotation, JavaXConstraintSupportType supportType){

         /*
            based on
            https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Max.html
            https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/DecimalMax.html
            null elements are considered valid.
         */
        Long max = null;
        Boolean inclusive = true;
        try {
            // TODO might change long to BigDecimal
            if (supportType == JavaXConstraintSupportType.DECIMAL_MAX){
                String maxStr =  (String) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);
                max = Long.valueOf(maxStr);
                inclusive = (Boolean) annotation.annotationType().getDeclaredMethod("inclusive").invoke(annotation);
            }else
                max = (Long) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);

        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            throw new RuntimeException("ERROR: fail to process max "+e.getMessage());
        }

        if (max == null){
            SimpleLogger.error("ERROR: Max value is null");
            return false;
        }

        if (inclusive != null && !inclusive)
            max = max - 1;

        return setMax(namedTypedValue, max.toString(), true);
    }

    private static boolean handleMin(NamedTypedValue namedTypedValue, Annotation annotation, JavaXConstraintSupportType supportType){

        Long min = null;
        Boolean inclusive = true;
        try {
            // TODO might change long to BigDecimal
            if (supportType == JavaXConstraintSupportType.DECIMAL_MIN){
                String minStr = (String) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);
                min = Long.valueOf(minStr);
                inclusive = (Boolean) annotation.annotationType().getDeclaredMethod("inclusive").invoke(annotation);
            }else
                min = (Long) annotation.annotationType().getDeclaredMethod("value").invoke(annotation);

        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            throw new RuntimeException("ERROR: fail to process min "+e.getMessage());
        }

        if (min == null){
            SimpleLogger.error("ERROR: Min value is null");
            return false;
        }

        if (inclusive != null && !inclusive)
            min = min + 1;


        return setMin(namedTypedValue, min.toString(), true);
    }

    private static boolean setMin(NamedTypedValue namedTypedValue, String min, boolean inclusive){
        if (!(namedTypedValue instanceof MinMaxValue))
            SimpleLogger.error("ERROR: Can not set MinValue for the class "+ namedTypedValue.getType().getFullTypeName());

        if (namedTypedValue instanceof PrimitiveOrWrapperParam){
            ((PrimitiveOrWrapperParam)namedTypedValue).setMin(Long.parseLong(min));
            ((PrimitiveOrWrapperParam<?>) namedTypedValue).setMinInclusive(inclusive);
        } else if (namedTypedValue instanceof StringParam){
            ((StringParam)namedTypedValue).setMin(Long.parseLong(min));
            ((StringParam) namedTypedValue).setMinInclusive(inclusive);
        } else if (namedTypedValue instanceof  BigIntegerParam){
            ((BigIntegerParam) namedTypedValue).setMin(new BigInteger(min));
            ((BigIntegerParam) namedTypedValue).setMinInclusive(inclusive);
        } else if(namedTypedValue instanceof BigDecimalParam){
            ((BigDecimalParam) namedTypedValue).setMin(new BigDecimal(min));
            ((BigDecimalParam) namedTypedValue).setMinInclusive(inclusive);
        }else {
            SimpleLogger.error("ERROR: Can not solve constraints by setting Min value for the class "+ namedTypedValue.getType().getFullTypeName());
            return false;
        }

        return true;
    }

    private static boolean setMax(NamedTypedValue namedTypedValue, String max, boolean inclusive){
        if (!(namedTypedValue instanceof MinMaxValue))
            SimpleLogger.error("ERROR: Can not set MaxValue for the class "+ namedTypedValue.getType().getFullTypeName());

        if (namedTypedValue instanceof PrimitiveOrWrapperParam){
            ((PrimitiveOrWrapperParam)namedTypedValue).setMax(Long.parseLong(max));
            ((PrimitiveOrWrapperParam<?>) namedTypedValue).setMaxInclusive(inclusive);
        } else if (namedTypedValue instanceof StringParam){
            ((StringParam)namedTypedValue).setMax(Long.parseLong(max));
            ((StringParam) namedTypedValue).setMaxInclusive(inclusive);
        } else if (namedTypedValue instanceof  BigIntegerParam){
            ((BigIntegerParam) namedTypedValue).setMax(new BigInteger(max));
            ((BigIntegerParam) namedTypedValue).setMaxInclusive(inclusive);
        } else if(namedTypedValue instanceof BigDecimalParam){
            ((BigDecimalParam) namedTypedValue).setMax(new BigDecimal(max));
            ((BigDecimalParam) namedTypedValue).setMaxInclusive(inclusive);
        }else {
            SimpleLogger.error("ERROR: Can not solve constraints by setting Max value for the class "+ namedTypedValue.getType().getFullTypeName());
            return false;
        }

        return true;
    }

    /**
     * from https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Digits.html
     *
     * The annotated element must be a number within accepted range Supported types are:
     * BigDecimal
     * BigInteger
     * CharSequence
     * byte, short, int, long, and their respective wrapper types
     * null elements are considered valid.
     *
     * @return whether the constraint is handled
     */
    private static boolean handleDigits(NamedTypedValue namedTypedValue, Annotation annotation){
        if (namedTypedValue instanceof BigDecimalParam || namedTypedValue instanceof  BigIntegerParam
                || namedTypedValue instanceof StringParam || (namedTypedValue instanceof PrimitiveOrWrapperParam && ((PrimitiveOrWrapperType)namedTypedValue.getType()).isIntegralNumber())){
        try {
            int dInteger = (int) annotation.annotationType().getDeclaredMethod("integer").invoke(annotation);
            int dFraction = (int) annotation.annotationType().getDeclaredMethod("fraction").invoke(annotation);

            namedTypedValue.setPrecision(dInteger + dFraction);
            namedTypedValue.setScale(dFraction);

        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            throw new RuntimeException("ERROR: fail to process Digits ", e);
        }

        } else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its Digits");
            return false;
        }


        return false;
    }

    private static boolean handlePositiveOrNegative(NamedTypedValue namedTypedValue, JavaXConstraintSupportType supportType){
        if (namedTypedValue instanceof BigDecimalParam || namedTypedValue instanceof  BigIntegerParam
                || (namedTypedValue instanceof PrimitiveOrWrapperParam && ((PrimitiveOrWrapperType)namedTypedValue.getType()).isNumber())){
            String zero = "0";
            switch (supportType){
                case POSITIVE: setMin(namedTypedValue, zero, false); break;
                case POSITIVEORZERO: setMin(namedTypedValue, zero, true); break;
                case NEGATIVE: setMax(namedTypedValue, zero, false); break;
                case NEGATIVEORZERO: setMax(namedTypedValue, zero, true); break;
                default: throw new IllegalStateException("ERROR: constraint is not handled "+ supportType);
            }
        } else {
            SimpleLogger.error("ERROR: Do not solve class "+ namedTypedValue.getType().getFullTypeName() + " with its Digits");
            return false;
        }

        return true;
    }
}
