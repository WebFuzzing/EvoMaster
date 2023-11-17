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

import static org.evomaster.client.java.instrumentation.JpaAnnotationName.*;

public class ClassAnalyzer {

    private static final List<String> JAKARTA_PERSISTENCE_LAYER_NAMES = Arrays.asList(JAKARTA_ENTITY_ANNOTATION_NAME, JAKARTA_NOT_NULL_ANNOTATION_NAME);
    private static final List<String> JAVAX_NAMES = Arrays.asList(JAVAX_ENTITY_ANNOTATION_NAME, JAVAX_NOT_NULL_ANNOTATION_NAME);

    /**
     * Try to load the given classes, and do different kind of analysis via reflection.
     * This can be helpful when doing such analysis at bytecode level (e.g., when intercepting class loader
     * with instrumentator) would be a bit complex
     * <p>
     * Note: this will have side effects on static data structures
     *
     * @param classNames the list of classes names to be analyzed
     */
    public static void doAnalyze(Collection<String> classNames) {

        // Java Persistence API
        boolean canUseJavaPersistenceApi = canUseJavaxJPA();

        // Jakarta Persistence
        boolean canUseJakartaPersistenceLayer;
        if (canUseJavaPersistenceApi) {
            // If JPA can be used, then JPL cannot be used
            canUseJakartaPersistenceLayer = false;
        } else {
            // Check if Jakarta Persistence Layer can be used
            canUseJakartaPersistenceLayer = canUseJakartaPersistenceLayer();
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
                if (canUseJavaPersistenceApi || canUseJakartaPersistenceLayer) {
                    NameSpace namespace;
                    if (canUseJavaPersistenceApi) {
                        namespace = NameSpace.JAVAX;
                    } else {
                        namespace = NameSpace.JAKARTA;
                    }
                    Objects.requireNonNull(namespace);
                    analyzeConstraints(klass, namespace);
                }
            } catch (Exception e) {
                SimpleLogger.error("Failed to analyze " + name, e);
            }
        }
    }

    private static boolean canUseJavaxJPA() {
        boolean canUseJavaxJPA = classesCanBeLoaded(JAVAX_NAMES);
        if (!canUseJavaxJPA) {
            SimpleLogger.info("Not analyzing JPA using javax package");
        }
        return canUseJavaxJPA;
    }

    private static boolean canUseJakartaPersistenceLayer() {
        boolean canUseJakartaPersistenceLayer = classesCanBeLoaded(JAKARTA_PERSISTENCE_LAYER_NAMES);
        if (!canUseJakartaPersistenceLayer) {
            SimpleLogger.info("Failed to load Jakarta Persistence Layer classes");
        }
        return canUseJakartaPersistenceLayer;
    }

    private static boolean classesCanBeLoaded(List<String> classNamesToBeLoaded) {
        try {
            ClassLoader loader = UnitsInfoRecorder.getInstance().getSutClassLoader();
            if (loader == null) {
                // could happen in tests
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

            JpaConstraint jpaConstraint = buildJpaConstraint(namespace, f, tableName, columnName);

            if (jpaConstraint.isMeaningful()) {
                UnitsInfoRecorder.registerNewJpaConstraint(jpaConstraint);
            }
        }
    }

    private static JpaConstraint buildJpaConstraint(NameSpace namespace, Field f, String tableName, String columnName)
            throws Exception {
        Objects.requireNonNull(namespace);
        return new JpaConstraintBuilder()
                .withTableName(tableName)
                .withColumnName(convertToSnakeCase(columnName))
                .withIsNullable(isNullableAnnotation(f, namespace))
                .withMinValue(getMinValue(f, namespace))
                .withMaxValue(getMaxValue(f, namespace))
                .withEnumValuesAsStrings(getEnumeratedAnnotation(f, namespace))
                .withDecimalMinValue(getDecimalMinValue(f, namespace))
                .withDecimalMaxValue(getDecimalMaxValue(f, namespace))
                .withIsNotBlank(isNotBlank(f, namespace))
                .withIsEmail(isEmail(f, namespace))
                .withIsNegative(isNegative(f, namespace))
                .withIsNegativeOrZero(isNegativeOrZero(f, namespace))
                .withIsPositive(isPositive(f, namespace))
                .withIsPositiveOrZero(isPositiveOrZero(f, namespace))
                .withIsFuture(isFuture(f, namespace))
                .withIsFutureOrPresent(isFutureOrPresent(f, namespace))
                .withIsPast(isPast(f, namespace))
                .withIsPastOrPresent(isPastOrPresent(f, namespace))
                .withIsAlwaysNull(isAlwaysNull(f, namespace))
                .withPatternRegExp(getPatterRegExp(f, namespace))
                .withSizeMin(getSizeMin(f, namespace))
                .withSizeMax(getSizeMax(f, namespace))
                .withDigitsInteger(getDigitsInteger(f, namespace))
                .withDigitsFraction(getDigitsFraction(f, namespace))
                .createJpaConstraint();
    }

    private static Annotation getColumnAnnotation(Field f, NameSpace namespace) {
        final String columnAnnotationName = getAnnotationName(namespace, JAVAX_COLUMN_ANNOTATION_NAME, JAKARTA_COLUMN_ANNOTATION_NAME);
        return getAnnotationByName(f, columnAnnotationName);
    }

    private static Annotation getTransientAnnotation(Field f, NameSpace namespace) {
        final String transientAnnotationName = getAnnotationName(namespace, JAVAX_TRANSIENT_ANNOTATION_NAME, JAKARTA_TRANSIENT_ANNOTATION_NAME);
        return getAnnotationByName(f, transientAnnotationName);
    }

    private static Annotation getTableAnnotation(Class<?> klass, NameSpace namespace) {
        final String tableAnnotationName = getAnnotationName(namespace, JAVAX_TABLE_ANNOTATION_NAME, JAKARTA_TABLE_ANNOTATION_NAME);
        return getAnnotationByName(klass, tableAnnotationName);
    }

    private static Annotation getEntityAnnotation(Class<?> klass, NameSpace namespace) {
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
        final String maxAnnotationName = getAnnotationName(namespace, JAVAX_MAX_ANNOTATION_NAME, JAKARTA_MAX_ANNOTATION_NAME);
        return getLongElement(f, maxAnnotationName);
    }

    /**
     * Returns a string with the literal value of a <code>javax.validation.constraints.Min</code> annotation on field f.
     *
     * @param f the reflection field that is annotated with the Min annotation.
     * @return the Long with the specific minimum value (as a literal) if the annotation is present, otherwise returns null.
     */
    private static Long getMinValue(Field f, NameSpace namespace) throws Exception {
        final String minAnnotationName = getAnnotationName(namespace, JAVAX_MIN_ANNOTATION_NAME, JAKARTA_MIN_ANNOTATION_NAME);
        return getLongElement(f, minAnnotationName);
    }

    private static String getDecimalMinValue(Field f, NameSpace namespace) throws Exception {
        final String decimalMinAnnotationName = getAnnotationName(namespace, JAVAX_DECIMAL_MIN_ANNOTATION_NAME, JAKARTA_DECIMAL_MIN_ANNOTATION_NAME);
        return getStringElement(f, decimalMinAnnotationName, "value");
    }

    private static String getDecimalMaxValue(Field f, NameSpace namespace) throws Exception {
        final String decimalMaxAnnotationName = getAnnotationName(namespace, JAVAX_DECIMAL_MAX_ANNOTATION_NAME, JAKARTA_DECIMAL_MAX_ANNOTATION_NAME);
        return getStringElement(f, decimalMaxAnnotationName, "value");
    }

    private static String getPatterRegExp(Field f, NameSpace namespace) throws Exception {
        final String patternAnnotationName = getAnnotationName(namespace, JAVAX_PATTERN_ANNOTATION_NAME, JAKARTA_PATTERN_ANNOTATION_NAME);
        return getStringElement(f, patternAnnotationName, "regexp");
    }
    /**
     * Gets the correct annotation name depending on using Javax or Jakarta
     */
    private static String getAnnotationName(NameSpace namespace, String javaxAnnotationName, String jakartaAnnotationName) {
        switch (namespace) {
            case JAVAX: {
                Objects.requireNonNull(javaxAnnotationName);
                assert (javaxAnnotationName.startsWith(JAVAX_PREFIX));
                return javaxAnnotationName;
            }
            case JAKARTA: {
                Objects.requireNonNull(jakartaAnnotationName);
                assert (jakartaAnnotationName.startsWith(JAKARTA_PREFIX));
                return jakartaAnnotationName;
            }
            default:
                throw new IllegalArgumentException("Unsupported namespace " + namespace);
        }
    }

    private static Long getLongElement(Field f, String annotationName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (Long) getElement(f, annotationName, "value");
    }

    private static String getStringElement(Field f, String annotationName, String elementName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (String) getElement(f, annotationName, elementName);
    }

    private static Integer getIntegerElement(Field f, String annotationName, String elementName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (Integer) getElement(f, annotationName, elementName);
    }

    private static Integer getSizeMin(Field f, NameSpace namespace) throws Exception {
        final String sizeAnnotationName = getAnnotationName(namespace, JAVAX_SIZE_ANNOTATION_NAME, JAKARTA_SIZE_ANNOTATION_NAME);
        return getIntegerElement(f, sizeAnnotationName, "min");
    }

    private static Integer getSizeMax(Field f, NameSpace namespace) throws Exception {
        final String sizeAnnotationName = getAnnotationName(namespace, JAVAX_SIZE_ANNOTATION_NAME, JAKARTA_SIZE_ANNOTATION_NAME);
        return getIntegerElement(f, sizeAnnotationName, "max");
    }

    private static Integer getDigitsInteger(Field f, NameSpace namespace) throws Exception {
        final String digitsAnnotationName = getAnnotationName(namespace, JAVAX_DIGITS_ANNOTATION_NAME, JAKARTA_DIGITS_ANNOTATION_NAME);
        return getIntegerElement(f, digitsAnnotationName, "integer");
    }

    private static Integer getDigitsFraction(Field f, NameSpace namespace) throws Exception {
        final String digitsAnnotationName = getAnnotationName(namespace, JAVAX_DIGITS_ANNOTATION_NAME, JAKARTA_DIGITS_ANNOTATION_NAME);
        return getIntegerElement(f, digitsAnnotationName, "fraction");
    }

    private static Object getElement(Field f, String annotationName, String elementName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object annotation = getAnnotationByName(f, annotationName);
        if (annotation != null) {
            return annotation.getClass().getMethod(elementName).invoke(annotation);
        }
        return null;
    }

    private static List<String> getEnumeratedAnnotation(Field f, NameSpace namespace)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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
     * @param namespace the namespace (e.g., javax.* or jakarta.*) for Java Persistence API (JPA) or Jakarta Persistence Layer (JPL)
     * @return false if the field is annotated as NotNull, otherwise it returns null
     */
    private static Boolean isNullableAnnotation(Field f, NameSpace namespace) {
        final String notNullAnnotationName = getAnnotationName(namespace, JAVAX_NOT_NULL_ANNOTATION_NAME, JAKARTA_NOT_NULL_ANNOTATION_NAME);
        if (f.getType().isPrimitive()
                || getAnnotationByName(f, notNullAnnotationName) != null) {
            return false;
        } else {
            return null;
        }
    }

    private static Boolean isNotBlank(Field f, NameSpace namespace) {
        final String notBlankAnnotationName = getAnnotationName(namespace, JAVAX_NOT_BLANK_ANNOTATION_NAME, JAKARTA_NOT_BLANK_ANNOTATION_NAME);
        return getIsAnnotationWith(f, notBlankAnnotationName);
    }

    private static Boolean isEmail(Field f, NameSpace namespace) {
        final String emailAnnotationName = getAnnotationName(namespace, JAVAX_EMAIL_ANNOTATION_NAME, JAKARTA_EMAIL_ANNOTATION_NAME);
        return getIsAnnotationWith(f, emailAnnotationName);
    }

    private static Boolean isPositive(Field f, NameSpace namespace) {
        final String positiveAnnotationName = getAnnotationName(namespace, JAVAX_POSITIVE_ANNOTATION_NAME, JAKARTA_POSITIVE_ANNOTATION_NAME);
        return getIsAnnotationWith(f, positiveAnnotationName);
    }

    private static Boolean isPositiveOrZero(Field f, NameSpace namespace) {
        final String positiveOrZeroAnnotationName = getAnnotationName(namespace, JAVAX_POSITIVE_OR_ZERO_ANNOTATION_NAME, JAKARTA_POSITIVE_OR_ZERO_ANNOTATION_NAME);
        return getIsAnnotationWith(f, positiveOrZeroAnnotationName);
    }

    private static Boolean isNegative(Field f, NameSpace namespace) {
        final String negativeAnnotationName = getAnnotationName(namespace, JAVAX_NEGATIVE_ANNOTATION_NAME, JAKARTA_NEGATIVE_ANNOTATION_NAME);
        return getIsAnnotationWith(f, negativeAnnotationName);
    }

    private static Boolean isNegativeOrZero(Field f, NameSpace namespace) {
        final String negativeOrZeroAnnotationName = getAnnotationName(namespace, JAVAX_NEGATIVE_OR_ZERO_ANNOTATION_NAME, JAKARTA_NEGATIVE_OR_ZERO_ANNOTATION_NAME);
        return getIsAnnotationWith(f, negativeOrZeroAnnotationName);
    }

    private static Boolean isPast(Field f, NameSpace namespace) {
        final String pastAnnotationName = getAnnotationName(namespace, JAVAX_PAST_ANNOTATION_NAME, JAKARTA_PAST_ANNOTATION_NAME);
        return getIsAnnotationWith(f, pastAnnotationName);
    }

    private static Boolean isPastOrPresent(Field f, NameSpace namespace) {
        final String pastOrPresentAnnotationName = getAnnotationName(namespace, JAVAX_PAST_OR_PRESENT_ANNOTATION_NAME, JAKARTA_PAST_OR_PRESENT_ANNOTATION_NAME);
        return getIsAnnotationWith(f, pastOrPresentAnnotationName);
    }

    private static Boolean isFuture(Field f, NameSpace namespace) {
        final String futureAnnotationName = getAnnotationName(namespace, JAVAX_FUTURE_ANNOTATION_NAME, JAKARTA_FUTURE_ANNOTATION_NAME);
        return getIsAnnotationWith(f, futureAnnotationName);
    }

    private static Boolean isFutureOrPresent(Field f, NameSpace namespace) {
        final String futureOrPresentAnnotationName = getAnnotationName(namespace, JAVAX_FUTURE_OR_PRESENT_ANNOTATION_NAME, JAKARTA_FUTURE_OR_PRESENT_ANNOTATION_NAME);
        return getIsAnnotationWith(f, futureOrPresentAnnotationName);
    }

    private static Boolean isAlwaysNull(Field f, NameSpace namespace) {
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
