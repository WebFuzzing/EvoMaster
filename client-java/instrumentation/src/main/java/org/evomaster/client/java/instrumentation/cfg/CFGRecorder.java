package org.evomaster.client.java.instrumentation.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global recorder to store CFGs discovered at instrumentation time.
 * This mirrors how other static state (eg UnitsInfoRecorder) is kept for later retrieval.
 */
public class CFGRecorder {

    private static final Map<String, ControlFlowGraph> graphsByMethodId = new ConcurrentHashMap<>(16384);

    private static String methodKey(String classBytecodeName, String methodName, String descriptor) {
        return classBytecodeName + "#" + methodName + descriptor;
    }

    public static void register(ControlFlowGraph cfg) {
        Objects.requireNonNull(cfg);
        graphsByMethodId.put(methodKey(cfg.getClassName(), cfg.getMethodName(), cfg.getDescriptor()), cfg);
    }

    public static ControlFlowGraph get(String classBytecodeName, String methodName, String descriptor) {
        return graphsByMethodId.get(methodKey(classBytecodeName, methodName, descriptor));
    }

    public static List<ControlFlowGraph> getAll() {
        return new ArrayList<>(graphsByMethodId.values());
    }

    public static void reset() {
        graphsByMethodId.clear();
    }
}


