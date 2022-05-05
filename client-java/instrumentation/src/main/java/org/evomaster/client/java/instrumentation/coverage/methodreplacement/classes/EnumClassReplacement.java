package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Field;

public class EnumClassReplacement implements MethodReplacementClass {


    @Override
    public Class<?> getTargetClass() {
        return Enum.class;
    }


    @Replacement(type = ReplacementType.TRACKER, replacingStatic = true, category = ReplacementCategory.EXT_0)
    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {



        if(enumType != null && name != null && ExecutionTracer.isTaintInput(name) ){
            try {
                Field values = enumType.getDeclaredField("$VALUES");
                values.setAccessible(true);
                Object[] entries = (Object[]) values.get(null);
                for(Object obj : entries){
                    ExecutionTracer.handleTaintForStringEquals(name, obj.toString(), false);
                }

            } catch (Exception e) {
                //should never happen, unless Java compiler changes
                throw new RuntimeException("BUG in EvoMaster", e);
            }
        }

        T x = Enum.valueOf(enumType, name);
        return x;
    }
}
