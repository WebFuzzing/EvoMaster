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
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassAnalyzer {

    private static final String JAVAX_MAX_ANNOTATION_NAME = "javax.validation.constraints.Max";
    private static final String JAKARTA_MAX_ANNOTATION_NAME = "jakarta.validation.constraints.Max";
    private static final String JAVAX_MIN_ANNOTATION_NAME = "javax.validation.constraints.Min";
    private static final String JAKARTA_MIN_ANNOTATION_NAME = "jakarta.validation.constraints.Min";
    private static final String JAVAX_DECIMAL_MIN_ANNOTATION_NAME = "javax.validation.constraints.DecimalMin";
    private static final String JAKARTA_DECIMAL_MIN_ANNOTATION_NAME = "jakarta.validation.constraints.DecimalMin";
    private static final String JAVAX_DECIMAL_MAX_ANNOTATION_NAME = "javax.validation.constraints.DecimalMax";
    private static final String JAKARTA_DECIMAL_MAX_ANNOTATION_NAME = "jakarta.validation.constraints.DecimalMax";
    private static final String JAVAX_PATTERN_ANNOTATION_NAME = "javax.validation.constraints.Pattern";
    private static final String JAKARTA_PATTERN_ANNOTATION_NAME = "jakarta.validation.constraints.Pattern";
    private static final String JAVAX_NOT_BLANK_ANNOTATION_NAME = "javax.validation.constraints.NotBlank";
    private static final String JAKARTA_NOT_BLANK_ANNOTATION_NAME = "jakarta.validation.constraints.NotBlank";
    private static final String JAVAX_EMAIL_ANNOTATION_NAME = "javax.validation.constraints.Email";
    private static final String JAKARTA_EMAIL_ANNOTATION_NAME = "jakarta.validation.constraints.Email";
    private static final String JAVAX_NEGATIVE_ANNOTATION_NAME = "javax.validation.constraints.Negative";
    private static final String JAKARTA_NEGATIVE_ANNOTATION_NAME = "jakarta.validation.constraints.Negative";
    private static final String JAVAX_NEGATIVE_OR_ZERO_ANNOTATION_NAME = "javax.validation.constraints.NegativeOrZero";
    private static final String JAKARTA_NEGATIVE_OR_ZERO_ANNOTATION_NAME = "jakarta.validation.constraints.NegativeOrZero";
    private static final String JAVAX_POSITIVE_ANNOTATION_NAME = "javax.validation.constraints.Positive";
    private static final String JAKARTA_POSITIVE_ANNOTATION_NAME = "jakarta.validation.constraints.Positive";
    private static final String JAVAX_POSITIVE_OR_ZERO_ANNOTATION_NAME = "javax.validation.constraints.PositiveOrZero";
    private static final String JAKARTA_POSITIVE_OR_ZERO_ANNOTATION_NAME = "jakarta.validation.constraints.PositiveOrZero";
    private static final String JAVAX_PAST_ANNOTATION_NAME = "javax.validation.constraints.Past";
    private static final String JAKARTA_PAST_ANNOTATION_NAME = "jakarta.validation.constraints.Past";
    private static final String JAVAX_PAST_OR_PRESENT_ANNOTATION_NAME = "javax.validation.constraints.PastOrPresent";
    private static final String JAKARTA_PAST_OR_PRESENT_ANNOTATION_NAME = "jakarta.validation.constraints.PastOrPresent";
    private static final String JAVAX_FUTURE_ANNOTATION_NAME = "javax.validation.constraints.Future";
    private static final String JAKARTA_FUTURE_ANNOTATION_NAME = "jakarta.validation.constraints.Future";
    private static final String JAVAX_FUTURE_OR_PRESENT_ANNOTATION_NAME = "javax.validation.constraints.FutureOrPresent";
    private static final String JAKARTA_FUTURE_OR_PRESENT_ANNOTATION_NAME = "jakarta.validation.constraints.FutureOrPresent";
    private static final String JAVAX_NULL_ANNOTATION_NAME = "javax.validation.constraints.Null";
    private static final String JAKARTA_NULL_ANNOTATION_NAME = "jakarta.validation.constraints.Null";
    private static final String JAVAX_SIZE_ANNOTATION_NAME = "javax.validation.constraints.Size";
    private static final String JAKARTA_SIZE_ANNOTATION_NAME = "jakarta.validation.constraints.Size";
    private static final String JAVAX_DIGITS_ANNOTATION_NAME = "javax.validation.constraints.Digits";
    private static final String JAKARTA_DIGITS_ANNOTATION_NAME = "jakarta.validation.constraints.Digits";
    private static final String JAVAX_ENTITY_ANNOTATION_NAME = "javax.persistence.Entity";
    private static final String JAKARTA_ENTITY_ANNOTATION_NAME = "jakarta.persistence.Entity";
    private static final String JAVAX_TABLE_ANNOTATION_NAME = "javax.persistence.Table";
    private static final String JAKARTA_TABLE_ANNOTATION_NAME = "jakarta.persistence.Table";

    private static final String JAVAX_NOT_NULL_ANNOTATION_NAME = "javax.validation.constraints.NotNull";
    private static final String JAKARTA_NOT_NULL_ANNOTATION_NAME = "jakarta.validation.constraints.NotNull";
    public static final String JAVAX_COLUMN_ANNOTATION_NAME = "javax.persistence.Column";
    public static final String JAKARTA_COLUMN_ANNOTATION_NAME = "jakarta.persistence.Column";
    public static final String JAVAX_TRANSIENT_ANNOTATION_NAME = "javax.persistence.Transient";
    public static final String JAKARTA_TRANSIENT_ANNOTATION_NAME = "jakarta.persistence.Transient";
    public static final String JAVAX_ENUMERATED_ANNOTATION_NAME = "javax.persistence.Enumerated";
    public static final String JAKARTA_ENUMERATED_ANNOTATION_NAME = "jakarta.persistence.Enumerated";

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

        // Java Persistence API
        boolean canUseJavaPersistenceApi = canUseJavaxJPA();

        // Jakarta Persistence
        boolean useJakartaPersistenceLayer;
        if (canUseJavaPersistenceApi) {
            // If JPA can be used, then JPL cannot be used
            useJakartaPersistenceLayer = false;
        } else {
            // Check if Jakarta Persistence Layer can be used
            useJakartaPersistenceLayer = canUseJakartaPersistenceLayer();
        }

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
                if (canUseJavaPersistenceApi || useJakartaPersistenceLayer) {
                    NameSpace namespace;
                    if (canUseJavaPersistenceApi) {
                        namespace = NameSpace.JAVAX;
                    } else {
                        namespace = NameSpace.JAKARTA;
                    }
                    analyzeConstraints(klass, namespace);
                }
            } catch (Exception e) {
                SimpleLogger.error("Failed to analyze " + name, e);
            }
        }
    }

    private static boolean canUseJavaxJPA() {
        boolean canUseJavaxJPA = classesCanBeLoaded(Arrays.asList(JAVAX_ENTITY_ANNOTATION_NAME, JAVAX_NOT_NULL_ANNOTATION_NAME));
        if (!canUseJavaxJPA) {
            SimpleLogger.info("Not analyzing JPA using javax package");
        }
        return canUseJavaxJPA;
    }

    private static boolean canUseJakartaPersistenceLayer() {
        boolean canUseJakartaPersistenceLayer = classesCanBeLoaded(Arrays.asList(JAKARTA_ENTITY_ANNOTATION_NAME, JAKARTA_NOT_NULL_ANNOTATION_NAME));
        if (!canUseJakartaPersistenceLayer) {
            SimpleLogger.info("Failed to load Jakarta Persistence Layer classes");
        }
        return canUseJakartaPersistenceLayer;
    }

    private static boolean classesCanBeLoaded(List<String> classNamesToBeLoaded) {
        try {
            ClassLoader loader = UnitsInfoRecorder.getInstance().getSutClassLoader();
            if (loader == null) {
                //could happen in tests
                SimpleLogger.warn("No identified ClassLoader for SUT");
                loader = ClassAnalyzer.class.getClassLoader();
            }
            for (String className : classNamesToBeLoaded) {
                loader.loadClass(className);
            }
            return true;
        } catch (ClassNotFoundException e) {
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

    }

    enum NameSpace {JAVAX, JAKARTA}

    private static void analyzeConstraints(Class<?> klass, NameSpace namespace) throws Exception {

        /*
            TODO what if current class ha no Entity, but extends a class using Entity?
            Rare, but could handle it
        */

//        Entity entity = klass.getAnnotation(Entity.class);
        Object entityAnnotation = getEntityAnnotation(klass, namespace);
        if (entityAnnotation == null) {
            return; //nothing to do
        }

        String entityName = (String) entityAnnotation.getClass().getMethod("name").invoke(entityAnnotation);
        String tableName;

        /*
            Default table naming is a fucking mess in Hibernate/Spring...
            https://www.jpa-buddy.com/blog/hibernate-naming-strategies-jpa-specification-vs-springboot-opinionation/
         */

        //Table table = klass.getAnnotation(Table.class);
        Object tableAnnotation = getTableAnnotation(klass, namespace);

        if (tableAnnotation != null) {
            tableName = (String) tableAnnotation.getClass().getMethod("name").invoke(tableAnnotation);
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
                    || getTransientAnnotation(f, namespace) != null) {
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
            Object columnAnnotation = getColumnAnnotation(f, namespace);

            if (columnAnnotation != null) {
                columnName = (String) columnAnnotation.getClass().getMethod("name").invoke(columnAnnotation);
            }

            if (columnName == null || columnName.isEmpty()) {
                columnName = f.getName();
            }

            columnName = convertToSnakeCase(columnName);

            final Boolean isNullable = getIsNullableAnnotation(f, namespace);
            final List<String> enumValuesAsStrings = getEnumeratedAnnotation(f, namespace);
            final Long minValue = getMinValue(f, namespace);
            final Long maxValue = getMaxValue(f, namespace);

            final Boolean isNotBlank = getIsNotBlank(f, namespace);
            final Boolean isEmail = getEmail(f, namespace);
            final Boolean isNegative = getNegative(f, namespace);
            final Boolean isNegativeOrZero = getNegativeOrZero(f, namespace);
            final Boolean isPositive = getPositive(f, namespace);
            final Boolean isPositiveOrZero = getPositiveOrZero(f, namespace);

            final Boolean isPast = getPast(f, namespace);
            final Boolean isPastOrPresent = getPastOrPresent(f, namespace);
            final Boolean isFuture = getFuture(f, namespace);
            final Boolean isFutureOrPresent = getFutureOrPresent(f, namespace);
            final Boolean isAlwaysNull = getNullAnnotation(f, namespace);
            final String decimalMinValue = getDecimalMinValue(f, namespace);
            final String decimalMaxValue = getDecimalMaxValue(f, namespace);
            final String patternRegExp = getPatterRegExp(f, namespace);

            //TODO
            final Integer sizeMin = getSizeMin(f, namespace);
            final Integer sizeMax = getSizeMax(f, namespace);
            final Integer digitsInteger = getDigitsInteger(f, namespace);
            final Integer digitsFraction = getDigitsFraction(f, namespace);

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

    private static Annotation getColumnAnnotation(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String columnAnnotationName = getAnnotationName(namespace, JAVAX_COLUMN_ANNOTATION_NAME, JAKARTA_COLUMN_ANNOTATION_NAME);
        return getAnnotationByName(f, columnAnnotationName);
    }

    private static Annotation getTransientAnnotation(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String transientAnnotationName = getAnnotationName(namespace, JAVAX_TRANSIENT_ANNOTATION_NAME, JAKARTA_TRANSIENT_ANNOTATION_NAME);
        return getAnnotationByName(f, transientAnnotationName);
    }

    private static Annotation getTableAnnotation(Class<?> klass, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String tableAnnotationName = getAnnotationName(namespace, JAVAX_TABLE_ANNOTATION_NAME, JAKARTA_TABLE_ANNOTATION_NAME);
        return getAnnotationByName(klass, tableAnnotationName);
    }

    private static Annotation getEntityAnnotation(Class<?> klass, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String entityAnnotationName = getAnnotationName(namespace, JAVAX_ENTITY_ANNOTATION_NAME, JAKARTA_ENTITY_ANNOTATION_NAME);
        return getAnnotationByName(klass, entityAnnotationName);
    }

    /**
     * Returns a string with the literal value of a <code>javax.validation.constraints.Max</code> annotation on field f.
     *
     * @param f the reflection field that is annotated.
     * @return the Long with the specific maximum value (as a literal) if the annotation is present, otherwise returns null.
     */
    private static Long getMaxValue(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String maxAnnotationName = getAnnotationName(namespace, JAVAX_MAX_ANNOTATION_NAME, JAKARTA_MAX_ANNOTATION_NAME);
        return getLongElement(f, maxAnnotationName, "value");
    }

    /**
     * Returns a string with the literal value of a <code>javax.validation.constraints.Min</code> annotation on field f.
     *
     * @param f the reflection field that is annotated with the Min annotation.
     * @return the Long with the specific minimum value (as a literal) if the annotation is present, otherwise returns null.
     */
    private static Long getMinValue(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String minAnnotationName = getAnnotationName(namespace, JAVAX_MIN_ANNOTATION_NAME, JAKARTA_MIN_ANNOTATION_NAME);
        return getLongElement(f, minAnnotationName, "value");
    }

    private static String getDecimalMinValue(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String decimalMinAnnotationName = getAnnotationName(namespace, JAVAX_DECIMAL_MIN_ANNOTATION_NAME, JAKARTA_DECIMAL_MIN_ANNOTATION_NAME);
        return getStringElement(f, decimalMinAnnotationName, "value");
    }

    private static String getDecimalMaxValue(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String decimalMaxAnnotationName = getAnnotationName(namespace, JAVAX_DECIMAL_MAX_ANNOTATION_NAME, JAKARTA_DECIMAL_MAX_ANNOTATION_NAME);
        return getStringElement(f, decimalMaxAnnotationName, "value");
    }

    private static String getPatterRegExp(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String patternAnnotationName = getAnnotationName(namespace, JAVAX_PATTERN_ANNOTATION_NAME, JAKARTA_PATTERN_ANNOTATION_NAME);
        return getStringElement(f, patternAnnotationName, "regexp");
    }

    private static String getAnnotationName(NameSpace namespace, String javaxAnnotationName, String jakartaAnnotationName) {
        switch (namespace) {
            case JAVAX:
                return javaxAnnotationName;
            case JAKARTA:
                return jakartaAnnotationName;
            default:
                throw new IllegalArgumentException("Unsupported namespace " + namespace);
        }
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


    private static String getDigitsAnnotationName(NameSpace namespace) {
        final String digitsAnnotationName = getAnnotationName(namespace, JAVAX_DIGITS_ANNOTATION_NAME, JAKARTA_DIGITS_ANNOTATION_NAME);
        return digitsAnnotationName;
    }

    private static Integer getSizeMin(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String sizeAnnotationName = getAnnotationName(namespace, JAVAX_SIZE_ANNOTATION_NAME, JAKARTA_SIZE_ANNOTATION_NAME);
        return getIntegerElement(f, sizeAnnotationName, "min");
    }

    private static Integer getSizeMax(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String sizeAnnotationName = getAnnotationName(namespace, JAVAX_SIZE_ANNOTATION_NAME, JAKARTA_SIZE_ANNOTATION_NAME);
        return getIntegerElement(f, sizeAnnotationName, "max");
    }


    private static Integer getDigitsInteger(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String digitsAnnotationName = getAnnotationName(namespace, JAVAX_DIGITS_ANNOTATION_NAME, JAKARTA_DIGITS_ANNOTATION_NAME);
        return getIntegerElement(f, digitsAnnotationName, "integer");
    }

    private static Integer getDigitsFraction(Field f, NameSpace namespace) throws Exception {
        Objects.requireNonNull(namespace);
        final String digitsAnnotationName = getAnnotationName(namespace, JAVAX_DIGITS_ANNOTATION_NAME, JAKARTA_DIGITS_ANNOTATION_NAME);
        return getIntegerElement(f, digitsAnnotationName, "fraction");
    }


    private static Object getElement(Field f, String annotationName, String elementName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object annotation = getAnnotationByName(f, annotationName);
        if (annotation != null) {
            return annotation.getClass().getMethod(elementName).invoke(annotation);
        }
        return null;
    }


    private static List<String> getEnumeratedAnnotation(Field f, NameSpace namespace) throws
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Objects.requireNonNull(namespace);
        List<String> enumValuesAsStrings = null;
        if (f.getType().isEnum()) {

            //TODO probably for enum of ints could just use a min-max range

            //Enumerated enumerated = f.getAnnotation(Enumerated.class);
            final String enumeratedAnnotationName = getAnnotationName(namespace, JAVAX_ENUMERATED_ANNOTATION_NAME, JAKARTA_ENUMERATED_ANNOTATION_NAME);
            Object enumerated = getAnnotationByName(f, enumeratedAnnotationName);
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
     * @param f         the target field of the entity.
     * @param namespace
     * @return false if the field is annotated as NotNull, otherwise it returns null
     */
    private static Boolean getIsNullableAnnotation(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String notNullAnnotationName = getAnnotationName(namespace, JAVAX_NOT_NULL_ANNOTATION_NAME, JAKARTA_NOT_NULL_ANNOTATION_NAME);
        final Boolean isNullable;
        if (f.getType().isPrimitive()
                || getAnnotationByName(f, notNullAnnotationName) != null) {
            isNullable = false;
        } else {
            isNullable = null;
        }
        return isNullable;
    }

    private static Boolean getIsNotBlank(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String notBlankAnnotationName = getAnnotationName(namespace, JAVAX_NOT_BLANK_ANNOTATION_NAME, JAKARTA_NOT_BLANK_ANNOTATION_NAME);
        return getIsAnnotationWith(f, notBlankAnnotationName);
    }

    private static Boolean getEmail(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String emailAnnotationName = getAnnotationName(namespace, JAVAX_EMAIL_ANNOTATION_NAME, JAKARTA_EMAIL_ANNOTATION_NAME);
        return getIsAnnotationWith(f, emailAnnotationName);
    }

    private static Boolean getPositive(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String positiveAnnotationName = getAnnotationName(namespace, JAVAX_POSITIVE_ANNOTATION_NAME, JAKARTA_POSITIVE_ANNOTATION_NAME);
        return getIsAnnotationWith(f, positiveAnnotationName);
    }

    private static Boolean getPositiveOrZero(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String positiveOrZeroAnnotationName = getAnnotationName(namespace, JAVAX_POSITIVE_OR_ZERO_ANNOTATION_NAME, JAKARTA_POSITIVE_OR_ZERO_ANNOTATION_NAME);
        return getIsAnnotationWith(f, positiveOrZeroAnnotationName);
    }

    private static Boolean getNegative(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String negativeAnnotationName = getAnnotationName(namespace, JAVAX_NEGATIVE_ANNOTATION_NAME, JAKARTA_NEGATIVE_ANNOTATION_NAME);
        return getIsAnnotationWith(f, negativeAnnotationName);
    }

    private static Boolean getNegativeOrZero(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String negativeOrZeroAnnotationName = getAnnotationName(namespace, JAVAX_NEGATIVE_OR_ZERO_ANNOTATION_NAME, JAKARTA_NEGATIVE_OR_ZERO_ANNOTATION_NAME);
        return getIsAnnotationWith(f, negativeOrZeroAnnotationName);
    }

    private static Boolean getPast(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String pastAnnotationName = getAnnotationName(namespace, JAVAX_PAST_ANNOTATION_NAME, JAKARTA_PAST_ANNOTATION_NAME);
        return getIsAnnotationWith(f, pastAnnotationName);
    }

    private static Boolean getPastOrPresent(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String pastOrPresentAnnotationName = getAnnotationName(namespace, JAVAX_PAST_OR_PRESENT_ANNOTATION_NAME, JAKARTA_PAST_OR_PRESENT_ANNOTATION_NAME);
        return getIsAnnotationWith(f, pastOrPresentAnnotationName);
    }

    private static Boolean getFuture(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String futureAnnotationName = getAnnotationName(namespace, JAVAX_FUTURE_ANNOTATION_NAME, JAKARTA_FUTURE_ANNOTATION_NAME);
        return getIsAnnotationWith(f, futureAnnotationName);
    }

    private static Boolean getFutureOrPresent(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String futureOrPresentAnnotationName = getAnnotationName(namespace, JAVAX_FUTURE_OR_PRESENT_ANNOTATION_NAME, JAKARTA_FUTURE_OR_PRESENT_ANNOTATION_NAME);
        return getIsAnnotationWith(f, futureOrPresentAnnotationName);
    }

    private static Boolean getNullAnnotation(Field f, NameSpace namespace) {
        Objects.requireNonNull(namespace);
        final String nullAnnotationName = getAnnotationName(namespace, JAVAX_NULL_ANNOTATION_NAME, JAKARTA_NULL_ANNOTATION_NAME);
        return getIsAnnotationWith(f, nullAnnotationName);
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
