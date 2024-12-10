package org.evomaster.client.java.instrumentation.object;

import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonTaint {

    /**
     * Check if given input is a valid taint for a JSON element.
     *
     * @param isArray if true, then assuming that klass is the type of the array, and not the array itself
     */
    public static void handlePossibleJsonTaint(String taint, Class<?> klass, boolean isArray) {

        if (!ExecutionTracer.isTaintInput(taint)) {
            return;
        }

        StringSpecializationInfo info;


        try{
            if(isArray){
                info = new StringSpecializationInfo(StringSpecialization.JSON_ARRAY,
                        ClassToSchema.getOrDeriveSchemaWithItsRef(klass));
            } else if(List.class.isAssignableFrom(klass) || Set.class.isAssignableFrom(klass)){
                info = new StringSpecializationInfo(StringSpecialization.JSON_ARRAY,null);
            } else if (Map.class.isAssignableFrom(klass)){
                /*
                    TODO: might add schema if we have generic info later.
                    TODO if so, check and update MapDtoJacksonEMTest
                 */
                info = new StringSpecializationInfo(StringSpecialization.JSON_MAP, null);
            }else {
                info = new StringSpecializationInfo(StringSpecialization.JSON_OBJECT,
                        ClassToSchema.getOrDeriveSchemaWithItsRef(klass));
            }

            ExecutionTracer.addStringSpecialization(taint, info);
        }catch (Exception e){
            SimpleLogger.warn("Fail to handle tainted value for "+klass.getName());
        }

    }
}
