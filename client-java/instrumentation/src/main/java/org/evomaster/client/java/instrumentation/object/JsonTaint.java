package org.evomaster.client.java.instrumentation.object;

import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.Collection;
import java.util.Map;

public class JsonTaint {

    /**
     * Check if given input is a valid taint for a JSON element.
     */
    public static void handlePossibleJsonTaint(String taint, Class<?> klass) {

        if (!ExecutionTracer.isTaintInput(taint)) {
            return;
        }

        StringSpecializationInfo info;

        try{
            if(Collection.class.isAssignableFrom(klass)){
                // TODO are there cases in which the content structure would be available? to check
                info = new StringSpecializationInfo(StringSpecialization.JSON_ARRAY,null);
            } else if (Map.class.isAssignableFrom(klass)){
                /*
                    might set schema value null, check later
                    it relates to add tainted map or map into string specification
                 */
                info = new StringSpecializationInfo(StringSpecialization.JSON_MAP, ClassToSchema.getOrDeriveSchemaWithItsRef(klass));
            }else {
                info = new StringSpecializationInfo(StringSpecialization.JSON_OBJECT, ClassToSchema.getOrDeriveSchemaWithItsRef(klass));
            }

            ExecutionTracer.addStringSpecialization(taint, info);
        }catch (Exception e){
            SimpleLogger.warn("Fail to handle tainted value for "+klass.getName());
        }

    }
}
