/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.branch;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BytecodeInstruction;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

// TODO: root branches should not be special cases
// every root branch should be a branch just
// like every other branch with it's own branchId and all

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
 * There are two kinds of Branch objects: normal branches and switch case
 * branches. For more details about the difference between these two look at the
 * Branch class.
 *
 * @author Andre Mis
 */
public class BranchPool {

    // maps className -> method inside that class -> list of branches inside
    // that method
    private final Map<String, Map<String, List<Branch>>> branchMap = new HashMap<>();

    // set of all known methods without a Branch
    private final Map<String, Map<String, Integer>> branchlessMethods = new HashMap<>();

    // maps the branchIDs assigned by this pool to their respective Branches
    private final Map<Integer, Branch> branchIdMap = new HashMap<>();

    // maps all known branch instructions to their branchId
    private final Map<BytecodeInstruction, Integer> registeredNormalBranches = new HashMap<>();

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
    // fill the pool

    

    /**
     * Called by the BytecodeInstructionPool whenever it detects an instruction
     * that corresponds to a Branch in the class under test as defined by
     * BytecodeInstruction.isActualBranch().
     *
     * @param instruction a {@link org.evosuite.graphs.cfg.BytecodeInstruction} object.
     */
    public void registerAsBranch(BytecodeInstruction instruction) {
        if (!(instruction.isActualBranch()))
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
        registeredNormalBranches.put(v, branchCounter);

        Branch b = new Branch(v, branchCounter);
        attachObjectiveIds(b);
        addBranchToMap(b);
        branchIdMap.put(branchCounter, b);

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
            branch.setObjectiveIds(ordinal, thenId, elseId);
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

    private void addBranchToMap(Branch b) {

        SimpleLogger.info("Adding to map the branch " + b.toString());

        String className = b.getClassName();
        String methodName = b.getMethodName();

        if (!branchMap.containsKey(className))
            branchMap.put(className, new HashMap<>());
        if (!branchMap.get(className).containsKey(methodName))
            branchMap.get(className).put(methodName, new ArrayList<>());
        branchMap.get(className).get(methodName).add(b);
    }

    // retrieve information from the pool

    /**
     * Checks whether the given instruction has Branch objects associated with
     * it.
     * <p>
     * Returns true if the given BytecodeInstruction previously passed a call to
     * registerAsBranch(instruction), false otherwise
     *
     * @param instruction a {@link org.evosuite.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    public boolean isKnownAsBranch(BytecodeInstruction instruction) {
        return isKnownAsNormalBranchInstruction(instruction);
    }

    /**
     * <p>
     * isKnownAsNormalBranchInstruction
     * </p>
     *
     * @param ins a {@link org.evosuite.graphs.cfg.BytecodeInstruction} object.
     * @return a boolean.
     */
    public boolean isKnownAsNormalBranchInstruction(BytecodeInstruction ins) {

        return registeredNormalBranches.containsKey(ins);
    }

    /**
     * <p>
     * getActualBranchIdForNormalBranchInstruction
     * </p>
     *
     * @param ins a {@link org.evosuite.graphs.cfg.BytecodeInstruction} object.
     * @return a int.
     */
    public int getActualBranchIdForNormalBranchInstruction(BytecodeInstruction ins) {
        if (!isKnownAsNormalBranchInstruction(ins))
            throw new IllegalArgumentException(
                    "instruction not registered as a normal branch");

        if (registeredNormalBranches.containsKey(ins))
            return registeredNormalBranches.get(ins);

        throw new IllegalStateException(
                "expect registeredNormalBranches to contain a key for each known normal branch instruction");
    }

    /**
     * <p>
     * getBranchForInstruction
     * </p>
     *
     * @param instruction a {@link org.evosuite.graphs.cfg.BytecodeInstruction} object.
     * @return a {@link org.evosuite.coverage.branch.Branch} object.
     */
    public Branch getBranchForInstruction(BytecodeInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("null given");
        if (!isKnownAsNormalBranchInstruction(instruction))
            throw new IllegalArgumentException(
                    "expect given instruction to be known as a normal branch");

        return getBranch(registeredNormalBranches.get(instruction));
    }

    /**
     * Returns the number of known Branches for a given methodName in a given
     * class.
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @return The number of currently known Branches inside the given method
     */
    public int getBranchCountForMethod(String className, String methodName) {
        if (branchMap.get(className) == null)
            return 0;
        if (branchMap.get(className).get(methodName) == null)
            return 0;

        return branchMap.get(className).get(methodName).size();
    }

    public int getNonArtificialBranchCountForMethod(String className,
                                                    String methodName) {
        if (branchMap.get(className) == null)
            return 0;
        if (branchMap.get(className).get(methodName) == null)
            return 0;

        int num = 0;
        for (Branch b : branchMap.get(className).get(methodName)) {
            if (!b.isInstrumented())
                num++;
        }

        return num;
    }

    /**
     * Returns the number of known Branches for a given class
     *
     * @param className a {@link java.lang.String} object.
     * @return The number of currently known Branches inside the given class
     */
    public int getBranchCountForClass(String className) {
        if (branchMap.get(className) == null)
            return 0;
        int total = 0;
        for (String method : branchMap.get(className).keySet()) {
            total += branchMap.get(className).get(method).size();
        }
        return total;
    }

    /**
     * Returns the number of known Branches for a given class
     *
     * @param prefix a {@link java.lang.String} object.
     * @return The number of currently known Branches inside the given class
     */
    public int getBranchCountForPrefix(String prefix) {
        int num = 0;
        for (String className : branchMap.keySet()) {
            if (className.startsWith(prefix)) {
                SimpleLogger.info("Found matching class for branch count: " + className + "/"
                        + prefix);
                for (String method : branchMap.get(className).keySet()) {
                    num += branchMap.get(className).get(method).size();
                }
            }
        }
        return num;
    }

    /**
     * Returns the number of known Branches for a given class
     *
     * @param prefix a {@link java.lang.String} object.
     * @return The number of currently known Branches inside the given class
     */
    public Set<Integer> getBranchIdsForPrefix(String prefix) {
        Set<Integer> ids = new LinkedHashSet<>();
        Set<Branch> sutBranches = new LinkedHashSet<>();
        for (String className : branchMap.keySet()) {
            if (className.startsWith(prefix)) {
                SimpleLogger.info("Found matching class for branch ids: " + className + "/"
                        + prefix);
                for (String method : branchMap.get(className).keySet()) {
                    sutBranches.addAll(branchMap.get(className).get(method));
                }
            }
        }

        for (Integer id : branchIdMap.keySet()) {
            if (sutBranches.contains(branchIdMap.get(id))) {
                ids.add(id);
            }
        }

        return ids;
    }

    /**
     * Returns the number of known Branches for a given class
     *
     * @param prefix a {@link java.lang.String} object.
     * @return The number of currently known Branches inside the given class
     */
    public int getBranchCountForMemberClasses(String prefix) {
        int num = 0;
        for (String className : branchMap.keySet()) {
            if (className.equals(prefix) || className.startsWith(prefix + "$")) {
                SimpleLogger.info("Found matching class for branch count: " + className + "/"
                        + prefix);
                for (String method : branchMap.get(className).keySet()) {
                    num += branchMap.get(className).get(method).size();
                }
            }
        }
        return num;
    }

    /**
     * Returns the number of currently known Branches
     *
     * @return The number of currently known Branches
     */
    public int getBranchCounter() {
        return branchCounter;
    }

    public int getNumArtificialBranches() {
        int num = 0;
        for (Branch b : branchIdMap.values()) {
            if (b.isInstrumented())
                num++;
        }

        return num;
    }

    /**
     * Returns the Branch object associated with the given branchID
     *
     * @param branchId The ID of a branch
     * @return The branch, or null if it does not exist
     */
    public Branch getBranch(int branchId) {

        return branchIdMap.get(branchId);
    }

    public Collection<Branch> getAllBranches() {
        return branchIdMap.values();
    }

    /**
     * Returns a Set containing all classes for which this pool knows Branches
     * for as Strings
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<String> knownClasses() {
        Set<String> r = new LinkedHashSet<>();
        r.addAll(branchMap.keySet());
        r.addAll(branchlessMethods.keySet());

        return r;
    }

    /**
     * Returns a Set containing all methods in the class represented by the
     * given String for which this pool knows Branches for as Strings
     *
     * @param className a {@link java.lang.String} object.
     * @return a {@link java.util.Set} object.
     */
    public Set<String> knownMethods(String className) {
        Set<String> r = new LinkedHashSet<>();
        Map<String, List<Branch>> methods = branchMap.get(className);
        if (methods != null)
            r.addAll(methods.keySet());

        return r;
    }

    /**
     * Returns a List containing all Branches in the given class and method
     * <p>
     * Should no such Branch exist an empty List is returned
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<Branch> retrieveBranchesInMethod(String className,
                                                 String methodName) {
        List<Branch> r = new ArrayList<>();
        if (branchMap.get(className) == null)
            return r;
        List<Branch> branches = branchMap.get(className).get(methodName);
        if (branches != null)
            r.addAll(branches);
        return r;
    }

    /**
     * Reset all the data structures used to keep track of the branch
     * information
     */
    public void reset() {
        branchCounter = 0;
        branchMap.clear();
        branchlessMethods.clear();
        branchIdMap.clear();
        registeredNormalBranches.clear();
        branchOrdinalCounters.clear();
    }

    /**
     * <p>
     * clear
     * </p>
     * <p>
     * TODO: One of these two methods should go
     */
    public void clear() {
        branchCounter = 0;
        branchMap.clear();
        branchIdMap.clear();
        branchlessMethods.clear();
        registeredNormalBranches.clear();
        branchOrdinalCounters.clear();
    }

    /**
     * <p>
     * clear
     * </p>
     *
     * @param className a {@link java.lang.String} object.
     */
    public void clear(String className) {
        branchMap.remove(className);
        branchlessMethods.remove(className);
        removeOrdinalCountersForClass(className);
    }

    /**
     * <p>
     * clear
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     */
    public void clear(String className, String methodName) {
        int numBranches = 0;

        if (branchMap.containsKey(className)) {
            if (branchMap.get(className).containsKey(methodName))
                numBranches = branchMap.get(className).get(methodName).size();
            branchMap.get(className).remove(methodName);
        }
        if (branchlessMethods.containsKey(className))
            branchlessMethods.get(className).remove(methodName);
        removeOrdinalCountersForMethod(className, methodName);
        SimpleLogger.info("Resetting branchCounter from " + branchCounter + " to "
                + (branchCounter - numBranches));
        branchCounter -= numBranches;
    }

    private void removeOrdinalCountersForClass(String className) {
        if (className == null) {
            return;
        }
        String prefix = className + "#";
        branchOrdinalCounters.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private void removeOrdinalCountersForMethod(String className, String methodName) {
        if (className == null || methodName == null) {
            return;
        }
        String prefix = className + "#" + methodName + "#";
        branchOrdinalCounters.keySet().removeIf(key -> key.startsWith(prefix));
    }

}
