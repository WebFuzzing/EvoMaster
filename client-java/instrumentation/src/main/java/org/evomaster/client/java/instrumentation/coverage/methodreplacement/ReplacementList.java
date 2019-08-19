package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.IntegerClassReplacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.LocalDateClassReplacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.StringClassReplacement;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReplacementList {

    public static List<MethodReplacementClass> getList() {
        return Arrays.asList(
                new IntegerClassReplacement(),
                new LocalDateClassReplacement(),
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
