package org.evomaster.client.java.instrumentation.graphs.cfg.branch;

import org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import static org.junit.jupiter.api.Assertions.*;

class BranchTest {

    private static final ClassLoader TEST_LOADER = BranchTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.BranchFixture";
    private static final String METHOD_NAME = "sample()V";

    @Test
    void constructorRejectsNonBranchInstructions() {
        BytecodeInstruction notBranch = nonBranchInstruction(1);
        assertThrows(IllegalArgumentException.class, () -> new Branch(notBranch, 1));
    }

    @Test
    void constructorRejectsNonPositiveIds() {
        BytecodeInstruction instruction = branchInstruction(2);
        assertThrows(IllegalStateException.class, () -> new Branch(instruction, 0));
    }

    @Test
    void gettersExposeInstructionMetadata() {
        BytecodeInstruction instruction = branchInstruction(3);
        Branch branch = new Branch(instruction, 7);

        assertEquals(7, branch.getActualBranchId());
        assertSame(instruction, branch.getInstruction());
        assertEquals(CLASS_NAME, branch.getClassName());
        assertEquals(METHOD_NAME, branch.getMethodName());
    }

    @Test
    void objectiveIdsAreStoredAndReflectedInToString() {
        BytecodeInstruction instruction = branchInstruction(4);
        Branch branch = new Branch(instruction, 9);

        assertNull(branch.getThenObjectiveId());
        assertNull(branch.getElseObjectiveId());

        branch.setObjectiveIds("thenId", "elseId");

        assertEquals("thenId", branch.getThenObjectiveId());
        assertEquals("elseId", branch.getElseObjectiveId());

        String representation = branch.toString();
        assertTrue(representation.contains("I4"));
        assertTrue(representation.contains("Branch 9"));
        assertTrue(representation.contains("IFEQ"));
        assertTrue(representation.contains("L104"));
        assertTrue(representation.contains("[thenId, elseId]"));
    }

    @Test
    void equalsAndHashCodeDependOnInstructionAndId() {
        Branch reference = new Branch(branchInstruction(5), 11);
        Branch sameValues = new Branch(branchInstruction(5), 11);
        Branch differentId = new Branch(branchInstruction(5), 12);
        Branch differentInstruction = new Branch(branchInstruction(6), 11);

        assertEquals(reference, sameValues);
        assertEquals(reference.hashCode(), sameValues.hashCode());

        assertNotEquals(reference, differentId);
        assertNotEquals(reference, differentInstruction);
        assertNotEquals(reference, null);
        assertNotEquals(reference, "branch");
    }

    private static BytecodeInstruction branchInstruction(int instructionId) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                METHOD_NAME,
                instructionId,
                instructionId,
                new JumpInsnNode(Opcodes.IFEQ, new LabelNode())
        );
        instruction.setLineNumber(100 + instructionId);
        return instruction;
    }

    private static BytecodeInstruction nonBranchInstruction(int instructionId) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                METHOD_NAME,
                instructionId,
                instructionId,
                new InsnNode(Opcodes.NOP)
        );
        instruction.setLineNumber(10 + instructionId);
        return instruction;
    }
}


