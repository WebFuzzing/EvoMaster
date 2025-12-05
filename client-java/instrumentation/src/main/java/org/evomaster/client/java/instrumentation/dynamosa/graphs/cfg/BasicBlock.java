/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.GraphPool;
import org.evomaster.client.java.instrumentation.dynamosa.graphs.cdg.ControlDependenceGraph;

import java.io.Serializable;
import java.util.*;

/**
 * This class is used to represent basic blocks in the control flow graph.
 * <p>
 * A basic block is a list of instructions for which the following holds:
 * <p>
 * Whenever control flow reaches the first instruction of this blocks list,
 * control flow will pass through all the instructions of this list successively
 * and not pass another instruction of the underlying method in the mean time.
 * The first element in this blocks list does not have a parent in the CFG that
 * can be prepended to the list and the same would still hold true Finally the
 * last element in this list does not have a child inside the CFG that could be
 * appended to the list such that the above still holds true
 * <p>
 * In other words: - the first/last element of this blocks list has either 0 or
 * >=2 parents/children in the CFG - every other element in the list has exactly
 * 1 parent and exactly 1 child in the raw CFG
 * <p>
 * <p>
 * Taken from:
 * <p>
 * "Efficiently Computing Static Single Assignment Form and the Control
 * Dependence Graph" RON CYTRON, JEANNE FERRANTE, BARRY K. ROSEN, and MARK N.
 * WEGMAN IBM Research Division and F. KENNETH ZADECK Brown University 1991
 *
 * @see org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ActualControlFlowGraph
 */
public class BasicBlock implements Serializable, Iterable<BytecodeInstruction> {

    private static final long serialVersionUID = -3465486470017841484L;

    private static int blockCount = 0;

    private int id = -1;
    protected ClassLoader classLoader;
    protected String className;
    protected String methodName;

    private Set<ControlDependency> controlDependencies;

    protected boolean isAuxiliaryBlock = false;

    private final List<BytecodeInstruction> instructions = new ArrayList<>();

    /**
     * <p>
     * Constructor for BasicBlock.
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @param blockNodes a {@link java.util.List} object.
     */
    public BasicBlock(ClassLoader classLoader, String className, String methodName,
                      List<BytecodeInstruction> blockNodes) {
        if (className == null || methodName == null || blockNodes == null)
            throw new IllegalArgumentException("null given");

        this.className = className;
        this.methodName = methodName;
        this.classLoader = classLoader;

        setId();
        setInstructions(blockNodes);
    }

    /**
     * Used by Entry- and ExitBlocks
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     */
    protected BasicBlock(String className, String methodName) {
        if (className == null || methodName == null)
            throw new IllegalArgumentException("null given");

        this.className = className;
        this.methodName = methodName;
        this.isAuxiliaryBlock = true;
    }

    // CDs

    /**
     * Returns the ControlDependenceGraph of this instructions method
     * <p>
     * Convenience method. Redirects the call to GraphPool.getCDG()
     *
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cdg.ControlDependenceGraph} object.
     */
    public ControlDependenceGraph getCDG() {

        ControlDependenceGraph myCDG = GraphPool.getInstance(classLoader).getCDG(className,
                methodName);
        if (myCDG == null)
            throw new IllegalStateException(
                    "expect GraphPool to know CDG for every method for which an instruction is known");

        return myCDG;
    }

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

        if (controlDependencies == null)
            controlDependencies = getCDG().getControlDependentBranches(this);

        //		return new HashSet<ControlDependency>(controlDependentBranches);
        return controlDependencies;
    }

    /**
     * <p>
     * hasControlDependenciesSet
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasControlDependenciesSet() {
        return controlDependencies != null;
    }

    // initialization

    private void setInstructions(List<BytecodeInstruction> blockNodes) {
        for (BytecodeInstruction instruction : blockNodes) {
            if (!appendInstruction(instruction))
                throw new IllegalStateException(
                        "internal error while addind instruction to basic block list");
        }
        if (instructions.isEmpty())
            throw new IllegalStateException(
                    "expect each basic block to contain at least one instruction");
    }

    private boolean appendInstruction(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");
        if (!className.equals(instruction.getClassName()))
            throw new IllegalArgumentException(
                    "expect elements of a basic block to be inside the same class");
        if (!methodName.equals(instruction.getMethodName()))
            throw new IllegalArgumentException(
                    "expect elements of a basic block to be inside the same class");
        if (instruction.hasBasicBlockSet())
            throw new IllegalArgumentException(
                    "expect to get instruction without BasicBlock already set");
        if (instructions.contains(instruction))
            throw new IllegalArgumentException(
                    "a basic block can not contain the same element twice");

        instruction.setBasicBlock(this);

        return instructions.add(instruction);
    }

    private void setId() {
        blockCount++;
        this.id = blockCount;
    }

    // retrieve information

    /**
     * <p>
     * containsInstruction
     * </p>
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    public boolean containsInstruction(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");

        return instructions.contains(instruction);
    }

    /**
     * <p>
     * getFirstInstruction
     * </p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getFirstInstruction() {
        if (instructions.isEmpty())
            return null;
        return instructions.get(0);
    }

    /**
     * <p>
     * getLastInstruction
     * </p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getLastInstruction() {
        if (instructions.isEmpty())
            return null;
        return instructions.get(instructions.size() - 1);
    }

    /**
     * <p>
     * getFirstLine
     * </p>
     *
     * @return a int.
     */
    public int getFirstLine() {
        for (BytecodeInstruction ins : instructions)
            if (ins.hasLineNumberSet())
                return ins.getLineNumber();

        return -1;
    }

    /**
     * <p>
     * getLastLine
     * </p>
     *
     * @return a int.
     */
    public int getLastLine() {

        int r = -1;

        for (BytecodeInstruction ins : instructions)
            if (ins.hasLineNumberSet())
                r = ins.getLineNumber();

        return r;
    }

    /**
     * <p>
     * getName
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return (isAuxiliaryBlock ? "aux" : "") + "BasicBlock " + id;
        // +" - "+methodName;
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
     * <p>
     * Getter for the field <code>methodName</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMethodName() {
        return methodName;
    }

    // inherited from Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        String r = "BB" + id;

        if (instructions.size() < 5)
            for (BytecodeInstruction ins : instructions)
                r = r.trim() + " " + ins.getInstructionType();
        else
            r += " " + getFirstInstruction().getInstructionType() + " ... "
                    + getLastInstruction().getInstructionType();

        int startLine = getFirstLine();
        int endLine = getLastLine();
        r += " l" + (startLine == -1 ? "?" : startLine + "");
        r += "-l" + (endLine == -1 ? "?" : endLine + "");

        return r;
    }


    // sanity check

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((className == null) ? 0 : className.hashCode());
        result = prime * result + id;
        result = prime * result
                + ((instructions == null) ? 0 : instructions.hashCode());
        result = prime * result + (isAuxiliaryBlock ? 1231 : 1237);
        result = prime * result
                + ((methodName == null) ? 0 : methodName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof BasicBlock))
            return false;
        BasicBlock other = (BasicBlock) obj;
        if (id != other.id)
            return false;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (methodName == null) {
            if (other.methodName != null)
                return false;
        } else if (!methodName.equals(other.methodName))
            return false;
        if (instructions == null) {
            if (other.instructions != null)
                return false;
        } else if (!instructions.equals(other.instructions))
            return false;
        if (isEntryBlock() != other.isEntryBlock())
            return false;
        return isExitBlock() == other.isExitBlock();
    }

    /**
     * <p>
     * isEntryBlock
     * </p>
     *
     * @return a boolean.
     */
    public boolean isEntryBlock() {
        return false;
    }

    /**
     * <p>
     * isExitBlock
     * </p>
     *
     * @return a boolean.
     */
    public boolean isExitBlock() {
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<BytecodeInstruction> iterator() {
        return instructions.iterator();
    }
}
