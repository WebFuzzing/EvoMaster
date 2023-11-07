package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.UUID;

public class UUIDClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return UUID.class;
    }

    @Replacement(replacingStatic = true, type = ReplacementType.EXCEPTION, category = ReplacementCategory.EXT_0)
    public static UUID fromString(String name, String idTemplate){

        if(ExecutionTracer.isTaintInput(name)){
            ExecutionTracer.addStringSpecialization(name,
                    new StringSpecializationInfo(StringSpecialization.UUID, name));
        }

        if (idTemplate == null) {
            return  UUID.fromString(name);
        }

        try {
            UUID res =  UUID.fromString(name);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (RuntimeException e) {
            double h = name == null ? DistanceHelper.H_REACHED_BUT_NULL : DistanceHelper.H_NOT_NULL;
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }
}
