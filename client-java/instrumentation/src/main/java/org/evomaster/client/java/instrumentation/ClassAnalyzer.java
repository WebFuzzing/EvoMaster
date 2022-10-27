package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

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
                klass = ClassAnalyzer.class.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                SimpleLogger.error("Failed to load class " + name, e);
                continue;
            }

            if(jpa) {
                analyzeJpaConstraints(klass);
            }
        }
    }

    private static boolean canUseJavaxJPA(){

        try {
            ClassAnalyzer.class.getClassLoader().loadClass("javax.persistence.Entity");
            ClassAnalyzer.class.getClassLoader().loadClass("javax.validation.constraints.NotNull");
            return true;
        } catch (ClassNotFoundException e) {
            SimpleLogger.info("Not analyzing JPA using javax package");
            return false;
        }
    }


    private static String convertToSnakeCase(String s){
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return s.replaceAll(regex, replacement).toLowerCase();
    }

    private static void analyzeJpaConstraints(Class<?> klass){

        //TODO recall to handle new Jakarta namespace as well

        /*
            TODO what if current class ha no Entity, but extends a class using Entity?
            Rare, but could handle it
        */

        Entity entity = klass.getAnnotation(Entity.class);
        if(entity == null){
            return; //nothing to do
        }

        String tableName;

        /*
            Default table naming is a fucking mess in Hibernate/Spring...
            https://www.jpa-buddy.com/blog/hibernate-naming-strategies-jpa-specification-vs-springboot-opinionation/
         */

        Table table = klass.getAnnotation(Table.class);
        if(table != null){
            tableName = table.name();
        } else if(entity.name() != null && !entity.name().isEmpty()){
            tableName = entity.name();
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

            if(Modifier.isStatic(f.getModifiers()) || f.getAnnotation(Transient.class) != null){
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
            Column column = f.getAnnotation(Column.class);
            if(column != null){
                columnName = column.name();
            }

            if(columnName == null || columnName.isEmpty()){
                columnName = f.getName();
            }

            columnName = convertToSnakeCase(columnName);

            Boolean isNullable = null;
            if(f.getType().isPrimitive() || f.getAnnotation(NotNull.class)!=null){
                isNullable = false;
            }

            //TODO
            Boolean isOptional = null;
            String maxValue = null;
            String minValue = null;

            JpaConstraint jpaConstraint = new JpaConstraint(tableName,columnName,isNullable,isOptional,minValue,maxValue);
            if(jpaConstraint.isMeaningful()) {
                UnitsInfoRecorder.registerNewJpaConstraint(jpaConstraint);
            }
        }
    }
}
