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
     */
    public static void handlePossibleJsonTaint(String taint, Class<?> klass) {

        if (!ExecutionTracer.isTaintInput(taint)) {
            return;
        }

        StringSpecializationInfo info;

        try{
            if(List.class.isAssignableFrom(klass) || Set.class.isAssignableFrom(klass)){
                // TODO are there cases in which the content structure would be available? to check
                info = new StringSpecializationInfo(StringSpecialization.JSON_ARRAY,null);
            } else if (Map.class.isAssignableFrom(klass)){
                /*
                    might set schema value null for 2-phases taint if needed later
                    the value relates to add 1) tainted map (null) or 2) map (with schema) into string specification
                 */
                info = new StringSpecializationInfo(StringSpecialization.JSON_MAP, null);
            }else {
                info = new StringSpecializationInfo(StringSpecialization.JSON_OBJECT, ClassToSchema.getOrDeriveSchemaWithItsRef(klass));
            }

            ExecutionTracer.addStringSpecialization(taint, info);
        }catch (Exception e){
            SimpleLogger.warn("Fail to handle tainted value for "+klass.getName());
        }

    }
}
