package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.cfg.branch.BranchPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeInstructionPoolTest {

    private static final ClassLoader TEST_LOADER = BytecodeInstructionPoolTest.class.getClassLoader();
    private static final String CLASS_NAME_PREFIX = "com.example.BytecodeInstructionPoolFixture";
    private static final String METHOD_NAME_PREFIX = "sample";

    @AfterEach
    void resetBranchPool() {
        BranchPool.resetForTesting(TEST_LOADER);
    }

    @Test
    void registerMethodNodePopulatesMappingsAndBranches() {
        String className = uniqueClassName("register");
        String methodName = uniqueMethodName("register");
        BytecodeInstructionPool pool = BytecodeInstructionPool.getInstance(TEST_LOADER);
        MethodNode methodNode = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, methodName, "()V", null, null);

        LabelNode label = new LabelNode();
        methodNode.instructions.add(new LineNumberNode(100, label));
        methodNode.instructions.add(label);
        methodNode.instructions.add(new InsnNode(Opcodes.NOP));
        methodNode.instructions.add(new JumpInsnNode(Opcodes.IFEQ, new LabelNode()));
        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));

        List<BytecodeInstruction> instructions = pool.registerMethodNode(methodNode, className, methodName);

        assertEquals(5, instructions.size());
        assertTrue(instructions.get(2).hasLineNumberSet());
        assertEquals(100, instructions.get(2).getLineNumber());

        BytecodeInstruction branch = instructions.get(3);
        assertTrue(branch.isBranch());
        assertTrue(BranchPool.getInstance(TEST_LOADER).isKnownAsBranch(branch));

        List<BytecodeInstruction> byLookup = pool.getInstructionsIn(className, methodName);
        assertEquals(instructions, byLookup);
    }

    @Test
    void getInstructionByIdReturnsRegisteredInstruction() {
        String className = uniqueClassName("lookup");
        String methodName = uniqueMethodName("lookup");
        BytecodeInstructionPool pool = BytecodeInstructionPool.getInstance(TEST_LOADER);
        MethodNode methodNode = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, methodName, "()V", null, null);
        methodNode.instructions.add(new InsnNode(Opcodes.NOP));
        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
        pool.registerMethodNode(methodNode, className, methodName);

        BytecodeInstruction instruction = pool.getInstruction(className, methodName, 1);
        assertNotNull(instruction);
        assertEquals(Opcodes.RETURN, instruction.getASMNode().getOpcode());
    }

    @Test
    void forgetInstructionRemovesFromPool() {
        String className = uniqueClassName("forget");
        String methodName = uniqueMethodName("forget");
        BytecodeInstructionPool pool = BytecodeInstructionPool.getInstance(TEST_LOADER);
        MethodNode methodNode = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, methodName, "()V", null, null);
        methodNode.instructions.add(new InsnNode(Opcodes.NOP));
        pool.registerMethodNode(methodNode, className, methodName);

        BytecodeInstruction instruction = pool.getInstruction(className, methodName, 0);
        assertTrue(pool.forgetInstruction(instruction));
        assertTrue(pool.getInstructionsIn(className, methodName).isEmpty());
    }

    private static String uniqueClassName(String suffix) {
        return CLASS_NAME_PREFIX + "." + suffix + "." + System.nanoTime();
    }

    private static String uniqueMethodName(String suffix) {
        return METHOD_NAME_PREFIX + "_" + suffix + "_" + System.nanoTime();
    }
}

