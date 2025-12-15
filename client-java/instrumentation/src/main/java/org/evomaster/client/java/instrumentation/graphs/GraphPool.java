/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster.
 */
package org.evomaster.client.java.instrumentation.graphs;

import org.evomaster.client.java.instrumentation.ClassesToExclude;
import com.google.common.annotations.VisibleForTesting;
import org.evomaster.client.java.instrumentation.graphs.cdg.ControlDependenceGraph;
import org.evomaster.client.java.instrumentation.graphs.cfg.ActualControlFlowGraph;
import org.evomaster.client.java.instrumentation.graphs.cfg.BytecodeInstruction;
import org.evomaster.client.java.instrumentation.graphs.cfg.ControlDependency;
import org.evomaster.client.java.instrumentation.graphs.cfg.RawControlFlowGraph;
import org.evomaster.client.java.instrumentation.graphs.cfg.branch.Branch;
import org.evomaster.client.java.instrumentation.external.ControlDependenceSnapshot;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.controller.api.dto.ControlDependenceGraphDto;
import org.evomaster.client.java.controller.api.dto.BranchObjectiveDto;
import org.evomaster.client.java.controller.api.dto.DependencyEdgeDto;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives access to all Graphs computed during CUT analysis such as CFGs created
 * by the CFGGenerator and BytcodeAnalyzer in the CFGMethodVisitor
 * <p>
 * For each CUT and each of their methods a Raw- and an ActualControlFlowGraph
 * instance are stored within this pool. Additionally a ControlDependenceGraph
 * is computed and stored for each such method.
 * <p>
 * This pool stores per-method CFGs and CDGs computed during analysis.
 *
 */
public class GraphPool {

    private static final Map<ClassLoader, GraphPool> instanceMap = new HashMap<>();
    private static final List<ControlDependenceGraphDto> exportedCdgs = new ArrayList<>();
    private static final Object exportLock = new Object();

    private final ClassLoader classLoader;

    /**
     * Private constructor
     */
    private GraphPool(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private static boolean isWriteCfgEnabled() {
        return ControlDependenceGraphConfig.isWriteCfgEnabled();
    }

    public static GraphPool getInstance(ClassLoader classLoader) {
        if (!instanceMap.containsKey(classLoader)) {
            instanceMap.put(classLoader, new GraphPool(classLoader));
        }

        return instanceMap.get(classLoader);
    }

    @VisibleForTesting
    public static void resetForTesting(ClassLoader classLoader) {
        instanceMap.remove(classLoader);
        synchronized (exportLock) {
            exportedCdgs.clear();
        }
    }

    /**
     * Complete control flow graph, contains each bytecode instruction, each
     * label and line number node Think of the direct Known Subclasses of
     * http://
     * asm.ow2.org/asm33/javadoc/user/org/objectweb/asm/tree/AbstractInsnNode
     * .html for a complete list of the nodes in this cfg
     * <p>
     * Maps from classNames to methodNames to corresponding RawCFGs
     */
    private final Map<String, Map<String, RawControlFlowGraph>> rawCFGs = new HashMap<>();

    /**
     * Minimized control flow graph. This graph only contains the first and last
     * node (usually a LABEL and IRETURN), nodes which create branches (all
     * jumps/switches except GOTO) and nodes which were mutated.
     * <p>
     * Maps from classNames to methodNames to corresponding ActualCFGs
     */
    private final Map<String, Map<String, ActualControlFlowGraph>> actualCFGs = new HashMap<>();

    /**
     * Control Dependence Graphs for each method.
     * <p>
     * Maps from classNames to methodNames to corresponding CDGs
     */
    private final Map<String, Map<String, ControlDependenceGraph>> controlDependencies = new HashMap<>();

    // retrieve graphs

    /**
     * Returns the {@link RawControlFlowGraph} of the specified method. To this end, one has to
     * provide
     * <ul>
     *     <li>the fully qualified name of the class containing the desired method, and</li>
     *     <li>a string consisting of the method name concatenated with the corresponding
     *     method descriptor.</li>
     * </ul>
     *
     * @param className  the fully qualified name of the containing class
     * @param methodName concatenation of method name and descriptor
     * @return the raw control flow graph
     */
    public RawControlFlowGraph getRawCFG(String className, String methodName) {

        if (rawCFGs.get(className) == null) {
            SimpleLogger.warn("Class unknown: " + className);
            SimpleLogger.warn(rawCFGs.keySet().toString());
            return null;
        }

        return rawCFGs.get(className).get(methodName);
    }

    /**
     * <p>
     * getActualCFG
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cfg.ActualControlFlowGraph} object.
     */
    public ActualControlFlowGraph getActualCFG(String className, String methodName) {

        if (actualCFGs.get(className) == null)
            return null;

        return actualCFGs.get(className).get(methodName);
    }

    /**
     * <p>
     * getCDG
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     * @return a {@link org.evomaster.client.java.instrumentation.graphs.cdg.ControlDependenceGraph} object.
     */
    public ControlDependenceGraph getCDG(String className, String methodName) {

        if (controlDependencies.get(className) == null)
            return null;

        return controlDependencies.get(className).get(methodName);
    }

    public static ControlDependenceSnapshot exportSnapshotFromIndex(int fromIndex) {
        synchronized (exportLock) {
            int start = Math.max(0, Math.min(fromIndex, exportedCdgs.size()));
            List<ControlDependenceGraphDto> slice = new ArrayList<>(exportedCdgs.subList(start, exportedCdgs.size()));
            return new ControlDependenceSnapshot(slice, exportedCdgs.size());
        }
    }

    // register graphs

    /**
     * <p>
     * registerRawCFG
     * </p>
     *
     * @param cfg a {@link org.evomaster.client.java.instrumentation.graphs.cfg.RawControlFlowGraph} object.
     */
    public void registerRawCFG(RawControlFlowGraph cfg) {
        String className = cfg.getClassName();
        String methodName = cfg.getMethodName();

        if (className == null || methodName == null)
            throw new IllegalStateException(
                    "expect class and method name of CFGs to be set before entering the GraphPool");

        if (!rawCFGs.containsKey(className)) {
            rawCFGs.put(className, new HashMap<>());
        }
        Map<String, RawControlFlowGraph> methods = rawCFGs.get(className);
        methods.put(methodName, cfg);

        if (isWriteCfgEnabled())
            cfg.toDot();
    }

    /**
     * <p>
     * registerActualCFG
     * </p>
     *
     * @param cfg a {@link org.evomaster.client.java.instrumentation.graphs.cfg.ActualControlFlowGraph}
     *            object.
     */
    public void registerActualCFG(ActualControlFlowGraph cfg) {
        String className = cfg.getClassName();
        String methodName = cfg.getMethodName();

        if (className == null || methodName == null)
            throw new IllegalStateException(
                    "expect class and method name of CFGs to be set before entering the GraphPool");

        if (!actualCFGs.containsKey(className)) {
            actualCFGs.put(className, new HashMap<>());
            // diameters.put(className, new HashMap<String, Double>());
        }
        Map<String, ActualControlFlowGraph> methods = actualCFGs.get(className);
        methods.put(methodName, cfg);

        if (isWriteCfgEnabled())
            cfg.toDot();

        if (shouldInstrument(cfg.getClassName(), cfg.getMethodName())) {
            createAndRegisterControlDependence(cfg);
        }
    }

    private boolean shouldInstrument(String className, String methodName) {
        return ClassesToExclude.checkIfCanInstrument(classLoader, new ClassName(className));
    }

    private void createAndRegisterControlDependence(ActualControlFlowGraph cfg) {

        ControlDependenceGraph cd = new ControlDependenceGraph(cfg);

        String className = cd.getClassName();
        String methodName = cd.getMethodName();

        if (className == null || methodName == null)
            throw new IllegalStateException(
                    "expect class and method name of CFGs to be set before entering the GraphPool");

        if (!controlDependencies.containsKey(className))
            controlDependencies.put(className,
                    new HashMap<>());
        Map<String, ControlDependenceGraph> cds = controlDependencies.get(className);

        cds.put(methodName, cd);
        if (isWriteCfgEnabled())
            cd.toDot();

        ControlDependenceGraphDto dto = buildControlDependenceDto(className, methodName, cd);
        if (dto != null) {
            appendExportLog(dto);
        }
    }

    /**
     * <p>
     * clear
     * </p>
     */
    public void clear() {
        rawCFGs.clear();
        actualCFGs.clear();
        controlDependencies.clear();
    }

    /**
     * <p>
     * clear
     * </p>
     *
     * @param className a {@link java.lang.String} object.
     */
    public void clear(String className) {
        rawCFGs.remove(className);
        actualCFGs.remove(className);
        controlDependencies.remove(className);
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
        if (rawCFGs.containsKey(className))
            rawCFGs.get(className).remove(methodName);
        if (actualCFGs.containsKey(className))
            actualCFGs.get(className).remove(methodName);
        if (controlDependencies.containsKey(className))
            controlDependencies.get(className).remove(methodName);
    }

    public static void clearAll(String className) {
        instanceMap.values().forEach(pool -> pool.clear(className));
    }

    public static void clearAll(String className, String methodName) {
        instanceMap.values().forEach(pool -> pool.clear(className, methodName));
    }

    public static void clearAll() {
        instanceMap.clear();
        synchronized (exportLock) {
            exportedCdgs.clear();
        }
    }

    public static void refreshAllCdgs() {
        synchronized (exportLock) {
            exportedCdgs.clear();
        }
        instanceMap.values().forEach(GraphPool::refreshCdgs);
    }

    private void refreshCdgs() {
        for (String className : controlDependencies.keySet()) {
            for (String methodName : controlDependencies.get(className).keySet()) {
                ControlDependenceGraph cdg = controlDependencies.get(className).get(methodName);
                ControlDependenceGraphDto dto = buildControlDependenceDto(className, methodName, cdg);
                if (dto != null) {
                    appendExportLog(dto);
                }
            }
        }
    }

    private static void appendExportLog(ControlDependenceGraphDto dto) {
        if (dto == null) {
            return;
        }
        synchronized (exportLock) {
            exportedCdgs.add(dto);
        }
    }

    private ControlDependenceGraphDto buildControlDependenceDto(String className,
                                                                String methodName,
                                                                ControlDependenceGraph cdg) {
        if (cdg == null) {
            return null;
        }
        ActualControlFlowGraph cfg = getActualCFG(className, methodName);
        if (cfg == null) {
            SimpleLogger.warn("Cannot export CDG for " + className + "#" + methodName + " as ActualCFG is missing");
            return null;
        }

        Map<Integer, BranchObjectiveDto> objectiveMap = new LinkedHashMap<>();
        Set<Integer> rootObjectives = new LinkedHashSet<>();
        Map<Integer, LinkedHashSet<Integer>> adjacency = new HashMap<>();

        // Iterate over all branches in the ActualCFG.
        for (BytecodeInstruction branchInstruction : cfg.getBranches()) {
            // Branches are stored in the BranchPool as they are instrumented.
            // Here we retrieve the Branch object from the BranchPool, by its BytecodeInstruction.
            // Branch also stores the descriptive identifiers for the "true" and "false" outcomes.
            Branch branch = branchInstruction.toBranch();
            if (branch == null) {
                continue;
            }


            List<Integer> branchObjectives = collectObjectiveIds(branch, objectiveMap);
            if (branchObjectives.isEmpty()) {
                continue;
            }

            Set<ControlDependency> dependencies = branchInstruction.getControlDependencies();
            if (dependencies == null || dependencies.isEmpty()) {
                rootObjectives.addAll(branchObjectives);
                continue;
            }

            for (ControlDependency dependency : dependencies) {
                Branch parent = dependency.getBranch();
                if (parent == null) {
                    continue;
                }
                Integer parentObjective = resolveParentObjectiveId(parent, dependency.getBranchExpressionValue(), objectiveMap);
                if (parentObjective == null) {
                    continue;
                }

                adjacency.computeIfAbsent(parentObjective, key -> new LinkedHashSet<>())
                        .addAll(branchObjectives);
            }
        }

        if (objectiveMap.isEmpty() && adjacency.isEmpty()) {
            return null;
        }

        List<DependencyEdgeDto> edges = new ArrayList<>();
        adjacency.forEach((parent, childrenIds) -> {
            for (Integer child : childrenIds) {
                DependencyEdgeDto edge = new DependencyEdgeDto();
                edge.parentObjectiveId = parent;
                edge.childObjectiveId = child;
                edges.add(edge);
            }
        });

        ControlDependenceGraphDto dto = new ControlDependenceGraphDto();
        dto.className = className;
        dto.methodName = methodName;
        dto.objectives = new ArrayList<>(objectiveMap.values());
        dto.rootObjectiveIds = new ArrayList<>(rootObjectives);
        dto.edges = edges;
        return dto;
    }

    private List<Integer> collectObjectiveIds(Branch branch,
                                              Map<Integer, BranchObjectiveDto> objectiveMap) {
        List<Integer> ids = new ArrayList<>(2);
        if (branch.getThenObjectiveId() != null) {
            ids.add(registerObjective(branch.getThenObjectiveId(), objectiveMap));
        }
        if (branch.getElseObjectiveId() != null) {
            ids.add(registerObjective(branch.getElseObjectiveId(), objectiveMap));
        }
        return ids;
    }

    private Integer resolveParentObjectiveId(Branch branch,
                                             boolean branchExpressionValue,
                                             Map<Integer, BranchObjectiveDto> objectiveMap) {
        String descriptiveId = branchExpressionValue
                ? branch.getThenObjectiveId()
                : branch.getElseObjectiveId();
        if (descriptiveId == null) {
            return null;
        }
        return registerObjective(descriptiveId, objectiveMap);
    }

    private Integer registerObjective(String descriptiveId,
                                      Map<Integer, BranchObjectiveDto> objectiveMap) {
        int id = ObjectiveRecorder.getMappedId(descriptiveId);
        objectiveMap.computeIfAbsent(id, key -> {
            BranchObjectiveDto obj = new BranchObjectiveDto();
            obj.id = id;
            obj.descriptiveId = descriptiveId;
            return obj;
        });
        return id;
    }

}
