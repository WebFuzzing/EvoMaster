package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

public class ObjectClassReplacement implements MethodReplacementClass {


    @Override
    public Class<?> getTargetClass() {
        return Object.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0)
    public static boolean equals(Object left, Object right, String idTemplate) {

        if(left == null) {
           left.equals(right); //throw NPE
        }

        return ObjectsClassReplacement.equals(left, right, idTemplate);
    }

}
