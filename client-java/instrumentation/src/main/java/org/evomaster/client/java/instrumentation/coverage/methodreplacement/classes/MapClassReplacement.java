package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class MapClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Map.class;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean containsKey(Map c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        // keyset() returns an set instance that indirectly calls
        // to containsKey(). In order to avoid an stack overflow
        // we compute a fresh collection
        Collection keyCollection = new HashSet<>(c.keySet());
        final boolean rv =keyCollection.contains(o);
        if (idTemplate==null) {
            return rv;
        }

        CollectionClassReplacement.containsHelper(keyCollection, o, idTemplate);
        return rv;
    }
}
