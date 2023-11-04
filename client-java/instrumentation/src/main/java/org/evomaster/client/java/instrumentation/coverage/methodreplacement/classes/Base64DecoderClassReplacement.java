package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.Base64;
import java.util.Objects;

public class Base64DecoderClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Base64.Decoder.class;
    }

    /**
     * Source: <code>https://stackoverflow.com/questions/52923168/is-there-any-regular-expression-available-to-identify-whether-a-string-is-base64</code>
     */
    private final static String BASE64_REGEX = "^([A-Za-z0-9\\+/]{4})*([A-Za-z0-9\\+/]{4}|[A-Za-z0-9\\+/]{3}=|[A-Za-z0-9\\+/]{2}==)$";


    @Replacement(type = ReplacementType.EXCEPTION, category = ReplacementCategory.EXT_0)
    public static byte[] decode(Base64.Decoder caller, String src, String idTemplate) {
        Objects.requireNonNull(caller);

        if (src != null && ExecutionTracer.isTaintInput(src)) {
            ExecutionTracer.addStringSpecialization(src,
                    new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, BASE64_REGEX));
        }

        if (idTemplate == null) {
            return caller.decode(src);
        }

        try {
            byte[] res = caller.decode(src);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1d, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (IllegalArgumentException ex) {
            double distance = RegexDistanceUtils.getStandardDistance(src, BASE64_REGEX);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1d / (1d + distance), 1d));
            throw ex;
        }
    }


}
