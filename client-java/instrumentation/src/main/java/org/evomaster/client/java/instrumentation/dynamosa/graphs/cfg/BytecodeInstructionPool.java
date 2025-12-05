/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.dynamosa.AnnotatedLabel;
import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.BranchPool;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

/**
 * <p>
 * BytecodeInstructionPool class.
 * </p>
 *
 */
public class BytecodeInstructionPool {

    private static final Map<ClassLoader, BytecodeInstructionPool> instanceMap = new LinkedHashMap<>();

    private final ClassLoader classLoader;

    private BytecodeInstructionPool(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static BytecodeInstructionPool getInstance(ClassLoader classLoader) {
        if (!instanceMap.containsKey(classLoader)) {
            instanceMap.put(classLoader, new BytecodeInstructionPool(classLoader));
        }

        return instanceMap.get(classLoader);
    }

    // maps className -> method inside that class -> list of
    // BytecodeInstructions
    private final Map<String, Map<String, List<BytecodeInstruction>>> instructionMap = new LinkedHashMap<>();

    // fill the pool

    /**
     * Called by each CFGGenerator for it's corresponding method.
     * <p>
     * The MethodNode contains all instructions within a method. A call to
     * registerMethodNode() fills the instructionMap of the
     * BytecodeInstructionPool with the instructions in that method and returns
     * a List containing the BytecodeInstructions within that method.
     * <p>
     * While registering all instructions the lineNumber of each
     * BytecodeInstruction is set.
     *
     * @param node       a {@link org.objectweb.asm.tree.MethodNode} object.
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<BytecodeInstruction> registerMethodNode(MethodNode node,
                                                        String className, String methodName) {
        int lastLineNumber = -1;
        int bytecodeOffset = 0;

        for (int instructionId = 0; instructionId < node.instructions.size(); instructionId++) {
            AbstractInsnNode instructionNode = node.instructions.get(instructionId);

            BytecodeInstruction instruction = new BytecodeInstruction(
                    classLoader,
                    className,
                    methodName,
                    instructionId,
                    bytecodeOffset,
                    instructionNode
            );

            if (instruction.isLineNumber())
                lastLineNumber = instruction.getLineNumber();
            else if (lastLineNumber != -1)
                instruction.setLineNumber(lastLineNumber);

            bytecodeOffset += getBytecodeIncrement(instructionNode);

            if (!instruction.isLabel() && !instruction.isLineNumber()
                    && !(instruction.getASMNode() instanceof FrameNode)) {
                bytecodeOffset++;
            }

            registerInstruction(instruction);

        }

        List<BytecodeInstruction> r = getInstructionsIn(className, methodName);
        if (r == null || r.size() == 0)
            throw new IllegalStateException(
                    "expect instruction pool to return non-null non-empty list of instructions for a previously registered method "
                            + methodName);

        return r;
    }

    /**
     * Determine how many bytes the current instruction occupies together with
     * its operands
     *
     * @return
     */
    private int getBytecodeIncrement(AbstractInsnNode instructionNode) {
        int opcode = instructionNode.getOpcode();
        switch (opcode) {
            case Opcodes.ALOAD: // index
            case Opcodes.ASTORE: // index
            case Opcodes.DLOAD:
            case Opcodes.DSTORE:
            case Opcodes.FLOAD:
            case Opcodes.FSTORE:
            case Opcodes.ILOAD:
            case Opcodes.ISTORE:
            case Opcodes.LLOAD:
            case Opcodes.LSTORE:
                VarInsnNode varNode = (VarInsnNode) instructionNode;
                if (varNode.var > 3)
                    return 1;
                else
                    return 0;
            case Opcodes.BIPUSH: // byte
            case Opcodes.NEWARRAY:
            case Opcodes.RET:
                return 1;
            case Opcodes.LDC:
                LdcInsnNode ldcNode = (LdcInsnNode) instructionNode;
                if (ldcNode.cst instanceof Double || ldcNode.cst instanceof Long)
                    return 2; // LDC2_W
                else
                    return 1;
            case 19: //LDC_W
            case 20: //LDC2_W
                return 2;
            case Opcodes.ANEWARRAY: // indexbyte1, indexbyte2
            case Opcodes.CHECKCAST: // indexbyte1, indexbyte2
            case Opcodes.GETFIELD:
            case Opcodes.GETSTATIC:
            case Opcodes.GOTO:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IFLE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFNE:
            case Opcodes.IFEQ:
            case Opcodes.IFNONNULL:
            case Opcodes.IFNULL:
            case Opcodes.IINC:
            case Opcodes.INSTANCEOF:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.JSR:
            case Opcodes.NEW:
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
            case Opcodes.SIPUSH:
                // case Opcodes.LDC_W
                // case Opcodes.LDC2_W

                return 2;
            case Opcodes.MULTIANEWARRAY:
                return 3;
            case Opcodes.INVOKEDYNAMIC:
            case Opcodes.INVOKEINTERFACE:
                return 4;

            case Opcodes.LOOKUPSWITCH:
            case Opcodes.TABLESWITCH:
                // TODO: Could be more
                return 4;
            // case Opcodes.GOTO_W
            // case Opcodes.JSR_W
        }
        return 0;
    }

    /**
     * <p>
     * registerInstruction
     * </p>
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public void registerInstruction(BytecodeInstruction instruction) {
        String className = instruction.getClassName();
        String methodName = instruction.getMethodName();

        if (!instructionMap.containsKey(className))
            instructionMap.put(className,
                    new LinkedHashMap<>());
        if (!instructionMap.get(className).containsKey(methodName))
            instructionMap.get(className).put(methodName,
                    new ArrayList<>());

        instructionMap.get(className).get(methodName).add(instruction);
        SimpleLogger.debug("Registering instruction " + instruction);
        List<BytecodeInstruction> instructions = instructionMap.get(className).get(methodName);
        if (instructions.size() > 1) {
            BytecodeInstruction previous = instructions.get(instructions.size() - 2);
            if (previous.isLabel()) {
                LabelNode ln = (LabelNode) previous.asmNode;
                if (ln.getLabel() instanceof AnnotatedLabel) {
                    AnnotatedLabel aLabel = (AnnotatedLabel) ln.getLabel();
                    if (aLabel.isStartTag()) {
                        if (aLabel.shouldIgnore()) {
                            SimpleLogger.debug("Ignoring artificial branch: " + instruction);
                            return;
                        }
                    }
                }
            }
        }

        if (instruction.isBranch()) {
            BranchPool.getInstance(classLoader).registerAsBranch(instruction);
        }
    }

    // retrieve data from the pool

    /**
     * <p>
     * getInstruction
     * </p>
     *
     * @param className     a {@link java.lang.String} object.
     * @param methodName    a {@link java.lang.String} object.
     * @param instructionId a int.
     * @param asmNode       a {@link org.objectweb.asm.tree.AbstractInsnNode} object.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getInstruction(String className, String methodName,
                                              int instructionId, AbstractInsnNode asmNode) {

        BytecodeInstruction r = getInstruction(className, methodName, instructionId);

        assert r == null || (asmNode != null && asmNode.equals(r.getASMNode()));

        return r;
    }

    /**
     * <p>
     * getInstruction
     * </p>
     *
     * @param className     a {@link java.lang.String} object.
     * @param methodName    a {@link java.lang.String} object.
     * @param instructionId a int.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getInstruction(String className, String methodName,
                                              int instructionId) {

        if (instructionMap.get(className) == null) {
            SimpleLogger.debug("unknown class: " + className);
            SimpleLogger.debug(instructionMap.keySet().toString());
            return null;
        }
        if (instructionMap.get(className).get(methodName) == null) {
            SimpleLogger.debug("unknown method: " + methodName);
            SimpleLogger.debug(instructionMap.get(className).keySet().toString());
            return null;
        }
        for (BytecodeInstruction instruction : instructionMap.get(className).get(methodName)) {
            if (instruction.getInstructionId() == instructionId)
                return instruction;
        }

        SimpleLogger.debug("unknown instruction " + instructionId + ", have "
                + instructionMap.get(className).get(methodName).size());
        for (int i = 0; i < instructionMap.get(className).get(methodName).size(); i++) {
            SimpleLogger.info(instructionMap.get(className).get(methodName).get(i).toString());
        }

        return null;
    }

    /**
     * <p>
     * getInstruction
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @param node       a {@link org.objectweb.asm.tree.AbstractInsnNode} object.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getInstruction(String className, String methodName,
                                              AbstractInsnNode node) {

        if (instructionMap.get(className) == null) {
            SimpleLogger.debug("unknown class: " + className);
            SimpleLogger.debug(instructionMap.keySet().toString());
            return null;
        }
        if (instructionMap.get(className).get(methodName) == null) {
            SimpleLogger.debug("unknown method: " + methodName);
            SimpleLogger.debug(instructionMap.get(className).keySet().toString());
            return null;
        }
        for (BytecodeInstruction instruction : instructionMap.get(className).get(methodName)) {
            if (instruction.asmNode == node)
                return instruction;
        }

        SimpleLogger.debug("unknown instruction: " + node + ", have "
                + instructionMap.get(className).get(methodName).size()
                + " instructions for this method");
        SimpleLogger.debug(instructionMap.get(className).get(methodName).toString());

        return null;
    }

    /**
     * <p>
     * getInstructionsIn
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<BytecodeInstruction> getInstructionsIn(String className, String methodName) {
        if (instructionMap.get(className) == null
                || instructionMap.get(className).get(methodName) == null)
            return null;

        List<BytecodeInstruction> r = new ArrayList<>(instructionMap.get(className).get(methodName));

        return r;
    }

    public List<BytecodeInstruction> getInstructionsIn(String className) {
        if (instructionMap.get(className) == null)
            return null;

        List<BytecodeInstruction> r = new ArrayList<>();
        Map<String, List<BytecodeInstruction>> methodMap = instructionMap.get(className);
        for (List<BytecodeInstruction> methodInstructions : methodMap.values()) {
            r.addAll(methodInstructions);
        }

        return r;
    }

    /**
     * <p>
     * forgetInstruction
     * </p>
     *
     * @param ins a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    public boolean forgetInstruction(BytecodeInstruction ins) {
        if (!instructionMap.containsKey(ins.getClassName()))
            return false;
        if (!instructionMap.get(ins.getClassName()).containsKey(ins.getMethodName()))
            return false;

        return instructionMap.get(ins.getClassName()).get(ins.getMethodName()).remove(ins);
    }

}

