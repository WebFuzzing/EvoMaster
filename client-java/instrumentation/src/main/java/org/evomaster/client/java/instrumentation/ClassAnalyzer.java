package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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
     *
     * Note: this will have side-effects on static data-structures
     *
     * @param classNames
     */
    public static void doAnalyze(Collection<String> classNames){

        boolean jpa = canUseJavaxJPA();

        for(String name : classNames){

            Class<?> klass;
            try {
                ClassLoader loader = UnitsInfoRecorder.getInstance().getFirstClassLoader(name);
                if(loader == null){
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
            }catch (Exception e){
                SimpleLogger.error("Failed to analyze " + name, e);
            }
        }
    }

    private static boolean canUseJavaxJPA(){

        try {
            ClassLoader loader = UnitsInfoRecorder.getInstance().getSutClassLoader();
            if(loader == null){
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

    private static Annotation getAnnotationByName(Class<?> klass, String name){
        return getAnnotationByName(klass.getAnnotations(), name);
    }

    private static Annotation getAnnotationByName(Field field, String name){
        return getAnnotationByName(field.getAnnotations(), name);
    }


    private static Annotation getAnnotationByName(Annotation[] annotations, String name){
        return Arrays.stream(annotations)
                .filter(a -> a.annotationType().getName().equals(name))
                .findFirst().orElse(null);
    }


    private static String convertToSnakeCase(String s){
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return s.replaceAll(regex, replacement).toLowerCase();
    }

    private static void analyzeJpaConstraints(Class<?> klass) throws Exception{

        //TODO recall to handle new Jakarta namespace as well

        /*
            TODO what if current class ha no Entity, but extends a class using Entity?
            Rare, but could handle it
        */

//        Entity entity = klass.getAnnotation(Entity.class);
        Object entity = getAnnotationByName(klass, "javax.persistence.Entity");
        if(entity == null){
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

        if(table != null){
            tableName = (String) table.getClass().getMethod("name").invoke(table);
        } else if(entityName != null && !entityName.isEmpty()){
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
        for(Field f : klass.getDeclaredFields()){

            if(Modifier.isStatic(f.getModifiers())
                    || getAnnotationByName(f, "javax.persistence.Transient") != null){
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

            if(column != null){
                columnName = (String) column.getClass().getMethod("name").invoke(column);
            }

            if(columnName == null || columnName.isEmpty()){
                columnName = f.getName();
            }

            columnName = convertToSnakeCase(columnName);

            Boolean isNullable = null;
            if(f.getType().isPrimitive()
                    || getAnnotationByName(f, "javax.validation.constraints.NotNull")!=null){
                isNullable = false;
            }

            List<String> enumValuesAsStrings = null;
            if(f.getType().isEnum()){

                //TODO probably for enum of ints could just use a min-max range

                //Enumerated enumerated = f.getAnnotation(Enumerated.class);
                Object enumerated = getAnnotationByName(f, "javax.persistence.Enumerated");
                Object enumeratedValue = enumerated.getClass().getMethod("value").invoke(enumerated);
                String enumTypeString = "STRING".toLowerCase(); // EnumType.STRING

                if(enumerated != null && enumeratedValue.toString().toLowerCase().equals(enumTypeString)){
                    enumValuesAsStrings = Arrays.stream(f.getType().getEnumConstants())
                            .map(e -> e.toString())
                            .collect(Collectors.toList());
                }
            }

            //TODO
            Boolean isOptional = null;
            String maxValue = null;
            String minValue = null;


            JpaConstraint jpaConstraint = new JpaConstraint(tableName,columnName,isNullable,isOptional,minValue,maxValue, enumValuesAsStrings);
            if(jpaConstraint.isMeaningful()) {
                UnitsInfoRecorder.registerNewJpaConstraint(jpaConstraint);
            }
        }
    }
}
