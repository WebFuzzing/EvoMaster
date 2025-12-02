/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cdg;

import java.util.HashSet;
import java.util.Set;

/**
 * This class serves as a convenience data structure within cfg.DominatorTree
 * <p>
 * For every node within a CFG for which the immediateDominators are to be
 * computed this class holds auxiliary information needed during the computation
 * inside the DominatorTree
 * <p>
 * After that computation instances of this class hold the connection between
 * CFG nodes and their immediateDominators
 * <p>
 * Look at cfg.DominatorTree for more detailed information
 *
 */
class DominatorNode<V> {

    final V node;
    int n = 0;

    // parent of node within spanning tree of DFS inside cfg.DominatorTree
    DominatorNode<V> parent;

    // computed dominators
    DominatorNode<V> semiDominator;
    DominatorNode<V> immediateDominator;

    // auxiliary field needed for dominator computation
    Set<DominatorNode<V>> bucket = new HashSet<>();

    // data structure needed to represented forest produced during cfg.DominatorTree computation
    DominatorNode<V> ancestor;
    DominatorNode<V> label;

    DominatorNode(V node) {
        this.node = node;

        this.label = this;
    }

    void link(DominatorNode<V> v) {
        ancestor = v;
    }

    DominatorNode<V> eval() {
        if (ancestor == null)
            return this;

        compress();

        return label;
    }

    void compress() {
        if (ancestor == null)
            throw new IllegalStateException("may only be called when ancestor is set");

        if (ancestor.ancestor != null) {
            ancestor.compress();
            if (ancestor.label.semiDominator.n < label.semiDominator.n)
                label = ancestor.label;

            ancestor = ancestor.ancestor;
        }
    }

    DominatorNode<V> getFromBucket() {

        for (DominatorNode<V> r : bucket)
            return r;

        return null;
    }

    /**
     * <p>isRootNode</p>
     *
     * @return a boolean.
     */
    public boolean isRootNode() {
        // TODO not that nice :/
        return n == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DTNode " + n + " - " + node;
    }
}
