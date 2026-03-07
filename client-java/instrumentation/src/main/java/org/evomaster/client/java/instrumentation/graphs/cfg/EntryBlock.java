/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster.
 */
package org.evomaster.client.java.instrumentation.graphs.cfg;

public class EntryBlock extends BasicBlock {

    /**
     * <p>Constructor for EntryBlock.</p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param methodName a {@link java.lang.String} object.
     */
    public EntryBlock(String className, String methodName) {
        super(className, methodName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEntryBlock() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "EntryBlock for method " + methodName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }
}
