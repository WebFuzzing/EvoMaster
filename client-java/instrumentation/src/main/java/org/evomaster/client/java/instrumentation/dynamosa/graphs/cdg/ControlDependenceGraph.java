/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cdg;

import org.evomaster.client.java.instrumentation.dynamosa.graphs.EvoMasterGraph;
import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.*;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.LinkedHashSet;
import java.util.Set;

public class ControlDependenceGraph extends EvoMasterGraph<BasicBlock, ControlFlowEdge> {

    private final ActualControlFlowGraph cfg;

    private final String className;
    private final String methodName;

    /**
     * <p>Constructor for ControlDependenceGraph.</p>
     *
     * @param cfg a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.ActualControlFlowGraph} object.
     */
    public ControlDependenceGraph(ActualControlFlowGraph cfg) {
        super(ControlFlowEdge.class);

        this.cfg = cfg;
        this.className = cfg.getClassName();
        this.methodName = cfg.getMethodName();

        computeGraph();
    }

    
    /**
     * Returns a Set containing all Branches the given BasicBlock is control
     * dependent on.
     * <p>
     * This is for each incoming ControlFlowEdge of the given block within this
     * CDG, the branch instruction of that edge will be added to the returned
     * set.
     *
     * @param insBlock a {@link org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.BasicBlock} object.
     * @return a {@link java.util.Set} object.
     */
    public Set<ControlDependency> getControlDependentBranches(BasicBlock insBlock) {
        if (insBlock == null)
            throw new IllegalArgumentException("null not accepted");
        if (!containsVertex(insBlock))
            throw new IllegalArgumentException("unknown block: " + insBlock.getName());

        if (insBlock.hasControlDependenciesSet())
            return insBlock.getControlDependencies();

        Set<ControlDependency> r = retrieveControlDependencies(insBlock,
                new LinkedHashSet<>());

        return r;
    }

    private Set<ControlDependency> retrieveControlDependencies(BasicBlock insBlock,
                                                               Set<ControlFlowEdge> handled) {

        Set<ControlDependency> r = new LinkedHashSet<>();

        for (ControlFlowEdge e : incomingEdgesOf(insBlock)) {
            if (handled.contains(e))
                continue;
            handled.add(e);

            ControlDependency cd = e.getControlDependency();
            if (cd != null)
                r.add(cd);
            else {
                BasicBlock in = getEdgeSource(e);
                if (!in.equals(insBlock))
                    r.addAll(retrieveControlDependencies(in, handled));
            }

        }

        return r;
    }

    // init

    private void computeGraph() {

        createGraphNodes();
        computeControlDependence();
    }

    private void createGraphNodes() {
        // copy CFG nodes
        addVertices(cfg.vertexSet());

        for (BasicBlock b : vertexSet())
            if (b.isExitBlock() && !graph.removeVertex(b)) // TODO refactor
                throw new IllegalStateException("internal error building up CDG");

    }

    private void computeControlDependence() {

        ActualControlFlowGraph rcfg = cfg.computeReverseCFG();
        DominatorTree<BasicBlock> dt = new DominatorTree<>(rcfg);

        for (BasicBlock b : rcfg.vertexSet())
            if (!b.isExitBlock()) {

                SimpleLogger.debug("DFs for: " + b.getName());
                for (BasicBlock cd : dt.getDominatingFrontiers(b)) {
                    ControlFlowEdge orig = cfg.getEdge(cd, b);

                    if (!cd.isEntryBlock() && orig == null) {
                        // in for loops for example it can happen that cd and b
                        // were not directly adjacent to each other in the CFG
                        // but rather there were some intermediate nodes between
                        // them and the needed information is inside one of the
                        // edges
                        // from cd to the first intermediate node. more
                        // precisely cd is expected to be a branch and to have 2
                        // outgoing edges, one for evaluating to true (jumping)
                        // and one for false. one of them can be followed and b
                        // will eventually be reached, the other one can not be
                        // followed in that way. 

                        SimpleLogger.debug("cd: " + cd);
                        SimpleLogger.debug("b: " + b);

                        // TODO this is just for now! unsafe and probably not
                        // even correct!
                        Set<ControlFlowEdge> candidates = cfg.outgoingEdgesOf(cd);
                        if (candidates.size() < 2)
                            throw new IllegalStateException("unexpected");

                        boolean leadToB = false;
                        boolean skip = false;

                        for (ControlFlowEdge e : candidates) {
                            if (!e.hasControlDependency()) {
                                skip = true;
                                break;
                            }

                            if (cfg.leadsToNode(e, b)) {
                                if (leadToB)
                                    orig = null;
                                // throw new
                                // IllegalStateException("unexpected");
                                leadToB = true;

                                orig = e;
                            }
                        }
                        if (skip)
                            continue;
                        if (!leadToB)
                            throw new IllegalStateException("unexpected");
                    }

                    if (orig == null)
                        SimpleLogger.debug("orig still null!");

                    if (!addEdge(cd, b, new ControlFlowEdge(orig)))
                        throw new IllegalStateException(
                                "internal error while adding CD edge");

                    SimpleLogger.debug("  " + cd.getName());
                }
            }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        // return "CDG" + graphId + "_" + methodName;
        return methodName + "_" + "CDG";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String dotSubFolder() {
        return toFileString(className) + "/CDG/";
    }

    /**
     * <p>Getter for the field <code>className</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getClassName() {
        return className;
    }

    /**
     * <p>Getter for the field <code>methodName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMethodName() {
        return methodName;
    }
}
