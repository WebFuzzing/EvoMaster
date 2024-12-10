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

import java.net.*;

public class URIClassReplacement implements MethodReplacementClass {

    private static ThreadLocal<URI> instance = new ThreadLocal<>();

    @Override
    public Class<?> getTargetClass() {
        return java.net.URI.class;
    }

    public static URI consumeInstance(){

        URI uri = instance.get();
        if(uri == null){
            //this should never happen, unless bug in instrumentation, or edge case we didn't think of
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return uri;
    }

    private static void addInstance(URI x){
        URI uri = instance.get();
        if(uri != null){
            //this should never happen, unless bug in instrumentation, or edge case we didn't think of
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }


    @Replacement(
            type = ReplacementType.EXCEPTION,
            category = ReplacementCategory.EXT_0,
            replacingConstructor = true
    )
    public static void URI(String s, String idTemplate) throws  URISyntaxException {

        if (ExecutionTracer.isTaintInput(s)) {
            ExecutionTracer.addStringSpecialization(s,
                    new StringSpecializationInfo(StringSpecialization.URI, null));
        }

        URI uri;

        if (idTemplate == null) {
            uri =  new java.net.URI(s);
        } else {

            try {
                URI res = new java.net.URI(s);
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                        new Truthness(1, DistanceHelper.H_NOT_NULL));
                uri = res;
            } catch (RuntimeException e) {
                double h = s == null ? DistanceHelper.H_REACHED_BUT_NULL : DistanceHelper.H_NOT_NULL;
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
                throw e;
            }
        }

        addInstance(uri);
    }


    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = true, category = ReplacementCategory.EXT_0)
    public static URI create(String s, String idTemplate){

        if (ExecutionTracer.isTaintInput(s)) {
            ExecutionTracer.addStringSpecialization(s,
                    new StringSpecializationInfo(StringSpecialization.URI, null));
        }

        if (idTemplate == null) {
            return URI.create(s);
        }

        try {
            URI res = URI.create(s);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (RuntimeException e) {
            double h = s == null ? DistanceHelper.H_REACHED_BUT_NULL : DistanceHelper.H_NOT_NULL;
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }

    @Replacement(type = ReplacementType.EXCEPTION, replacingStatic = false, category = ReplacementCategory.EXT_0)
    public static URI resolve(URI caller, String s, String idTemplate){

        if (ExecutionTracer.isTaintInput(s)) {
            ExecutionTracer.addStringSpecialization(s,
                    new StringSpecializationInfo(StringSpecialization.URI, null));
        }

        if (idTemplate == null) {
            return caller.resolve(s);
        }

        try {
            URI res = caller.resolve(s);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (RuntimeException e) {
            double h = s == null ? DistanceHelper.H_REACHED_BUT_NULL : DistanceHelper.H_NOT_NULL;
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }
    }

}
