package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
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

        String tableName = entity.name();
        if(tableName == null || tableName.isEmpty()){
            tableName = klass.getSimpleName();
        }

        //TODO: should check if need to consider getters as well (likely yes...)

        //TODO: this does NOT include fields in super-classes
        for(Field f : klass.getDeclaredFields()){

            String columnName;
            Column column = f.getAnnotation(Column.class);
            if(column == null){
                columnName = f.getName();
            } else {
                columnName = column.name();
            }

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
