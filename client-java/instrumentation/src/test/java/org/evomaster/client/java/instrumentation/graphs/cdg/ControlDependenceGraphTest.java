package org.evomaster.client.java.instrumentation.graphs.cdg;

import org.evomaster.client.java.instrumentation.graphs.GraphPool;
import org.evomaster.client.java.instrumentation.graphs.cfg.ActualControlFlowGraph;
import org.evomaster.client.java.instrumentation.graphs.cfg.BasicBlock;
import org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction;
import org.evomaster.client.java.instrumentation.graphs.cfg.ControlDependency;
import org.evomaster.client.java.instrumentation.graphs.cfg.RawControlFlowGraph;
import org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch;
import org.evomaster.client.java.instrumentation.graphs.cfg.branch.BranchPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ControlDependenceGraph}.
 */
class ControlDependenceGraphTest {

    private static final ClassLoader TEST_LOADER = ControlDependenceGraphTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.GraphTestClass";
    private static final String METHOD_NAME = "sample()V";

    @AfterEach
    void resetPools() {
        resetBranchPool();
        resetGraphPool();
    }

    @Test
    void testConstructor() {
        GraphFixture fixture = GraphFixture.build();
        ControlDependenceGraph cdg = new ControlDependenceGraph(fixture.cfg);
        assertEquals(fixture.cfg.getClassName(), cdg.getClassName());
        assertEquals(fixture.cfg.getMethodName(), cdg.getMethodName());
        assertEquals(fixture.cfg, cdg.getCFG());

        // Synthetic entry block is the root node in the new graph. 
        BasicBlock syntheticEntry = cdg.vertexSet().stream()
                .filter(BasicBlock::isEntryBlock)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Synthetic entry block not found"));

        // Vertexes should match the CFG minus the exit block 
        Set<BasicBlock> expectedVertices = new java.util.HashSet<>(fixture.cfg.vertexSet());
        expectedVertices.remove(fixture.exitBlock);
        assertEquals(expectedVertices, cdg.vertexSet());
        
        // branch -> true path
        // branch -> false path
        assertTrue(cdg.containsEdge(fixture.branchBlock, fixture.trueBlock));
        assertTrue(cdg.containsEdge(fixture.branchBlock, fixture.falseBlock));

        // synthetic entry -> entry block
        // synthetic entry -> branch block
        assertTrue(cdg.containsEdge(syntheticEntry, fixture.entryBlock));
        assertTrue(cdg.containsEdge(syntheticEntry, fixture.branchBlock));

    }

    @Test
    void testGetControlDependentBranchesThrowsIllegalArgumentExceptionForNullBlock() {
        GraphFixture fixture = GraphFixture.build();
        ControlDependenceGraph cdg = new ControlDependenceGraph(fixture.cfg);
        assertThrows(IllegalArgumentException.class, () -> cdg.getControlDependentBranches(null));
    }

    @Test
    void testGetControlDependentBranchesThrowsUnknownBlockExceptionForABlockThaIsNotPartOfTheCFG() {
        GraphFixture fixture = GraphFixture.build();
        ControlDependenceGraph cdg = new ControlDependenceGraph(fixture.cfg);
        // Here we are creating a mock block that is not part of the fixture.cfg, so 
        BasicBlock foreign = org.mockito.Mockito.mock(BasicBlock.class);
        assertThrows(IllegalArgumentException.class, () -> cdg.getControlDependentBranches(foreign));
    }

    @Test
    void testGetControlDependenciesReturnsTheCorrectDependenciesForEveryBlock() {
        GraphFixture fixture = GraphFixture.build();
        ControlDependenceGraph cdg = new ControlDependenceGraph(fixture.cfg);

        // Entry block should have no dependencies
        Set<ControlDependency> entryDeps = cdg.getControlDependentBranches(fixture.entryBlock);
        assertTrue(entryDeps.isEmpty());

        // Branch block should have no dependencies
        Set<ControlDependency> branchDeps = cdg.getControlDependentBranches(fixture.branchBlock);
        assertTrue(branchDeps.isEmpty());

        // True block should have a dependency on the branch
        Set<ControlDependency> trueDeps = cdg.getControlDependentBranches(fixture.trueBlock);
        assertEquals(1, trueDeps.size());
        assertTrue(trueDeps.contains(new ControlDependency(fixture.branch, true)));

        // False block should have a dependency on the branch
        Set<ControlDependency> falseDeps = cdg.getControlDependentBranches(fixture.falseBlock);
        assertEquals(1, falseDeps.size());
        assertTrue(falseDeps.contains(new ControlDependency(fixture.branch, false)));
    }

    private static void resetBranchPool() {
        BranchPool.resetForTesting(TEST_LOADER);
    }

    private static void resetGraphPool() {
        GraphPool.resetForTesting(TEST_LOADER);
    }

    private static final class GraphFixture {
        final ActualControlFlowGraph cfg;
        final BasicBlock branchBlock;
        final BasicBlock trueBlock;
        final BasicBlock falseBlock;
        final BasicBlock exitBlock;
        final Branch branch;
        final BasicBlock entryBlock;

        private GraphFixture(ActualControlFlowGraph cfg,
                             BasicBlock branchBlock,
                             BasicBlock trueBlock,
                             BasicBlock falseBlock,
                             BasicBlock exitBlock,
                             Branch branch,
                             BasicBlock entryBlock) {
            this.cfg = cfg;
            this.branchBlock = branchBlock;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            this.exitBlock = exitBlock;
            this.branch = branch;
            this.entryBlock = entryBlock;
        }

        static GraphFixture build() {

            // entry -> branch
            // branch -> true path
            // branch -> false path
            // true path -> exit
            // false path -> exit

            TestRawControlFlowGraph raw = new TestRawControlFlowGraph();

            BytecodeInstruction entry = instruction(0, Opcodes.NOP);
            BytecodeInstruction branchInsn = branchInstruction(1);
            BytecodeInstruction falsePath = instruction(2, Opcodes.NOP);
            BytecodeInstruction truePath = instruction(10, Opcodes.NOP);
            BytecodeInstruction exit = instruction(20, Opcodes.RETURN);

            raw.addVertex(entry);
            raw.addVertex(branchInsn);
            raw.addVertex(falsePath);
            raw.addVertex(truePath);
            raw.addVertex(exit);

            raw.connect(entry, branchInsn);
            raw.connect(branchInsn, truePath);   // jump (true)
            raw.connect(branchInsn, falsePath);  // fall-through (false)
            raw.connect(truePath, exit);
            raw.connect(falsePath, exit);

            ActualControlFlowGraph cfg = new ActualControlFlowGraph(raw);

            BasicBlock entryBlock = findBlockContaining(cfg, entry);
            BasicBlock branchBlock = findBlockContaining(cfg, branchInsn);
            BasicBlock trueBlock = findBlockContaining(cfg, truePath);
            BasicBlock falseBlock = findBlockContaining(cfg, falsePath);
            BasicBlock exitBlock = cfg.vertexSet().stream()
                    .filter(BasicBlock::isExitBlock)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Exit block not found"));

            Branch branch = BranchPool.getInstance(TEST_LOADER).getBranchForInstruction(branchInsn);

            return new GraphFixture(cfg, branchBlock, trueBlock, falseBlock, exitBlock, branch, entryBlock);
        }

        private static final class TestRawControlFlowGraph extends RawControlFlowGraph {
            private TestRawControlFlowGraph() {
                super(TEST_LOADER, CLASS_NAME, METHOD_NAME, Opcodes.ACC_PUBLIC);
            }

            private void connect(BytecodeInstruction src, BytecodeInstruction dst) {
                super.addEdge(src, dst, false);
            }
        }

        private static BasicBlock findBlockContaining(ActualControlFlowGraph cfg, BytecodeInstruction instruction) {
            return cfg.vertexSet().stream()
                    .filter(bb -> bb.containsInstruction(instruction))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Block containing instruction not found"));
        }

        private static BytecodeInstruction instruction(int id, int opcode) {
            BytecodeInstruction instruction = new BytecodeInstruction(
                    TEST_LOADER,
                    CLASS_NAME,
                    METHOD_NAME,
                    id,
                    id,
                    new InsnNode(opcode));
            instruction.setLineNumber(100 + id);
            return instruction;
        }

        private static BytecodeInstruction branchInstruction(int id) {
            BytecodeInstruction instruction = new BytecodeInstruction(
                    TEST_LOADER,
                    CLASS_NAME,
                    METHOD_NAME,
                    id,
                    id,
                    new JumpInsnNode(Opcodes.IFEQ, new LabelNode()));
            instruction.setLineNumber(200 + id);
            BranchPool.getInstance(TEST_LOADER).registerAsBranch(instruction);
            return instruction;
        }
    }
}


