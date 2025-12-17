package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.GraphPool;
import org.evomaster.client.java.instrumentation.graphs.cfg.branch.BranchPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;

import static org.junit.jupiter.api.Assertions.*;

class CFGGeneratorTest {

    private static final ClassLoader TEST_LOADER = CFGGeneratorTest.class.getClassLoader();
    private static final String CLASS_NAME_PREFIX = "com.example.CFGGeneratorFixture";
    private static final String METHOD_NAME_PREFIX = "sample";

    @AfterEach
    void resetPools() {
        GraphPool.resetForTesting(TEST_LOADER);
        BranchPool.resetForTesting(TEST_LOADER);
    }

    @Test
    void constructorRejectsNullArguments() {
        MethodNode method = simpleMethodNode();
        assertThrows(IllegalArgumentException.class, () -> new CFGGenerator(TEST_LOADER, null, "m()V", method));
        assertThrows(IllegalArgumentException.class, () -> new CFGGenerator(TEST_LOADER, "c", null, method));
        assertThrows(IllegalArgumentException.class, () -> new CFGGenerator(TEST_LOADER, "c", "m()V", null));
    }

    @Test
    void registerCFGsStoresGraphsInPool() {
        String className = uniqueClassName("store");
        String methodName = uniqueMethodName("store");
        MethodNode method = simpleMethodNode();

        CFGGenerator generator = new CFGGenerator(TEST_LOADER, className, methodName, method);

        // Frames in ASM are analysis snapshots of the JVM state at each instruction.
        // We don't care about them, but registerControlFlowEdge() requires them.
        // So, we create a dummy frame for each instruction.

        int size = method.instructions.size();
        Frame<?>[] frames = new Frame<?>[size];
        for (int i = 0; i < size; i++) {
            frames[i] = new Frame<>(0, 0);
        }
        for (int i = 0; i < size - 1; i++) {
            generator.registerControlFlowEdge(i, i + 1, frames, false);
        }

        generator.registerCFGs();

        RawControlFlowGraph raw = GraphPool.getInstance(TEST_LOADER).getRawCFG(className, methodName);
        ActualControlFlowGraph actual = GraphPool.getInstance(TEST_LOADER).getActualCFG(className, methodName);

        assertSame(generator.getRawGraph(), raw);
        assertNotNull(actual);
        assertFalse(raw.vertexSet().isEmpty());
        assertFalse(actual.vertexSet().isEmpty());
    }

    @Test
    void registerControlFlowEdgeRejectsNullFrames() {
        String className = uniqueClassName("nullFrames");
        String methodName = uniqueMethodName("nullFrames");
        MethodNode method = simpleMethodNode();

        CFGGenerator generator = new CFGGenerator(TEST_LOADER, className, methodName, method);

        assertThrows(IllegalArgumentException.class, () ->
                generator.registerControlFlowEdge(0, 1, null, false));
    }

    @Test
    void registerControlFlowEdgeSkipsUnreachableDestination() {
        String className = uniqueClassName("unreachable");
        String methodName = uniqueMethodName("unreachable");
        MethodNode method = simpleMethodNode();

        CFGGenerator generator = new CFGGenerator(TEST_LOADER, className, methodName, method);

        Frame<?>[] frames = new Frame<?>[2];
        generator.registerControlFlowEdge(0, 1, frames, false);

        assertTrue(generator.getRawGraph().edgeSet().isEmpty());
    }

    private static MethodNode simpleMethodNode() {
        MethodNode methodNode = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "dummy", "()V", null, null);

        LabelNode label = new LabelNode();
        methodNode.instructions.add(new LineNumberNode(100, label));
        methodNode.instructions.add(label);
        methodNode.instructions.add(new InsnNode(Opcodes.NOP));
        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));

        return methodNode;
    }

    private static String uniqueClassName(String suffix) {
        return CLASS_NAME_PREFIX + "." + suffix + "." + System.nanoTime();
    }

    private static String uniqueMethodName(String suffix) {
        return METHOD_NAME_PREFIX + "_" + suffix + "_" + System.nanoTime() + "()V";
    }
}

