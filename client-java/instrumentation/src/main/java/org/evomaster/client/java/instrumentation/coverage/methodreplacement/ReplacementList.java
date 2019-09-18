package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReplacementList {

    public static List<MethodReplacementClass> getList() {
        return Arrays.asList(
                new BooleanClassReplacement(),
                new CollectionClassReplacement(),
                new DateClassReplacement(),
                new DateFormatClassReplacement(),
                new DoubleClassReplacement(),
                new FloatClassReplacement(),
                new IntegerClassReplacement(),
                new LocalDateClassReplacement(),
                new LongClassReplacement(),
                new MapClassReplacement(),
                new MatcherClassReplacement(),
                new ObjectsClassReplacement(),
                new StringClassReplacement()
        );
    }

    public static List<MethodReplacementClass> getReplacements(Class<?> target) {
        Objects.requireNonNull(target);

        return getList().stream()
                .filter(t -> t.getTargetClass().isAssignableFrom(target))
                .collect(Collectors.toList());
    }
}
