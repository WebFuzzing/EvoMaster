package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BasicBlockTest {

    private static final ClassLoader TEST_LOADER = BasicBlockTest.class.getClassLoader();
    private static final String CLASS_NAME = "com.example.BasicBlockFixture";
    private static final String METHOD_NAME = "sample()V";

    @Test
    void constructorRejectsNullArguments() {
        BytecodeInstruction instruction = instruction(CLASS_NAME, METHOD_NAME, 0, 100);
        List<BytecodeInstruction> nodes = Collections.singletonList(instruction);

        assertThrows(IllegalArgumentException.class,
                () -> new BasicBlock(TEST_LOADER, null, METHOD_NAME, nodes));
        assertThrows(IllegalArgumentException.class,
                () -> new BasicBlock(TEST_LOADER, CLASS_NAME, null, nodes));
        assertThrows(IllegalArgumentException.class,
                () -> new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, null));
    }

    @Test
    void constructorRejectsInvalidInstructionLists() {
        BytecodeInstruction valid = instruction(CLASS_NAME, METHOD_NAME, 1, 101);
        BytecodeInstruction otherClass = instruction("other.Class", METHOD_NAME, 2, 102);
        BytecodeInstruction otherMethod = instruction(CLASS_NAME, "other()V", 3, 103);
        BytecodeInstruction duplicate = instruction(CLASS_NAME, METHOD_NAME, 4, 104);

        assertThrows(IllegalArgumentException.class,
                () -> new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Arrays.asList(valid, otherClass)));
        assertThrows(IllegalArgumentException.class,
                () -> new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Arrays.asList(valid, otherMethod)));
        assertThrows(IllegalArgumentException.class,
                () -> new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Arrays.asList(duplicate, duplicate)));
    }

    @Test
    void constructorRejectsInstructionsAlreadyBelongingToAnotherBlock() {
        BytecodeInstruction shared = instruction(CLASS_NAME, METHOD_NAME, 5, 105);
        new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Collections.singletonList(shared));

        assertThrows(IllegalArgumentException.class,
                () -> new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Collections.singletonList(shared)));
    }

    @Test
    void exposesInstructionOrderLinesAndFlags() {
        BytecodeInstruction first = instruction(CLASS_NAME, METHOD_NAME, 6, 106);
        BytecodeInstruction second = instruction(CLASS_NAME, METHOD_NAME, 7, 207);
        BasicBlock block = new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Arrays.asList(first, second));

        assertSame(first, block.getFirstInstruction());
        assertSame(second, block.getLastInstruction());
        assertEquals(106, block.getFirstLine());
        assertEquals(207, block.getLastLine());
        assertTrue(block.containsInstruction(first));
        assertTrue(block.containsInstruction(second));

        List<BytecodeInstruction> iterated = new ArrayList<>();
        block.forEach(iterated::add);
        assertEquals(Arrays.asList(first, second), iterated);

        assertFalse(block.isEntryBlock());
        assertFalse(block.isExitBlock());
    }

    @Test
    void nameAndToStringReflectBlockIdentity() {
        BytecodeInstruction ins = instruction(CLASS_NAME, METHOD_NAME, 8, 208);
        BasicBlock block = new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Collections.singletonList(ins));

        assertTrue(block.getName().contains("BasicBlock"));
        assertTrue(block.toString().contains(ins.getInstructionType()));
        assertTrue(block.toString().contains("l208"));

        BasicBlock other = new BasicBlock(TEST_LOADER, CLASS_NAME, METHOD_NAME, Collections.singletonList(instruction(CLASS_NAME, METHOD_NAME, 9, 209)));
        assertNotEquals(block.getName(), other.getName());
        assertNotEquals(block, other);
    }

    private static BytecodeInstruction instruction(String className, String methodName, int id, int line) {
        BytecodeInstruction instruction = new BytecodeInstruction(
                TEST_LOADER,
                className,
                methodName,
                id,
                id,
                new InsnNode(Opcodes.NOP)
        );
        instruction.setLineNumber(line);
        return instruction;
    }
}

