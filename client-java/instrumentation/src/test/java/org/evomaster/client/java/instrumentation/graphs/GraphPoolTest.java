package org.evomaster.client.java.instrumentation.graphs;

import org.evomaster.client.java.instrumentation.graphs.cfg.ActualControlFlowGraph;
import org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction;
import org.evomaster.client.java.instrumentation.graphs.cfg.RawControlFlowGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;

import static org.junit.jupiter.api.Assertions.*;

class GraphPoolTest {

    private static final ClassLoader TEST_LOADER = GraphPoolTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.GraphPoolFixture";
    private static final String METHOD_NAME = "sample()V";

    // Custom class loader that intentionally throws an exception when the ExecutionTracer class is loaded.
    private ClassLoader blockingLoader;

    @AfterEach
    void resetPools() {
        GraphPool.resetForTesting(TEST_LOADER);
        if (blockingLoader != null) {
            GraphPool.resetForTesting(blockingLoader);
            blockingLoader = null;
        }
    }

    @Test
    void getInstanceReturnsSingletonPerLoader() {
        GraphPool first = GraphPool.getInstance(TEST_LOADER);
        GraphPool second = GraphPool.getInstance(TEST_LOADER);
        assertSame(first, second);

        ClassLoader otherLoader = new SimpleDelegatingClassLoader(TEST_LOADER);
        GraphPool third = GraphPool.getInstance(otherLoader);
        assertNotSame(first, third);
        GraphPool.resetForTesting(otherLoader);
    }

    @Test
    void getRawCFGReturnsNullWhenClassIsUnknown() {
        GraphPool pool = GraphPool.getInstance(TEST_LOADER);
        assertNull(pool.getRawCFG("unknown.class", METHOD_NAME));
    }

    @Test
    void registerRawCFGStoresGraphUntilCleared() {
        GraphPool pool = GraphPool.getInstance(TEST_LOADER);
        RawControlFlowGraph raw = simpleRawGraph(TEST_LOADER, CLASS_NAME, METHOD_NAME);

        // Test that registerRawCFG and GetRawCFG works correctly
        pool.registerRawCFG(raw);
        assertSame(raw, pool.getRawCFG(CLASS_NAME, METHOD_NAME));

        // Test that clear works correctly
        pool.clear(CLASS_NAME, METHOD_NAME);
        assertNull(pool.getRawCFG(CLASS_NAME, METHOD_NAME));
    }

    @Test
    void registerActualCFGSkipsCdgWhenInstrumentationDisabled() {
        blockingLoader = new NoExecutionTracerClassLoader(GraphPoolTest.class.getClassLoader());
        GraphPool pool = GraphPool.getInstance(blockingLoader);
        RawControlFlowGraph raw = simpleRawGraph(blockingLoader, CLASS_NAME, METHOD_NAME);
        ActualControlFlowGraph cfg = new ActualControlFlowGraph(raw);

        pool.registerActualCFG(cfg);

        assertSame(cfg, pool.getActualCFG(CLASS_NAME, METHOD_NAME));
        assertNull(pool.getCDG(CLASS_NAME, METHOD_NAME));
    }

    @Test
    void resetForTestingDropsCachedInstancesAndGraphs() {
        GraphPool pool = GraphPool.getInstance(TEST_LOADER);
        RawControlFlowGraph raw = simpleRawGraph(TEST_LOADER, CLASS_NAME, METHOD_NAME);
        pool.registerRawCFG(raw);

        GraphPool.resetForTesting(TEST_LOADER);

        GraphPool fresh = GraphPool.getInstance(TEST_LOADER);
        assertNotSame(pool, fresh);
        assertNull(fresh.getRawCFG(CLASS_NAME, METHOD_NAME));
    }

    private static RawControlFlowGraph simpleRawGraph(ClassLoader loader,
                                                      String className,
                                                      String methodName) {
        TestRawControlFlowGraph raw = new TestRawControlFlowGraph(loader, className, methodName);

        BytecodeInstruction entry = instruction(loader, className, methodName, 0, Opcodes.NOP);
        BytecodeInstruction body = instruction(loader, className, methodName, 1, Opcodes.NOP);
        BytecodeInstruction exit = instruction(loader, className, methodName, 2, Opcodes.RETURN);

        raw.addVertex(entry);
        raw.addVertex(body);
        raw.addVertex(exit);

        raw.connect(entry, body);
        raw.connect(body, exit);

        return raw;
    }

    private static BytecodeInstruction instruction(ClassLoader loader,
                                                   String className,
                                                   String methodName,
                                                   int instructionId,
                                                   int opcode) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                loader,
                className,
                methodName,
                instructionId,
                instructionId,
                new InsnNode(opcode)
        );
        instruction.setLineNumber(100 + instructionId);
        return instruction;
    }

    private static final class TestRawControlFlowGraph extends RawControlFlowGraph {
        private TestRawControlFlowGraph(ClassLoader loader, String className, String methodName) {
            super(loader, className, methodName, Opcodes.ACC_PUBLIC);
        }

        private void connect(BytecodeInstruction src, BytecodeInstruction dst) {
            super.addEdge(src, dst, false);
        }
    }

    private static class SimpleDelegatingClassLoader extends ClassLoader {
        private SimpleDelegatingClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

    private static class NoExecutionTracerClassLoader extends ClassLoader {
        private NoExecutionTracerClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if ("org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer".equals(name)) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }
}

