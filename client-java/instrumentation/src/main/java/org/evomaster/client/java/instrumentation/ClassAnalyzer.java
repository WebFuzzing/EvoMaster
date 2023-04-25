package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ClassAnalyzer {

    /**
     * Try to load the given classes, and do different kind of analysis via reflection.
     * This can be helpful when doing such analysis at bytecode level (eg when intercepting class loader
     * with instrumentator) would be bit complex
     * <p>
     * Note: this will have side-effects on static data-structures
     *
     * @param classNames
     */
    public static void doAnalyze(Collection<String> classNames) {

        boolean jpa = canUseJavaxJPA();

        for (String name : classNames) {

            Class<?> klass;
            try {
                ClassLoader loader = UnitsInfoRecorder.getInstance().getFirstClassLoader(name);
                if (loader == null) {
                    //could happen in tests
                    loader = ClassAnalyzer.class.getClassLoader();
                    SimpleLogger.warn("No class loader registered for " + name);
                }
                klass = loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                SimpleLogger.error("Failed to load class " + name, e);
                continue;
            }

            try {
                if (jpa) {
                    analyzeJpaConstraints(klass);
                }
            } catch (Exception e) {
                SimpleLogger.error("Failed to analyze " + name, e);
            }
        }
    }

    private static boolean canUseJavaxJPA() {

        try {
            ClassLoader loader = UnitsInfoRecorder.getInstance().getSutClassLoader();
            if (loader == null) {
                //could happen in tests
                SimpleLogger.warn("No identified ClassLoader for SUT");
                loader = ClassAnalyzer.class.getClassLoader();
            }
            loader.loadClass("javax.persistence.Entity");
            loader.loadClass("javax.validation.constraints.NotNull");
            return true;
        } catch (ClassNotFoundException e) {
            SimpleLogger.info("Not analyzing JPA using javax package");
            return false;
        }
    }

    private static Annotation getAnnotationByName(Class<?> klass, String name) {
        return getAnnotationByName(klass.getAnnotations(), name);
    }

    private static Annotation getAnnotationByName(Field field, String name) {
        return getAnnotationByName(field.getAnnotations(), name);
    }


    private static Annotation getAnnotationByName(Annotation[] annotations, String name) {
        return Arrays.stream(annotations)
                .filter(a -> a.annotationType().getName().equals(name))
                .findFirst().orElse(null);
    }


    private static String convertToSnakeCase(String s) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return s.replaceAll(regex, replacement).toLowerCase();
    }

    private static void analyzeJpaConstraints(Class<?> klass) throws Exception {

        //TODO recall to handle new Jakarta namespace as well

        /*
            TODO what if current class ha no Entity, but extends a class using Entity?
            Rare, but could handle it
        */

//        Entity entity = klass.getAnnotation(Entity.class);
        Object entity = getAnnotationByName(klass, "javax.persistence.Entity");
        if (entity == null) {
            return; //nothing to do
        }

        String entityName = (String) entity.getClass().getMethod("name").invoke(entity);
        String tableName;

        /*
            Default table naming is a fucking mess in Hibernate/Spring...
            https://www.jpa-buddy.com/blog/hibernate-naming-strategies-jpa-specification-vs-springboot-opinionation/
         */

        //Table table = klass.getAnnotation(Table.class);
        Object table = getAnnotationByName(klass, "javax.persistence.Table");

        if (table != null) {
            tableName = (String) table.getClass().getMethod("name").invoke(table);
        } else if (entityName != null && !entityName.isEmpty()) {
            tableName = entityName;
        } else {
            tableName = klass.getSimpleName();
        }

        /*
            TODO guaranteed there are going to be edge cases in which this one ll not work... :(
            https://stackoverflow.com/questions/10310321/regex-for-converting-camelcase-to-camel-case-in-java

            for example, seen cases in which ExistingDataEntityX gets transformed into existing_data_entityx
            instead of existing_data_entity_x
         */
        tableName = convertToSnakeCase(tableName);


        //TODO: should check if need to consider getters as well (likely yes...)

        //TODO: this does NOT include fields in super-classes
        for (Field f : klass.getDeclaredFields()) {

            if (Modifier.isStatic(f.getModifiers())
                    || getAnnotationByName(f, "javax.persistence.Transient") != null) {
                /*
                    Likely other cases to skip
                 */
                continue;
            }

            /*
                TODO Following will not work for field that points to other Entities, eg

                @NotNull @OneToOne
                private ExistingDataEntityX x;

                as the names of column are different, eg "x_id"... :(
             */

            String columnName = null;
            //Column column = f.getAnnotation(Column.class);
            Object column = getAnnotationByName(f, "javax.persistence.Column");

            if (column != null) {
                columnName = (String) column.getClass().getMethod("name").invoke(column);
            }

            if (columnName == null || columnName.isEmpty()) {
                columnName = f.getName();
            }

            columnName = convertToSnakeCase(columnName);

            final Boolean isNullable = getIsNullableAnnotation(f);
            final List<String> enumValuesAsStrings = getEnumeratedAnnotation(f);
            final Long minValue = getMinValue(f);
            final Long maxValue = getMaxValue(f);

            final Boolean isNotBlank = getIsNotBlank(f);
            final Boolean isEmail = getEmail(f);
            final Boolean isNegative = getNegative(f);
            final Boolean isNegativeOrZero = getNegativeOrZero(f);
            final Boolean isPositive = getPositive(f);
            final Boolean isPositiveOrZero = getPositiveOrZero(f);

            final Boolean isPast = getPast(f);
            final Boolean isPastOrPresent = getPastOrPresent(f);
            final Boolean isFuture = getFuture(f);
            final Boolean isFutureOrPresent = getFutureOrPresent(f);
            final Boolean isAlwaysNull = getNullAnnotation(f);
            final String decimalMinValue = getDecimalMinValue(f);
            final String decimalMaxValue = getDecimalMaxValue(f);
            final String patternRegExp = getPatterRegExp(f);

            //TODO
            final Integer sizeMin = getSizeMin(f);
            final Integer sizeMax = getSizeMax(f);
            final Integer digitsInteger = getDigitsInteger(f);
            final Integer digitsFraction = getDigitsFraction(f);

            // ???
            final Boolean isOptional = null;


            JpaConstraint jpaConstraint = new JpaConstraint(
                    tableName,
                    columnName,
                    isNullable,
                    isOptional,
                    minValue,
                    maxValue,
                    enumValuesAsStrings,
                    decimalMinValue,
                    decimalMaxValue,
                    isNotBlank,
                    isEmail,
                    isNegative,
                    isNegativeOrZero,
                    isPositive,
                    isPositiveOrZero,
                    isFuture,
                    isFutureOrPresent,
                    isPast,
                    isPastOrPresent,
                    isAlwaysNull,
                    patternRegExp,
                    sizeMin,
                    sizeMax,
                    digitsInteger,
                    digitsFraction
            );
            if (jpaConstraint.isMeaningful()) {
                UnitsInfoRecorder.registerNewJpaConstraint(jpaConstraint);
            }
        }
    }

    /**
     * Returns a string with the literal value of a <code>javax.validation.constraints.Max</code> annotation on field f.
     *
     * @param f the reflection field that is annotated.
     * @return the Long with the specific maximum value (as a literal) if the annotation is present, otherwise returns null.
     */
    private static Long getMaxValue(Field f) throws Exception {
        return getLongElement(f, "javax.validation.constraints.Max", "value");
    }

    /**
     * Returns a string with the literal value of a <code>javax.validation.constraints.Min</code> annotation on field f.
     *
     * @param f the reflection field that is annotated with the Min annotation.
     * @return the Long with the specific minimum value (as a literal) if the annotation is present, otherwise returns null.
     */
    private static Long getMinValue(Field f) throws Exception {
        return getLongElement(f, "javax.validation.constraints.Min", "value");
    }

    private static String getDecimalMinValue(Field f) throws Exception {
        return getStringElement(f, "javax.validation.constraints.DecimalMin", "value");
    }

    private static String getDecimalMaxValue(Field f) throws Exception {
        return getStringElement(f, "javax.validation.constraints.DecimalMax", "value");
    }

    private static String getPatterRegExp(Field f) throws Exception {
        return getStringElement(f, "javax.validation.constraints.Pattern", "regexp");
    }

    private static Long getLongElement(Field f, String annotationName, String elementName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (Long) getElement(f, annotationName, elementName);

    }

    private static String getStringElement(Field f, String annotationName, String elementName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (String) getElement(f, annotationName, elementName);
    }

    private static Integer getIntegerElement(Field f, String annotationName, String elementName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (Integer) getElement(f, annotationName, elementName);
    }

    private static Integer getSizeMin(Field f) throws Exception {
        return getIntegerElement(f, "javax.validation.constraints.Size", "min");
    }

    private static Integer getSizeMax(Field f) throws Exception {
        return getIntegerElement(f, "javax.validation.constraints.Size", "max");
    }

    private static Integer getDigitsInteger(Field f) throws Exception {
        return getIntegerElement(f, "javax.validation.constraints.Digits", "integer");
    }

    private static Integer getDigitsFraction(Field f) throws Exception {
        return getIntegerElement(f, "javax.validation.constraints.Digits", "fraction");
    }


    private static Object getElement(Field f, String annotationName, String elementName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object annotation = getAnnotationByName(f, annotationName);
        if (annotation != null) {
            return annotation.getClass().getMethod(elementName).invoke(annotation);
        }
        return null;
    }


    private static List<String> getEnumeratedAnnotation(Field f) throws
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<String> enumValuesAsStrings = null;
        if (f.getType().isEnum()) {

            //TODO probably for enum of ints could just use a min-max range

            //Enumerated enumerated = f.getAnnotation(Enumerated.class);
            Object enumerated = getAnnotationByName(f, "javax.persistence.Enumerated");
            if (enumerated != null) {
                Object enumeratedValue = enumerated.getClass().getMethod("value").invoke(enumerated);
                String enumTypeString = "STRING".toLowerCase(); // EnumType.STRING
                if (enumeratedValue.toString().toLowerCase().equals(enumTypeString)) {
                    enumValuesAsStrings = Arrays.stream(f.getType().getEnumConstants())
                            .map(Object::toString)
                            .collect(Collectors.toList());
                }
            }
        }
        return enumValuesAsStrings;
    }

    /**
     * Returns a boolean if the <code>javax.validation.constraints.NotNull</code> annotation is present.
     *
     * @param f the target field of the entity.
     * @return false if the field is annotated as NotNull, otherwise it returns null
     */
    private static Boolean getIsNullableAnnotation(Field f) {
        final Boolean isNullable;
        if (f.getType().isPrimitive()
                || getAnnotationByName(f, "javax.validation.constraints.NotNull") != null) {
            isNullable = false;
        } else {
            isNullable = null;
        }
        return isNullable;
    }

    private static Boolean getIsNotBlank(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.NotBlank");
    }

    private static Boolean getEmail(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.Email");
    }

    private static Boolean getPositive(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.Positive");
    }

    private static Boolean getPositiveOrZero(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.PositiveOrZero");
    }

    private static Boolean getNegative(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.Negative");
    }

    private static Boolean getNegativeOrZero(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.NegativeOrZero");
    }

    private static Boolean getPast(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.Past");
    }

    private static Boolean getPastOrPresent(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.PastOrPresent");
    }

    private static Boolean getFuture(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.Future");
    }

    private static Boolean getFutureOrPresent(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.FutureOrPresent");
    }

    private static Boolean getNullAnnotation(Field f) {
        return getIsAnnotationWith(f, "javax.validation.constraints.Null");
    }

    private static Boolean getIsAnnotationWith(Field f, String annotationName) {
        final Boolean isAnnotated;
        if (getAnnotationByName(f, annotationName) != null) {
            isAnnotated = true;
        } else {
            isAnnotated = null;
        }
        return isAnnotated;
    }

}
