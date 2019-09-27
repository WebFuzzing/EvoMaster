package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.*;

public class MapClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Map.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean containsKey(Map c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        if (idTemplate == null || c instanceof IdentityHashMap) {
            /*
                IdentityHashMap does not use .equals() for the comparisons
             */
            return c.containsKey(o);
        }

        // keyset() returns an set instance that indirectly calls
        // to containsKey(). In order to avoid a stack overflow
        // we compute a fresh collection
        Collection keyCollection = new HashSet(c.keySet());

        return CollectionClassReplacement.containsHelper(keyCollection, o, idTemplate);
    }
}
