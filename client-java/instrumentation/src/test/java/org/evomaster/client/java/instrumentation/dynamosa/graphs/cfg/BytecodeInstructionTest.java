package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.Branch;
import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.BranchPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeInstructionTest {

    private static final ClassLoader TEST_LOADER = BytecodeInstructionTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.BytecodeInstructionFixture";
    private static final String METHOD_NAME = "sample()V";

    @AfterEach
    void resetBranchPool() {
        BranchPool.resetForTesting(TEST_LOADER);
    }

    @Test
    void constructorRejectsInvalidArguments() {
        AbstractInsnNodeStub node = new AbstractInsnNodeStub(Opcodes.NOP);
        assertThrows(IllegalArgumentException.class,
                () -> new BytecodeInstruction(TEST_LOADER, null, METHOD_NAME, 0, 0, node));
        assertThrows(IllegalArgumentException.class,
                () -> new BytecodeInstruction(TEST_LOADER, CLASS_NAME, METHOD_NAME, -1, 0, node));
        assertThrows(IllegalArgumentException.class,
                () -> new BytecodeInstruction(TEST_LOADER, CLASS_NAME, METHOD_NAME, 0, 0, null));
    }

    @Test
    void setBasicBlockValidatesClassAndSingleAssignment() {
        BytecodeInstruction instruction = instruction(1, new InsnNode(Opcodes.NOP));

        DummyBlock matching = new DummyBlock(CLASS_NAME, METHOD_NAME);
        instruction.setBasicBlock(matching);
        assertTrue(instruction.hasBasicBlockSet());
        assertSame(matching, instruction.getBasicBlock());

        DummyBlock wrongClass = new DummyBlock("other.Class", METHOD_NAME);
        assertThrows(IllegalArgumentException.class, () -> instruction.setBasicBlock(wrongClass));
        assertThrows(IllegalArgumentException.class, () -> instruction.setBasicBlock(matching));
    }

    @Test
    void lineNumberManagementHandlesManualAndAsmValues() {
        BytecodeInstruction manual = instruction(2, new InsnNode(Opcodes.NOP));
        manual.setLineNumber(150);
        assertEquals(150, manual.getLineNumber());

        BytecodeInstruction fromAsm = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                METHOD_NAME,
                3,
                3,
                new LineNumberNode(321, new LabelNode())
        );
        assertEquals(321, fromAsm.getLineNumber());
    }

    @Test
    void branchDetectionAndExplainUseBranchPoolInformation() {
        BytecodeInstruction branchInsn = instruction(4, new JumpInsnNode(Opcodes.IFEQ, new LabelNode()));
        BranchPool.getInstance(TEST_LOADER).registerAsBranch(branchInsn);

        assertTrue(branchInsn.isBranch());
        Branch branch = branchInsn.toBranch();
        assertNotNull(branch);

        String explanation = branchInsn.explain();
        assertTrue(explanation.contains("Branch"));
        assertTrue(explanation.contains("IFEQ"));
    }

    @Test
    void returnAndThrowDetectionWorks() {
        BytecodeInstruction returnInsn = instruction(5, new InsnNode(Opcodes.RETURN));
        assertTrue(returnInsn.isReturn());
        assertTrue(returnInsn.canReturnFromMethod());
        assertFalse(returnInsn.isThrow());

        BytecodeInstruction throwInsn = instruction(6, new InsnNode(Opcodes.ATHROW));
        assertFalse(throwInsn.isReturn());
        assertTrue(throwInsn.isThrow());
        assertTrue(throwInsn.canReturnFromMethod());
    }

    private static BytecodeInstruction instruction(int id, org.objectweb.asm.tree.AbstractInsnNode node) {
        return new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                METHOD_NAME,
                id,
                id,
                node
        );
    }

    private static final class DummyBlock extends BasicBlock {
        private DummyBlock(String className, String methodName) {
            super(className, methodName);
        }
    }

    private static final class AbstractInsnNodeStub extends org.objectweb.asm.tree.AbstractInsnNode {
        private AbstractInsnNodeStub(int opcode) {
            super(opcode);
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        public void accept(org.objectweb.asm.MethodVisitor methodVisitor) {
        }

        @Override
        public org.objectweb.asm.tree.AbstractInsnNode clone(java.util.Map<org.objectweb.asm.tree.LabelNode, org.objectweb.asm.tree.LabelNode> labels) {
            return new AbstractInsnNodeStub(opcode);
        }
    }
}

