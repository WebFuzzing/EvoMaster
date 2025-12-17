package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.cfg.branch.BranchPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RawControlFlowGraphTest {

    private static final ClassLoader TEST_LOADER = RawControlFlowGraphTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.RawCFGFixture";
    private static final String METHOD_NAME = "sample()V";

    @AfterEach
    void resetPools() {
        BranchPool.resetForTesting(TEST_LOADER);
    }

    @Test
    void addEdgeCreatesControlDependenciesForBranchEdges() {
        TestRawCFG cfg = new TestRawCFG();

        BytecodeInstruction branch = branchInstruction(10);
        BytecodeInstruction jumpTarget = instruction(30, Opcodes.NOP);
        BytecodeInstruction fallThrough = instruction(11, Opcodes.NOP);

        cfg.addVertex(branch);
        cfg.addVertex(jumpTarget);
        cfg.addVertex(fallThrough);

        ControlFlowEdge jumpEdge = cfg.connect(branch, jumpTarget);
        ControlFlowEdge fallEdge = cfg.connect(branch, fallThrough);

        assertTrue(jumpEdge.hasControlDependency());
        assertTrue(jumpEdge.getControlDependency().getBranchExpressionValue());
        assertFalse(fallEdge.getControlDependency().getBranchExpressionValue());
        assertSame(branch.toBranch(), jumpEdge.getBranchInstruction());
    }

    @Test
    void determineEntryAndExitPointsFallbackToMinAndMaxWhenNecessary() {
        TestRawCFG cfg = new TestRawCFG();
        BytecodeInstruction first = instruction(0, Opcodes.NOP);
        BytecodeInstruction second = instruction(1, Opcodes.NOP);

        cfg.addVertex(first);
        cfg.addVertex(second);

        cfg.connect(first, second);
        cfg.connect(second, first); // cycle -> no node with zero in-degree/out-degree

        assertSame(first, cfg.determineEntryPoint());

        Set<BytecodeInstruction> exits = cfg.determineExitPoints();
        assertEquals(1, exits.size());
        assertTrue(exits.contains(second));
    }

    @Test
    void determineBranchesAndJoinsReflectOutAndInDegrees() {
        TestRawCFG cfg = new TestRawCFG();
        BytecodeInstruction entry = instruction(0, Opcodes.NOP);
        BytecodeInstruction branch = branchInstruction(5);
        BytecodeInstruction truePath = instruction(6, Opcodes.NOP);
        BytecodeInstruction falsePath = instruction(7, Opcodes.NOP);
        BytecodeInstruction join = instruction(8, Opcodes.NOP);

        Arrays.asList(entry, branch, truePath, falsePath, join)
                .forEach(cfg::addVertex);

        cfg.connect(entry, branch);
        cfg.connect(branch, truePath);
        cfg.connect(branch, falsePath);
        cfg.connect(truePath, join);
        cfg.connect(falsePath, join);

        assertEquals(
                java.util.Collections.singleton(branch),
                cfg.determineBranches()
        );

        assertEquals(
                java.util.Collections.singleton(join),
                cfg.determineJoins()
        );
    }

    @Test
    void determineBasicBlockMergesLinearSequence() {
        TestRawCFG cfg = new TestRawCFG();
        BytecodeInstruction first = instruction(0, Opcodes.NOP);
        BytecodeInstruction middle = instruction(1, Opcodes.NOP);
        BytecodeInstruction last = instruction(2, Opcodes.RETURN);

        cfg.addVertex(first);
        cfg.addVertex(middle);
        cfg.addVertex(last);

        cfg.connect(first, middle);
        cfg.connect(middle, last);

        BasicBlock block = cfg.determineBasicBlockFor(middle);

        assertEquals(first, block.getFirstInstruction());
        assertEquals(last, block.getLastInstruction());
        java.util.List<BytecodeInstruction> collected = new java.util.ArrayList<>();
        block.forEach(collected::add);
        assertEquals(
                Arrays.asList(first, middle, last),
                collected
        );
    }

    private static BytecodeInstruction instruction(int instructionId, int opcode) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                METHOD_NAME,
                instructionId,
                instructionId,
                new InsnNode(opcode)
        );
        instruction.setLineNumber(100 + instructionId);
        return instruction;
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
        instruction.setLineNumber(200 + instructionId);
        BranchPool.getInstance(TEST_LOADER).registerAsBranch(instruction);
        return instruction;
    }

    private static final class TestRawCFG extends RawControlFlowGraph {
        private TestRawCFG() {
            super(TEST_LOADER, CLASS_NAME, METHOD_NAME, Opcodes.ACC_PUBLIC);
        }

        private ControlFlowEdge connect(BytecodeInstruction src, BytecodeInstruction dst) {
            return super.addEdge(src, dst, false);
        }
    }
}

