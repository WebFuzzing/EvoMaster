/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * AnnotatedMethodNode wraps ASM's MethodNode to support Dynamosa-specific metadata.
 */
public class AnnotatedMethodNode extends MethodNode {

    /**
     * <p>Constructor for AnnotatedMethodNode.</p>
     *
     * @param access     a int.
     * @param name       a {@link java.lang.String} object.
     * @param desc       a {@link java.lang.String} object.
     * @param signature  a {@link java.lang.String} object.
     * @param exceptions an array of {@link java.lang.String} objects.
     */
    public AnnotatedMethodNode(int access, String name, String desc, String signature,
                               String[] exceptions) {
        super(Opcodes.ASM9, access, name, desc, signature, exceptions);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the LabelNode corresponding to the given Label. Creates a new
     * LabelNode if necessary. The default implementation of this method uses
     * the {@link Label#info} field to store associations between labels and
     * label nodes.
     */
    @Override
    protected LabelNode getLabelNode(final Label l) {
        if (l instanceof AnnotatedLabel) {
            AnnotatedLabel al = (AnnotatedLabel) l;
            al.setParent(new LabelNode(al));
            return al.getParent();
        } else {
            if (!(l.info instanceof LabelNode)) {
                l.info = new LabelNode(l);
            }
            return (LabelNode) l.info;
        }
    }
}
