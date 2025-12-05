package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import static org.junit.jupiter.api.Assertions.*;

class BranchPoolTest {

    private static final ClassLoader TEST_LOADER = BranchPoolTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.BranchPoolFixture";
    private static final String METHOD_NAME = "sample()V";

    @AfterEach
    void resetPool() {
        BranchPool.resetForTesting(TEST_LOADER);
    }

    @Test
    void registerAsBranchRejectsNonBranchInstructions() {
        BranchPool pool = BranchPool.getInstance(TEST_LOADER);
        BytecodeInstruction notBranch = nonBranchInstruction(1);

        assertThrows(IllegalArgumentException.class, () -> pool.registerAsBranch(notBranch));
    }

    @Test
    void registerAsBranchAssignsDeterministicIdsAndObjectiveNamesCorrectly() {
        BranchPool pool = BranchPool.getInstance(TEST_LOADER);
        BytecodeInstruction first = branchInstruction(2, 150);
        BytecodeInstruction second = branchInstruction(3, 150); // same line to exercise ordinal handling

        pool.registerAsBranch(first);
        pool.registerAsBranch(second);

        Branch firstBranch = pool.getBranchForInstruction(first);
        Branch secondBranch = pool.getBranchForInstruction(second);

        assertEquals(1, firstBranch.getActualBranchId());
        assertEquals(2, secondBranch.getActualBranchId());

        assertEquals("Branch_at_com.example.BranchPoolFixture_at_line_00150_position_0_trueBranch_153",
                firstBranch.getThenObjectiveId());
        assertEquals("Branch_at_com.example.BranchPoolFixture_at_line_00150_position_0_falseBranch_153",
                firstBranch.getElseObjectiveId());
        assertEquals("Branch_at_com.example.BranchPoolFixture_at_line_00150_position_1_trueBranch_153",
                secondBranch.getThenObjectiveId());
        assertEquals("Branch_at_com.example.BranchPoolFixture_at_line_00150_position_1_falseBranch_153",
                secondBranch.getElseObjectiveId());
    }

    @Test
    void isKnownAsBranchReflectsRegistrationState() {
        BranchPool pool = BranchPool.getInstance(TEST_LOADER);
        BytecodeInstruction instruction = branchInstruction(4, 200);

        assertFalse(pool.isKnownAsBranch(instruction));

        pool.registerAsBranch(instruction);

        assertTrue(pool.isKnownAsBranch(instruction));
    }

    @Test
    void getBranchForInstructionRejectsUnknownBranches() {
        BranchPool pool = BranchPool.getInstance(TEST_LOADER);
        BytecodeInstruction instruction = branchInstruction(5, 220);

        assertThrows(IllegalArgumentException.class, () -> pool.getBranchForInstruction(instruction));
    }

    @Test
    void registeringSameInstructionTwiceReturnsExistingBranch() {
        BranchPool pool = BranchPool.getInstance(TEST_LOADER);
        BytecodeInstruction instruction = branchInstruction(6, 230);

        pool.registerAsBranch(instruction);
        Branch first = pool.getBranchForInstruction(instruction);

        pool.registerAsBranch(instruction);
        Branch second = pool.getBranchForInstruction(instruction);

        assertSame(first, second);
    }

    private static BytecodeInstruction branchInstruction(int instructionId, int lineNumber) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                METHOD_NAME,
                instructionId,
                instructionId,
                new JumpInsnNode(Opcodes.IFEQ, new LabelNode())
        );
        instruction.setLineNumber(lineNumber);
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
        instruction.setLineNumber(50 + instructionId);
        return instruction;
    }
}

