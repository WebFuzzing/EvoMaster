package org.evomaster.client.java.instrumentation.dynamosa.graphs.cdg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DominatorNodeTest {

    @Test
    void constructorSetsNodeAndLabel() {
        DominatorNode<String> node = new DominatorNode<>("entry");
        assertEquals("entry", node.node);
        assertSame(node, node.label);
    }

    @Test
    void linkSetsAncestor() {
        DominatorNode<String> node = new DominatorNode<>("entry");
        DominatorNode<String> ancestor = new DominatorNode<>("ancestor");
        node.link(ancestor);
        assertSame(ancestor, node.ancestor);
    }

    @Test
    void evalReturnsSelfWhenAncestorIsNull() {
        DominatorNode<String> node = new DominatorNode<>("entry");
        assertSame(node, node.eval());
    }

    @Test
    void evalCompressesAncestorWhenPresentAndReturnsUpdatedLabel() {
        DominatorNode<String> node = new DominatorNode<>("node");
        DominatorNode<String> ancestor = new DominatorNode<>("ancestor");
        DominatorNode<String> ancestorAncestor = new DominatorNode<>("ancestorAncestor");

        // In the Lengauer-Tarjan algorithm used to create the DominatorTree, every visited node has a N value
        // N is the order of discovery of the node in the DFS of the DominatorTree
        // Smaller N means its higher in the tree (closer to the root)
        // Label always points to the node's best dominator candidate so far (lowest N value)

        // From every path on the start to the node, the node's semi-dominator is the node with the lowest N value
        node.semiDominator = new DominatorNode<>("nodeSemi");
        node.semiDominator.n = 10;

        ancestor.semiDominator = new DominatorNode<>("ancestorSemi");
        ancestor.semiDominator.n = 1;

        // Link the nodes together to form a tree
        node.link(ancestor);
        ancestor.link(ancestorAncestor);

        // Evaluate the node to find its immediate dominator
        DominatorNode<String> result = node.eval();

        // eval must return the best candidate, in this case the ancestor
        assertSame(ancestor, result);
        // label must be updated to the best candidate
        assertSame(ancestor, node.label);
        // compression also flattens the ancestor chain, so node.ancestor now points
        // directly to the ancestor's ancestor
        assertSame(ancestorAncestor, node.ancestor);
    }

    @Test
    void isRootNodeReturnsTrueWhenNIsOne() {
        DominatorNode<String> node = new DominatorNode<>("entry");
        node.n = 1;
        assertTrue(node.isRootNode());
    }

    @Test
    void isRootNodeReturnsFalseWhenNGreaterThanOne() {
        DominatorNode<String> node = new DominatorNode<>("entry");
        node.n = 2;
        assertFalse(node.isRootNode());
    }

    @Test
    void getFromBucketReturnsFirstElement() {
        DominatorNode<String> node = new DominatorNode<>("A");
        DominatorNode<String> bucketNode = new DominatorNode<>("B");
        node.bucket.add(bucketNode);
        assertSame(bucketNode, node.getFromBucket());
    }
}
