/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch;

import com.google.common.annotations.VisibleForTesting;
import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is supposed to hold all the available information concerning
 * Branches.
 * <p>
 * The addBranch()-Method gets called during class analysis. Whenever the
 * BytecodeInstructionPool detects a BytecodeInstruction that corresponds to a
 * Branch in the class under test as defined in
 * BytecodeInstruction.isActualBranch() it calls the registerAsBranch() method
 * of this class which in turn properly registers the instruction within this
 * pool.
 * <p>
 *
 */
public class BranchPool {

    // maps all known branch instructions to their branchId
    private final Map<BytecodeInstruction, Branch> registeredNormalBranches = new HashMap<>();

    // number of known Branches - used for actualBranchIds
    private int branchCounter = 0;

    private static final Map<ClassLoader, BranchPool> instanceMap = new HashMap<>();

    private final Map<String, Integer> branchOrdinalCounters = new HashMap<>();

    public static BranchPool getInstance(ClassLoader classLoader) {
        if (!instanceMap.containsKey(classLoader)) {
            instanceMap.put(classLoader, new BranchPool());
        }

        return instanceMap.get(classLoader);
    }

    @VisibleForTesting
    public static void resetForTesting(ClassLoader classLoader) {
        BranchPool pool = instanceMap.remove(classLoader);
        if (pool != null) {
            pool.registeredNormalBranches.clear();
            pool.branchOrdinalCounters.clear();
            pool.branchCounter = 0;
        }
    }
    // fill the pool

    

    /**
     * Called by the BytecodeInstructionPool whenever it detects an instruction
     * that corresponds to a Branch in the class under test as defined by
     * BytecodeInstruction.isActualBranch().
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     */
    public void registerAsBranch(BytecodeInstruction instruction) {
        if (!(instruction.isBranch()))
            throw new IllegalArgumentException("CFGVertex of a branch expected");
        if (isKnownAsBranch(instruction))
            return;

        registerInstruction(instruction);

    }

    private void registerInstruction(BytecodeInstruction v) {
        if (isKnownAsBranch(v))
            throw new IllegalStateException(
                    "expect registerInstruction() to be called at most once for each instruction");

        if (!v.isBranch())
            throw new IllegalArgumentException("expect given instruction to be a normal branch");

            registerNormalBranchInstruction(v);
    }

    private void registerNormalBranchInstruction(BytecodeInstruction v) {
        if (!v.isBranch())
            throw new IllegalArgumentException("normal branch instruction expceted");

        if (registeredNormalBranches.containsKey(v))
            throw new IllegalArgumentException(
                    "instruction already registered as a normal branch");

        branchCounter++;
        Branch b = new Branch(v, branchCounter);
        attachObjectiveIds(b);
        registeredNormalBranches.put(v, b);
        SimpleLogger.info("Branch " + branchCounter + " at line " + v.getLineNumber());
    }

    private void attachObjectiveIds(Branch branch) {
        if (branch == null) {
            return;
        }
        BytecodeInstruction instruction = branch.getInstruction();
        if (instruction == null || !instruction.isBranch()) {
            return;
    }
        try {
            if (!instruction.hasLineNumberSet()) {
                SimpleLogger.warn("Cannot attach objective ids to branch without line number: " + branch);
                return;
        }
            int lineNumber = instruction.getLineNumber();
            if (lineNumber <= 0) {
                SimpleLogger.warn("Invalid line number while attaching objective ids to branch: " + branch);
                return;
            }
            String className = instruction.getClassName();
            String methodName = instruction.getMethodName();
            int ordinal = nextOrdinalForLine(className, methodName, lineNumber);
            int opcode = instruction.getASMNode().getOpcode();
            String thenId = ObjectiveNaming.branchObjectiveName(className, lineNumber, ordinal, true, opcode);
            String elseId = ObjectiveNaming.branchObjectiveName(className, lineNumber, ordinal, false, opcode);
            branch.setObjectiveIds(thenId, elseId);
        } catch (Exception e) {
            SimpleLogger.warn("Failed to attach objective ids to branch " + branch + ": " + e.getMessage());
        }
    }

    private int nextOrdinalForLine(String className, String methodName, int lineNumber) {
        String key = className + "#" + methodName + "#" + lineNumber;
        Integer ordinal = branchOrdinalCounters.getOrDefault(key, 0);
        branchOrdinalCounters.put(key, ordinal + 1);
        return ordinal;
    }

    // retrieve information from the pool

    /**
     * Checks whether the given instruction has Branch objects associated with
     * it.
     * <p>
     * Returns true if the given BytecodeInstruction previously passed a call to
     * registerAsBranch(instruction), false otherwise
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    public boolean isKnownAsBranch(BytecodeInstruction instruction) {
        return registeredNormalBranches.containsKey(instruction);
    }

    /**
     * <p>
     * getBranchForInstruction
     * </p>
     *
     * @param instruction a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction} object.
     * @return a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch.Branch} object.
     */
    public Branch getBranchForInstruction(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");
        if (!isKnownAsBranch(instruction))
            throw new IllegalArgumentException(
                    "expect given instruction to be known as a normal branch");

        Branch branch = registeredNormalBranches.get(instruction);
        if (branch == null) {
            throw new IllegalStateException("Branch not found for instruction");
        }
        return branch;
    }

}
