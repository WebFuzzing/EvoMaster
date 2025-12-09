/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction;

import java.io.Serializable;

/**
 * An object of this class corresponds to a Branch inside the class under test.
 *
 * <p>
 * Branches are created by the {@code CFGMethodVisitor} via the {@code BranchPool}. Each Branch
 * holds its corresponding {@code BytecodeInstruction} from the {@code RawControlFlowGraph} and
 * is associated with a unique {@code actualBranchId}.
 *
 * <p>
 * A Branch can either come from a jump instruction, as defined in
 * {@code BytecodeInstruction.isBranch()}.
 * Only {@code BytecodeInstructions} satisfying {@code BytecodeInstruction.isActualbranch()} are
 * expected to be associated with a {@code Branch} object.
 *
 */
public class Branch implements Serializable {

    private static final long serialVersionUID = -4732587925060748263L;

    private final int actualBranchId;

    private final BytecodeInstruction instruction;

    /**
     * Canonical identifiers matching the descriptive ids used in {@code ObjectiveNaming}.
     */
    private String thenObjectiveId;
    private String elseObjectiveId;

    /**
     * Constructor for usual jump instruction Branches.
     *
     * @param branchInstruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @param actualBranchId    a int.
     */
    public Branch(BytecodeInstruction branchInstruction, int actualBranchId) {
        if (!branchInstruction.isBranch())
            throw new IllegalArgumentException("only branch instructions are accepted");

        this.instruction = branchInstruction;
        this.actualBranchId = actualBranchId;

        if (this.actualBranchId < 1)
            throw new IllegalStateException(
                    "expect branch to have actualBranchId set to positive value");
    }

    /**
     * <p>
     * Getter for the field <code>actualBranchId</code>.
     * </p>
     *
     * @return a int.
     */
    public int getActualBranchId() {
        return actualBranchId;
    }

    /**
     * <p>
     * Getter for the field <code>instruction</code>.
     * </p>
     *
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public BytecodeInstruction getInstruction() {
        return instruction;
    }

    /**
     * <p>
     * getClassName
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getClassName() {
        return instruction.getClassName();
    }

    /**
     * <p>
     * getMethodName
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMethodName() {
        return instruction.getMethodName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + actualBranchId;
        result = prime * result + ((instruction == null) ? 0 : instruction.hashCode());
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
        Branch other = (Branch) obj;
        if (actualBranchId != other.actualBranchId)
            return false;
        if (instruction == null) {
            if (other.instruction != null)
                return false;
        } else if (!instruction.equals(other.instruction))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String r = "I" + instruction.getInstructionId();
        r += " Branch " + getActualBranchId();
        r += " " + instruction.getInstructionType();
            r += " L" + instruction.getLineNumber();

        if (thenObjectiveId != null || elseObjectiveId != null) {
             r += " [" + (thenObjectiveId == null ? "null" : thenObjectiveId) +
                  ", " + (elseObjectiveId == null ? "null" : elseObjectiveId) + "]";
        }

        return r;
    }

    /**
     * Store the descriptive identifiers associated with this branch.
     *
     * @param thenId descriptive id for the "true" outcome
     * @param elseId descriptive id for the "false" outcome
     */
    public void setObjectiveIds(String thenId, String elseId) {
        this.thenObjectiveId = thenId;
        this.elseObjectiveId = elseId;
    }

    public String getThenObjectiveId() {
        return thenObjectiveId;
    }

    public String getElseObjectiveId() {
        return elseObjectiveId;
    }

}
