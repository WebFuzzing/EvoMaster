/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

/**
 * Annotated labels are used to identify instrumented code
 */
public class AnnotatedLabel extends Label {

    private boolean isStart = false;

    private boolean ignore = false;

    private boolean ignoreFalse = false;

    private LabelNode parent = null;

    public AnnotatedLabel(boolean ignore, boolean start) {
        this.ignore = ignore;
        this.isStart = start;
    }

    public AnnotatedLabel(boolean ignore, boolean start, LabelNode parent) {
        this.ignore = ignore;
        this.isStart = start;
        this.parent = parent;
    }

    public boolean isStartTag() {
        return isStart;
    }

    public boolean shouldIgnore() {
        return ignore;
    }

    public void setIgnoreFalse(boolean value) {
        ignoreFalse = value;
    }

    public boolean shouldIgnoreFalse() {
        return ignoreFalse;
    }

    public LabelNode getParent() {
        return parent;
    }

    public void setParent(LabelNode parent) {
        this.parent = parent;
    }
}
