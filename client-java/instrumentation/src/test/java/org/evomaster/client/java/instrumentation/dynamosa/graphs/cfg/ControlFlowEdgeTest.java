package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.Branch;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import static org.junit.jupiter.api.Assertions.*;

class ControlFlowEdgeTest {

    private static final ClassLoader TEST_LOADER = ControlFlowEdgeTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.ControlFlowEdgeFixture";
    private static final String METHOD_NAME = "sample()V";

    @Test
    void defaultConstructorProducesEdgeWithoutDependency() {
        ControlFlowEdge edge = new ControlFlowEdge();

        assertFalse(edge.hasControlDependency());
        assertNull(edge.getControlDependency());
        assertNull(edge.getBranchInstruction());
        assertTrue(edge.getBranchExpressionValue());
        assertFalse(edge.isExceptionEdge());
    }

    @Test
    void constructorWithDependencyStoresBranchAndExceptionFlag() {
        Branch branch = branch(1, 120);
        ControlDependency dependency = new ControlDependency(branch, false);

        ControlFlowEdge edge = new ControlFlowEdge(dependency, true);

        assertTrue(edge.hasControlDependency());
        assertSame(dependency, edge.getControlDependency());
        assertSame(branch, edge.getBranchInstruction());
        assertFalse(edge.getBranchExpressionValue());
        assertTrue(edge.isExceptionEdge());
        assertTrue(edge.toString().contains("FALSE"));
    }

    @Test
    void copyConstructorDuplicatesFlagsAndDependencyReference() {
        ControlFlowEdge original = new ControlFlowEdge(new ControlDependency(branch(2, 130), true), false);

        ControlFlowEdge copy = new ControlFlowEdge(original);

        assertNotSame(original, copy);
        assertEquals(original.hasControlDependency(), copy.hasControlDependency());
        assertSame(original.getControlDependency(), copy.getControlDependency());
        assertEquals(original.isExceptionEdge(), copy.isExceptionEdge());
    }

    @Test
    void booleanConstructorOnlySetsExceptionFlag() {
        ControlFlowEdge edge = new ControlFlowEdge(true);

        assertTrue(edge.isExceptionEdge());
        assertFalse(edge.hasControlDependency());
    }

    private static Branch branch(int instructionId, int lineNumber) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                METHOD_NAME,
                instructionId,
                instructionId,
                new JumpInsnNode(Opcodes.IFEQ, new LabelNode())
        );
        instruction.setLineNumber(lineNumber);
        return new Branch(instruction, instructionId + 10);
    }
}

