package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.cfg.branch.BranchPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ActualControlFlowGraphTest {

    private static final ClassLoader TEST_LOADER = ActualControlFlowGraphTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.ActualCFGFixture";

    @AfterEach
    void resetPools() {
        BranchPool.resetForTesting(TEST_LOADER);
    }

    @Test
    void constructorBuildsBasicBlocksAndBranches() {
        GraphFixture fixture = GraphFixture.build("constructor");
        ActualControlFlowGraph cfg = new ActualControlFlowGraph(fixture.raw);

        BasicBlock entryBlock = cfg.vertexSet().stream()
                .filter(BasicBlock::isEntryBlock)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Synthetic entry block missing"));
        BasicBlock exitBlock = cfg.vertexSet().stream()
                .filter(BasicBlock::isExitBlock)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Synthetic exit block missing"));

        assertNotNull(entryBlock);
        assertNotNull(exitBlock);

        BasicBlock branchBlock = cfg.getBlockOf(fixture.branchInsn);
        assertNotNull(branchBlock);

        BasicBlock trueBlock = cfg.getBlockOf(fixture.trueInsn);
        BasicBlock falseBlock = cfg.getBlockOf(fixture.falseInsn);

        Set<BasicBlock> branchChildren = cfg.getChildren(branchBlock);
        assertEquals(2, branchChildren.size());
        assertTrue(branchChildren.contains(trueBlock));
        assertTrue(branchChildren.contains(falseBlock));

        assertEquals(1, cfg.getBranches().size());
        assertTrue(cfg.getBranches().contains(fixture.branchInsn));
    }

    @Test
    void computeReverseCFGPreservesVertexCount() {
        GraphFixture fixture = GraphFixture.build("reverse");
        ActualControlFlowGraph cfg = new ActualControlFlowGraph(fixture.raw);

        ActualControlFlowGraph reverse = cfg.computeReverseCFG();

        assertEquals(cfg.vertexCount(), reverse.vertexCount());
        assertNotNull(reverse.getBlockOf(fixture.branchInsn));
    }

    @Test
    void getBlockOfCachesBlocksAndGetInstructionUsesPool() {
        GraphFixture fixture = GraphFixture.build("lookup");
        ActualControlFlowGraph cfg = new ActualControlFlowGraph(fixture.raw);

        BytecodeInstruction target = fixture.trueInsn;
        BasicBlock first = cfg.getBlockOf(target);
        assertNotNull(first);
        BasicBlock second = cfg.getBlockOf(target);
        assertSame(first, second);

        BytecodeInstruction resolved = cfg.getInstruction(target.getInstructionId());
        assertSame(target, resolved);
    }

    private static final class GraphFixture {
        final RawControlFlowGraph raw;
        final BytecodeInstruction branchInsn;
        final BytecodeInstruction trueInsn;
        final BytecodeInstruction falseInsn;

        private GraphFixture(RawControlFlowGraph raw,
                             BytecodeInstruction branchInsn,
                             BytecodeInstruction trueInsn,
                             BytecodeInstruction falseInsn) {
            this.raw = raw;
            this.branchInsn = branchInsn;
            this.trueInsn = trueInsn;
            this.falseInsn = falseInsn;
        }

        static GraphFixture build(String methodSuffix) {
            String methodName = METHOD_NAME(methodSuffix);
            TestRawCFG raw = new TestRawCFG(methodName);

            BytecodeInstruction entry = instruction(methodName, 0, Opcodes.NOP);
            BytecodeInstruction branch = branchInstruction(methodName, 1);
            BytecodeInstruction truePath = instruction(methodName, 2, Opcodes.NOP);
            BytecodeInstruction falsePath = instruction(methodName, 3, Opcodes.NOP);
            BytecodeInstruction exit = instruction(methodName, 4, Opcodes.RETURN);

            raw.addVertex(entry);
            raw.addVertex(branch);
            raw.addVertex(truePath);
            raw.addVertex(falsePath);
            raw.addVertex(exit);

            raw.connect(entry, branch);
            raw.connect(branch, truePath);
            raw.connect(branch, falsePath);
            raw.connect(truePath, exit);
            raw.connect(falsePath, exit);

            return new GraphFixture(raw, branch, truePath, falsePath);
        }

        private static String METHOD_NAME(String suffix) {
            return "sample_" + suffix + "()V";
        }
    }

    private static BytecodeInstruction instruction(String methodName, int instructionId, int opcode) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                methodName,
                instructionId,
                instructionId,
                new InsnNode(opcode)
        );
        instruction.setLineNumber(100 + instructionId);
        BytecodeInstructionPool.getInstance(TEST_LOADER).registerInstruction(instruction);
        return instruction;
    }

    private static BytecodeInstruction branchInstruction(String methodName, int instructionId) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                CLASS_NAME,
                methodName,
                instructionId,
                instructionId,
                new JumpInsnNode(Opcodes.IFEQ, new LabelNode())
        );
        instruction.setLineNumber(200 + instructionId);
        BytecodeInstructionPool.getInstance(TEST_LOADER).registerInstruction(instruction);
        BranchPool.getInstance(TEST_LOADER).registerAsBranch(instruction);
        return instruction;
    }

    private static final class TestRawCFG extends RawControlFlowGraph {
        private TestRawCFG(String methodName) {
            super(TEST_LOADER, CLASS_NAME, methodName, Opcodes.ACC_PUBLIC);
        }

        private void connect(BytecodeInstruction src, BytecodeInstruction dst) {
            super.addEdge(src, dst, false);
        }
    }
}

