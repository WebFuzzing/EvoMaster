package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Parsed representation of a Branch objective descriptive id produced by ObjectiveNaming.branchObjectiveName.
 * Fields mirror what is encoded in the id.
 */
public class BranchTargetDescriptor implements Serializable {

    private static final long serialVersionUID = 43L;

    public final String classNameDots;
    public final int line;
    public final int positionInLine;
    public final boolean thenBranch;
    public final int opcode;

    public BranchTargetDescriptor(String classNameDots, int line, int positionInLine, boolean thenBranch, int opcode) {
        this.classNameDots = Objects.requireNonNull(classNameDots);
        this.line = line;
        this.positionInLine = positionInLine;
        this.thenBranch = thenBranch;
        this.opcode = opcode;
        if (line <= 0 || positionInLine < 0) {
            throw new IllegalArgumentException("Invalid line/position for branch target");
        }
    }
}


