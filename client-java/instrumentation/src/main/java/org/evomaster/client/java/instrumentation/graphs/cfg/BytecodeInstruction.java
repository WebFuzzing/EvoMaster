/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster.
 */
package org.evomaster.client.java.instrumentation.graphs.cfg;

import org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch;
import org.evomaster.client.java.instrumentation.graphs.cfg.branch.BranchPool;

import org.evomaster.client.java.instrumentation.graphs.GraphPool;
import org.evomaster.client.java.instrumentation.graphs.cdg.ControlDependenceGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.io.Serializable;
import java.util.Set;

/**
 * Internal representation of a BytecodeInstruction
 */
public class BytecodeInstruction implements Serializable,
        Comparable<BytecodeInstruction> {

    private static final long serialVersionUID = 3630449183355518857L;

    // from ASM library
    protected AbstractInsnNode asmNode;

    // identification of a byteCode instruction inside EvoSuite
    protected ClassLoader classLoader;
    protected String className;
    protected String methodName;
    protected int instructionId;
    protected int bytecodeOffset;

    // auxiliary information
    private int lineNumber = -1;

    // experiment: also searching through all CFG nodes in order to determine an
    // instruction BasicBlock might be a little to expensive too just to safe
    // space for one reference
    private BasicBlock basicBlock;

    /**
     * Generates a ByteCodeInstruction instance that represents a byteCode
     * instruction as indicated by the given ASMNode in the given method and
     * class
     *
     * @param className      a {@link java.lang.String} object.
     * @param methodName     a {@link java.lang.String} object.
     * @param instructionId  a int.
     * @param bytecodeOffset a int.
     * @param asmNode        a {@link org.objectweb.asm.tree.AbstractInsnNode} object.
     */
    public BytecodeInstruction(ClassLoader classLoader, String className,
                               String methodName, int instructionId, int bytecodeOffset, AbstractInsnNode asmNode) {

        if (className == null || methodName == null || asmNode == null)
            throw new IllegalArgumentException("null given");
        if (instructionId < 0)
            throw new IllegalArgumentException(
                    "expect instructionId to be positive, not " + instructionId);

        this.instructionId = instructionId;
        this.bytecodeOffset = bytecodeOffset;
        this.asmNode = asmNode;

        this.classLoader = classLoader;

        setClassName(className);
        setMethodName(methodName);
    }

    /**
     * Can represent any byteCode instruction
     *
     * @param wrap a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction(BytecodeInstruction wrap) {

        this(wrap.classLoader, wrap.className, wrap.methodName, wrap.instructionId,
                wrap.bytecodeOffset, wrap.asmNode, wrap.lineNumber, wrap.basicBlock);
    }

    /**
     * <p>
     * Constructor for BytecodeInstruction.
     * </p>
     *
     * @param className      a {@link java.lang.String} object.
     * @param methodName     a {@link java.lang.String} object.
     * @param instructionId  a int.
     * @param bytecodeOffset a int.
     * @param asmNode        a {@link org.objectweb.asm.tree.AbstractInsnNode} object.
     * @param lineNumber     a int.
     * @param basicBlock     a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BasicBlock} object.
     */
    public BytecodeInstruction(ClassLoader classLoader, String className,
                               String methodName, int instructionId, int bytecodeOffset, AbstractInsnNode asmNode,
                               int lineNumber, BasicBlock basicBlock) {

        this(classLoader, className, methodName, instructionId, bytecodeOffset, asmNode,
                lineNumber);

        this.basicBlock = basicBlock;
    }

    /**
     * <p>
     * Constructor for BytecodeInstruction.
     * </p>
     *
     * @param className      a {@link java.lang.String} object.
     * @param methodName     a {@link java.lang.String} object.
     * @param instructionId  a int.
     * @param bytecodeOffset a int.
     * @param asmNode        a {@link org.objectweb.asm.tree.AbstractInsnNode} object.
     * @param lineNumber     a int.
     */
    public BytecodeInstruction(ClassLoader classLoader, String className,
                               String methodName, int instructionId, int bytecodeOffset, AbstractInsnNode asmNode,
                               int lineNumber) {

        this(classLoader, className, methodName, instructionId, bytecodeOffset, asmNode);

        if (lineNumber != -1)
            setLineNumber(lineNumber);
    }

    // getter + setter

    private void setMethodName(String methodName) {
        if (methodName == null)
            throw new IllegalArgumentException("null given");

        this.methodName = methodName;
    }

    private void setClassName(String className) {
        if (className == null)
            throw new IllegalArgumentException("null given");

        this.className = className;
    }

    // --- Field Management ---

    /**
     * {@inheritDoc}
     */
    public int getInstructionId() {
        return instructionId;
    }

    /**
     * <p>
     * getBytecodeOffset
     * </p>
     *
     * @return a int.
     */
    public int getBytecodeOffset() {
        return bytecodeOffset;
    }

    /**
     * {@inheritDoc}
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * <p>
     * Getter for the field <code>className</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Return's the BasicBlock that contain's this instruction in it's CFG.
     * <p>
     * If no BasicBlock containing this instruction was created yet, null is
     * returned.
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BasicBlock} object.
     */
    public BasicBlock getBasicBlock() {
        if (!hasBasicBlockSet())
            retrieveBasicBlock();
        return basicBlock;
    }

    private void retrieveBasicBlock() {

        if (basicBlock == null)
            basicBlock = getActualCFG().getBlockOf(this);
    }

    /**
     * Once the CFG has been asked for this instruction's BasicBlock it sets
     * this instance's internal basicBlock field.
     *
     * @param block a {@link org.evomaster.client.java.instrumentation.graphs.cfg.BasicBlock} object.
     */
    public void setBasicBlock(BasicBlock block) {
        if (block == null)
            throw new IllegalArgumentException("null given");

        if (!block.getClassName().equals(getClassName())
                || !block.getMethodName().equals(getMethodName()))
            throw new IllegalArgumentException(
                    "expect block to be for the same method and class as this instruction");
        if (this.basicBlock != null)
            throw new IllegalArgumentException(
                    "basicBlock already set! not allowed to overwrite");

        this.basicBlock = block;
    }

    /**
     * Checks whether this instance's basicBlock has already been set by the CFG
     * or
     *
     * @return a boolean.
     */
    public boolean hasBasicBlockSet() {
        return basicBlock != null;
    }

    /**
     * {@inheritDoc}
     */
    public int getLineNumber() {

        if (lineNumber == -1 && isLineNumber())
            retrieveLineNumber();

        return lineNumber;
    }

    /**
     * <p>
     * Setter for the field <code>lineNumber</code>.
     * </p>
     *
     * @param lineNumber a int.
     */
    public void setLineNumber(int lineNumber) {
        if (lineNumber <= 0)
            throw new IllegalArgumentException(
                    "expect lineNumber value to be positive");

        if (isLabel())
            return;

        if (isLineNumber()) {
            int asmLine = getASMLineNumber();
            // sanity check
            if (lineNumber != -1 && asmLine != lineNumber)
                throw new IllegalStateException(
                        "linenumber instruction has lineNumber field set to a value different from instruction linenumber");
            this.lineNumber = asmLine;
        } else {
            this.lineNumber = lineNumber;
        }
    }

    /**
     * At first, if this instruction constitutes a line number instruction this
     * method tries to retrieve the lineNumber from the underlying asmNode and
     * set the lineNumber field to the value given by the asmNode.
     * <p>
     * This can lead to an IllegalStateException, should the lineNumber field
     * have been set to another value previously
     * <p>
     * After that, if the lineNumber field is still not initialized, this method
     * returns false Otherwise it returns true
     *
     * @return a boolean.
     */
    public boolean hasLineNumberSet() {
        retrieveLineNumber();
        return lineNumber != -1;
    }

    /**
     * If the underlying ASMNode is a LineNumberNode the lineNumber field of
     * this instance will be set to the lineNumber contained in that
     * LineNumberNode
     * <p>
     * Should the lineNumber field have been set to a value different from that
     * contained in the asmNode, this method throws an IllegalStateExeption
     */
    private void retrieveLineNumber() {
        if (isLineNumber()) {
            int asmLine = getASMLineNumber();
            // sanity check
            if (this.lineNumber != -1 && asmLine != this.lineNumber)
                throw new IllegalStateException(
                        "lineNumber field was manually set to a value different from the actual lineNumber contained in LineNumberNode");
            this.lineNumber = asmLine;
        }
    }

    // --- graph section ---

    /**
     * Returns the ActualControlFlowGraph of this instructions method
     * <p>
     * Convenience method. Redirects the call to GraphPool.getActualCFG()
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.ActualControlFlowGraph} object.
     */
    public ActualControlFlowGraph getActualCFG() {

        ActualControlFlowGraph myCFG = GraphPool.getInstance(classLoader).getActualCFG(className,
                methodName);
        if (myCFG == null)
            throw new IllegalStateException(
                    "expect GraphPool to know CFG for every method for which an instruction is known");

        return myCFG;
    }

    /**
     * Returns the RawControlFlowGraph of this instructions method
     * <p>
     * Convenience method. Redirects the call to GraphPool.getRawCFG()
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.RawControlFlowGraph} object.
     */
    public RawControlFlowGraph getRawCFG() {

        RawControlFlowGraph myCFG = GraphPool.getInstance(classLoader).getRawCFG(className,
                methodName);
        if (myCFG == null)
            throw new IllegalStateException(
                    "expect GraphPool to know CFG for every method for which an instruction is known");

        return myCFG;
    }

    /**
     * Returns the ControlDependenceGraph of this instructions method
     * <p>
     * Convenience method. Redirects the call to GraphPool.getCDG()
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cdg.ControlDependenceGraph} object.
     */
    public ControlDependenceGraph getCDG() {

        ControlDependenceGraph myCDG = GraphPool.getInstance(classLoader).getCDG(className,
                methodName);
        if (myCDG == null)
            throw new IllegalStateException(
                    "expect GraphPool to know CDG for every method for which an instruction is known");

        return myCDG;
    }

    // --- CDG-Section ---

    /**
     * Returns a cfg.Branch object for each branch this instruction is control
     * dependent on as determined by the ControlDependenceGraph. If this
     * instruction is only dependent on the root branch this method returns an
     * empty set
     * <p>
     * If this instruction is a Branch and it is dependent on itself - which can
     * happen in loops for example - the returned set WILL contain this. If you
     * do not need the full set in order to avoid loops, call
     * getAllControlDependentBranches instead
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<ControlDependency> getControlDependencies() {

        BasicBlock myBlock = getBasicBlock();

        // return new
        // HashSet<ControlDependency>(myBlock.getControlDependencies());
        return myBlock.getControlDependencies();
    }

        public Branch getControlDependentBranch() {

        Set<ControlDependency> controlDependentBranches = getControlDependencies();

        for (ControlDependency cd : controlDependentBranches)
            return cd.getBranch();

        return null; // root branch
    }

    // String methods

    /**
     * <p>
     * explain
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String explain() {
        if (isBranch()) {
            if (BranchPool.getInstance(classLoader).isKnownAsBranch(this)) {
                Branch b = BranchPool.getInstance(classLoader).getBranchForInstruction(this);
                if (b == null)
                    throw new IllegalStateException(
                            "expect BranchPool to be able to return Branches for instructions fullfilling BranchPool.isKnownAsBranch()");

                return "Branch " + b.getActualBranchId() + " - "
                        + getInstructionType();
            }
            return "UNKNOWN Branch I" + instructionId + " "
                    + getInstructionType() + ", jump to " + ((JumpInsnNode) asmNode).label.getLabel();

            // + " - " + ((JumpInsnNode) asmNode).label.getLabel();
        }

        return getASMNodeString();
    }

    /**
     * <p>
     * getASMNodeString
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getASMNodeString() {
        String type = getType();
        String opcode = getInstructionType();

        String stack = "n/a";

        if (asmNode instanceof LabelNode) {
            return "LABEL " + ((LabelNode) asmNode).getLabel().toString();
        } else if (asmNode instanceof FieldInsnNode)
            return "Field" + " " + ((FieldInsnNode) asmNode).owner + "."
                    + ((FieldInsnNode) asmNode).name + " Type=" + type
                    + ", Opcode=" + opcode;
        else if (asmNode instanceof FrameNode)
            return "Frame" + " " + asmNode.getOpcode() + " Type=" + type
                    + ", Opcode=" + opcode;
        else if (asmNode instanceof IincInsnNode)
            return "IINC " + ((IincInsnNode) asmNode).var + " Type=" + type
                    + ", Opcode=" + opcode;
        else if (asmNode instanceof InsnNode)
            return "" + opcode;
        else if (asmNode instanceof IntInsnNode)
            return "INT " + ((IntInsnNode) asmNode).operand + " Type=" + type
                    + ", Opcode=" + opcode;
        else if (asmNode instanceof MethodInsnNode)
            return opcode + " " + ((MethodInsnNode) asmNode).owner + "." + ((MethodInsnNode) asmNode).name + ((MethodInsnNode) asmNode).desc;
        else if (asmNode instanceof JumpInsnNode)
            return "JUMP " + ((JumpInsnNode) asmNode).label.getLabel()
                    + " Type=" + type + ", Opcode=" + opcode + ", Stack: "
                    + stack + " - Line: " + lineNumber;
        else if (asmNode instanceof LdcInsnNode)
            return "LDC " + ((LdcInsnNode) asmNode).cst + " Type=" + type; // +
            // ", Opcode=";
            // + opcode; // cst starts with mutationid if
            // this is location of mutation
        else if (asmNode instanceof LineNumberNode)
            return "LINE " + " " + ((LineNumberNode) asmNode).line;
        else if (asmNode instanceof LookupSwitchInsnNode)
            return "LookupSwitchInsnNode" + " " + asmNode.getOpcode()
                    + " Type=" + type + ", Opcode=" + opcode;
        else if (asmNode instanceof MultiANewArrayInsnNode)
            return "MULTIANEWARRAY " + " " + asmNode.getOpcode() + " Type="
                    + type + ", Opcode=" + opcode;
        else if (asmNode instanceof TableSwitchInsnNode)
            return "TableSwitchInsnNode" + " " + asmNode.getOpcode() + " Type="
                    + type + ", Opcode=" + opcode;
        else if (asmNode instanceof TypeInsnNode) {
            switch (asmNode.getOpcode()) {
                case Opcodes.NEW:
                    return "NEW " + ((TypeInsnNode) asmNode).desc;
                case Opcodes.ANEWARRAY:
                    return "ANEWARRAY " + ((TypeInsnNode) asmNode).desc;
                case Opcodes.CHECKCAST:
                    return "CHECKCAST " + ((TypeInsnNode) asmNode).desc;
                case Opcodes.INSTANCEOF:
                    return "INSTANCEOF " + ((TypeInsnNode) asmNode).desc;
                default:
                    return "Unknown node" + " Type=" + type + ", Opcode=" + opcode;
            }
        }
        // return "TYPE " + " " + node.getOpcode() + " Type=" + type
        // + ", Opcode=" + opcode;
        else if (asmNode instanceof VarInsnNode)
            return opcode + " " + ((VarInsnNode) asmNode).var;
        else
            return "Unknown node" + " Type=" + type + ", Opcode=" + opcode;
    }

    // --- Inherited from Object ---

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        String r = "I" + instructionId;

        r += " (" + +bytecodeOffset + ")";
        r += " " + explain();

        if (hasLineNumberSet() && !isLineNumber())
            r += " l" + getLineNumber();

        return r;
    }

    /**
     * Convenience method:
     * <p>
     * If this instruction is known by the BranchPool to be a Branch, you can
     * call this method in order to retrieve the corresponding Branch object
     * registered within the BranchPool.
     * <p>
     * Otherwise this method will return null;
     *
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch} object.
     */
    public Branch toBranch() {

        try {
            return BranchPool.getInstance(classLoader).getBranchForInstruction(this);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * isLastInstructionInMethod
     * </p>
     *
     * @return a boolean.
     */
    public boolean isLastInstructionInMethod() {
        return equals(getRawCFG().getInstructionWithBiggestId());
    }

    /**
     * <p>
     * canBeExitPoint
     * </p>
     *
     * @return a boolean.
     */
    public boolean canBeExitPoint() {
        return canReturnFromMethod() || isLastInstructionInMethod();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((className == null) ? 0 : className.hashCode());
        result = prime * result + instructionId;
        result = prime * result
                + ((methodName == null) ? 0 : methodName.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BytecodeInstruction other = (BytecodeInstruction) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (instructionId != other.instructionId)
            return false;
        if (methodName == null) {
            return other.methodName == null;
        } else return methodName.equals(other.methodName);
    }

    // inherited from Object

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(BytecodeInstruction o) {
        return getLineNumber() - o.getLineNumber();
    }

    /**
     * <p>
     * getASMNode
     * </p>
     *
     * @return a {@link org.objectweb.asm.tree.AbstractInsnNode} object.
     */
    public AbstractInsnNode getASMNode() {
        return asmNode;
    }

    /**
     * <p>
     * getInstructionType
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getInstructionType() {

        if (asmNode.getOpcode() >= 0 && asmNode.getOpcode() < Printer.OPCODES.length)
            return Printer.OPCODES[asmNode.getOpcode()];

        if (isLineNumber())
            return "LINE " + this.getLineNumber();

        return getType();
    }

    /**
     * <p>
     * getType
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getType() {
        // TODO explain
        String type = "";
        if (asmNode.getType() >= 0 && asmNode.getType() < Printer.TYPES.length)
            type = Printer.TYPES[asmNode.getType()];

        return type;
    }

    /**
     * <p>
     * canReturnFromMethod
     * </p>
     *
     * @return a boolean.
     */
    public boolean canReturnFromMethod() {
        return isReturn() || isThrow();
    }

    /**
     * <p>
     * isReturn
     * </p>
     *
     * @return a boolean.
     */
    public boolean isReturn() {
        switch (asmNode.getOpcode()) {
            case Opcodes.RETURN:
            case Opcodes.ARETURN:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
            case Opcodes.FRETURN:
                return true;
            default:
                return false;
        }
    }

    /**
     * <p>
     * isThrow
     * </p>
     *
     * @return a boolean.
     */
    public boolean isThrow() {
        // TODO: Need to check if this is a caught exception?
        return asmNode.getOpcode() == Opcodes.ATHROW;
    }

    /**
     * <p>
     * isJump
     * </p>
     *
     * @return a boolean.
     */
    public boolean isJump() {
        return (asmNode instanceof JumpInsnNode);
    }

    /**
     * <p>
     * isGoto
     * </p>
     *
     * @return a boolean.
     */
    public boolean isGoto() {
        if (asmNode instanceof JumpInsnNode) {
            return (asmNode.getOpcode() == Opcodes.GOTO);
        }
        return false;
    }

    /**
     * <p>
     * isBranch
     * </p>
     *
     * @return a boolean.
     */
    public boolean isBranch() {
        return (isJump() && !isGoto());
    }

    /**
     * Determines if this instruction is a line number instruction
     * <p>
     * More precisely this method checks if the underlying asmNode is a
     * LineNumberNode
     *
     * @return a boolean.
     */
    public boolean isLineNumber() {
        return (asmNode instanceof LineNumberNode);
    }

    /**
     * <p>
     * getASMLineNumber
     * </p>
     *
     * @return a int.
     */
    public int getASMLineNumber() {
        if (!isLineNumber())
            return -1;

        return ((LineNumberNode) asmNode).line;
    }

    /**
     * <p>
     * isLabel
     * </p>
     *
     * @return a boolean.
     */
    public boolean isLabel() {
        return asmNode instanceof LabelNode;
    }

}
