package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.Branch;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import static org.junit.jupiter.api.Assertions.*;

class ControlDependencyTest {

    private static final ClassLoader TEST_LOADER = ControlDependencyTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.ControlDependencyFixture";
    private static final String METHOD_NAME = "sample()V";

    @Test
    void constructorRequiresBranch() {
        assertThrows(IllegalArgumentException.class, () -> new ControlDependency(null, true));
    }

    @Test
    void gettersReflectBranchAndExpressionValue() {
        Branch branch = branch(1, 120);
        ControlDependency dependency = new ControlDependency(branch, true);

        assertSame(branch, dependency.getBranch());
        assertTrue(dependency.getBranchExpressionValue());
        assertTrue(dependency.toString().contains("TRUE"));

        ControlDependency falseDependency = new ControlDependency(branch, false);
        assertFalse(falseDependency.getBranchExpressionValue());
        assertTrue(falseDependency.toString().contains("FALSE"));
    }

    @Test
    void equalsAndHashCodeDependOnBranchAndValue() {
        Branch branchA = branch(2, 130);
        Branch branchB = branch(3, 140);

        ControlDependency depTrue = new ControlDependency(branchA, true);
        ControlDependency depTrueCopy = new ControlDependency(branchA, true);
        ControlDependency depFalse = new ControlDependency(branchA, false);
        ControlDependency depOtherBranch = new ControlDependency(branchB, true);

        assertEquals(depTrue, depTrueCopy);
        assertEquals(depTrue.hashCode(), depTrueCopy.hashCode());

        assertNotEquals(depTrue, depFalse);
        assertNotEquals(depTrue, depOtherBranch);
        assertNotEquals(depTrue, null);
        assertNotEquals(depTrue, "dependency");
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
        return new Branch(instruction, instructionId + 1);
    }
}

