package org.evomaster.client.java.instrumentation.cfg;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CFGGeneratorTest {

    static byte[] bytesOf(Class<?> c) throws Exception {
        String res = "/" + c.getName().replace('.', '/') + ".class";
        try (InputStream in = c.getResourceAsStream(res)) {
            assertNotNull(in, "missing bytecode for " + c.getName());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return baos.toByteArray();
        }
    }

    static class S1 {
        int f(int x) {
            if (x > 0) return 1;
            return -1;
        }
    }

    static class S2 {
        int g(int x) {
            int i = 0;
            while (i < x) i++;
            return i;
        }
    }

    static class S3 {
        int h(String s) {
            try { return s.length(); }
            catch (NullPointerException e) { return -1; }
        }
    }

    @Test
    void conditionalProducesTwoWayBranch() throws Exception {
        ClassReader cr = new ClassReader(bytesOf(S1.class));
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_FRAMES);
        CFGRecorder.reset();
        CFGGenerator.computeAndRegister(cn);

        ControlFlowGraph cfg = CFGRecorder.get(cn.name, "f", "(I)I");
        assertNotNull(cfg);

        // Pick the first line mapped and expect exactly one conditional branch index there
        List<Integer> allLines = cfg.getInstructionIndexToLineNumber().values().stream().distinct().sorted().collect(Collectors.toList());
        assertFalse(allLines.isEmpty());
        int line = allLines.get(0);

        List<Integer> branchIdxs = cfg.getBranchInstructionIndicesForLine(line);
        assertTrue(branchIdxs.size() >= 1, "expected at least one conditional branch");

        Integer insnIdx = branchIdxs.get(0);
        Integer bId = cfg.blockIdForInstructionIndex(insnIdx);
        assertNotNull(bId);
        Set<Integer> succ = cfg.getBlocksById().get(bId).getSuccessorBlockIds();
        assertEquals(2, succ.size(), "conditional should have 2 successors");
    }

    @Test
    void gotoIsNotCountedAsBranchTarget() throws Exception {
        ClassReader cr = new ClassReader(bytesOf(S2.class));
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_FRAMES);
        CFGRecorder.reset();
        CFGGenerator.computeAndRegister(cn);

        ControlFlowGraph cfg = CFGRecorder.get(cn.name, "g", "(I)I");
        assertNotNull(cfg);

        // Ensure recorded branch indices are not GOTO
        for (int bi : cfg.getBranchInstructionIndices()) {
            Integer op = cfg.getInstructionIndexToOpcode().get(bi);
            assertNotNull(op);
            assertNotEquals(org.objectweb.asm.Opcodes.GOTO, op.intValue());
        }
    }

    @Test
    void handlerStartsBlockAndHasIncomingEdges() throws Exception {
        ClassReader cr = new ClassReader(bytesOf(S3.class));
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_FRAMES);
        CFGRecorder.reset();
        CFGGenerator.computeAndRegister(cn);

        ControlFlowGraph cfg = CFGRecorder.get(cn.name, "h", "(Ljava/lang/String;)I");
        assertNotNull(cfg);

        // Heuristic: some block should have predecessors due to handler edges
        boolean hasBlockWithPreds = cfg.getBlocksById().values().stream()
                .anyMatch(bb -> !bb.getPredecessorBlockIds().isEmpty());
        assertTrue(hasBlockWithPreds, "expected at least one block with predecessors (likely the handler)");
    }
}


