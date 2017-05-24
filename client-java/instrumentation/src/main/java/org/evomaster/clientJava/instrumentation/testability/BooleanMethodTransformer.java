package org.evomaster.clientJava.instrumentation.testability;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;
import java.util.ListIterator;
import java.util.Objects;

public abstract class BooleanMethodTransformer {

    protected final Class<?> target;

    protected BooleanMethodTransformer(Class<?> target) {
        this.target = Objects.requireNonNull(target);
    }

    public Class<?> getTarget() {
        return target;
    }

    public void transformClass(ClassNode cn) {

        for (Object mn : cn.methods) {
            transformMethod((MethodNode) mn);
        }
    }

    public boolean transformMethod(MethodNode mn) {

        boolean changed = replaceBooleanCalls(mn);
        if (!changed) {
            return false;
        }

        AbstractInsnNode node = mn.instructions.getFirst();

        while (node != null) {

            if (!isReplaceMethod(node)) {
                node = node.getNext();
                continue;
            }

            AbstractInsnNode next = node.getNext();
            assert next != null;

            switch (next.getOpcode()) {
                case Opcodes.IFNE:
                    handleIFNE(next);
                    break;
                case Opcodes.IFEQ:
                    handleIFEQ(next);
                    break;
                case Opcodes.IRETURN:
                    handleIRETURN(mn, next);
                    break;
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_1:
                    handleICONST(mn, next);
                    break;
                case Opcodes.ISTORE:
                    handleISTORE(mn, next);
                    break;
                default:
                    throw new IllegalStateException("Not handled code in testability transformation: " + next.getOpcode());
            }

            node = next.getNext();
        }

        return true;
    }

    //TODO write explanation

    /*
        TODO: current transformation for ISTORE and IRETURN
        are pointless, ie just add unnecessary overhead.
        Would need to extend TT to create artificial branches
        for those cases.
     */

    private void handleISTORE(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.ISTORE;
        assert isReplaceMethod(node.getPrevious());

        MethodInsnNode n = new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(BooleanMethodTransformer.class),
                "convertIntToBoolean",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE,
                        new Type[]{Type.INT_TYPE}), false);

        mn.instructions.insertBefore(node, n);
    }

    private void handleIRETURN(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IRETURN;
        assert isReplaceMethod(node.getPrevious());


        MethodInsnNode n = new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(BooleanMethodTransformer.class),
                "convertIntToBoolean",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE,
                        new Type[]{Type.INT_TYPE}), false);

        mn.instructions.insertBefore(node, n);
    }

    private void handleIFEQ(AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IFEQ;
        assert isReplaceMethod(node.getPrevious());

        JumpInsnNode branch = (JumpInsnNode) node;
        branch.setOpcode(Opcodes.IFLE);
    }

    private void handleIFNE(AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IFNE;
        assert isReplaceMethod(node.getPrevious());

        JumpInsnNode branch = (JumpInsnNode) node;
        branch.setOpcode(Opcodes.IFGT);
    }

    private void handleICONST(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.ICONST_0 || node.getOpcode() == Opcodes.ICONST_1;
        assert isReplaceMethod(node.getPrevious());

        AbstractInsnNode next = node.getNext();
        assert next != null;

        if (next.getOpcode() == Opcodes.IF_ICMPEQ) {
            handleIF_ICMPEQ(mn, next);
        } else if (next.getOpcode() == Opcodes.IF_ICMPNE) {
            handleIF_ICMPNE(mn, next);
        } else {
            throw new IllegalStateException("Cannot handle ICONST case");
        }
    }

    private void handleIF_ICMPNE(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IF_ICMPNE;
        assert isReplaceMethod(node.getPrevious().getPrevious());

        JumpInsnNode branch = (JumpInsnNode) node;

        if (node.getPrevious().getOpcode() == Opcodes.ICONST_0) {
            branch.setOpcode(Opcodes.IFGT);
            mn.instructions.remove(node.getPrevious());
        } else if (node.getPrevious().getOpcode() == Opcodes.ICONST_1) {
            branch.setOpcode(Opcodes.IFLE);
            mn.instructions.remove(node.getPrevious());
        }
    }

    private void handleIF_ICMPEQ(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IF_ICMPEQ;
        assert isReplaceMethod(node.getPrevious().getPrevious());

        JumpInsnNode branch = (JumpInsnNode) node;
        if (node.getPrevious().getOpcode() == Opcodes.ICONST_0) {
            branch.setOpcode(Opcodes.IFLE);
            mn.instructions.remove(node.getPrevious());
        } else if (node.getPrevious().getOpcode() == Opcodes.ICONST_1) {
            branch.setOpcode(Opcodes.IFGT);
            mn.instructions.remove(node.getPrevious());
        }
    }


    private boolean replaceBooleanCalls(MethodNode mn) {

        boolean changed = false;
        ListIterator<AbstractInsnNode> iterator = mn.instructions.iterator();

        while (iterator.hasNext()) {
            AbstractInsnNode node = iterator.next();
            if (!(node instanceof MethodInsnNode)) {
                continue;
            }

            String replacementClass = Type.getInternalName(target);

            MethodInsnNode call = (MethodInsnNode) node;
            if (!call.owner.equals(replacementClass)) {
                continue;
            }

            for (Method m : getClass().getMethods()) {
                BooleanReplacement br = m.getAnnotation(BooleanReplacement.class);
                if (br == null) {
                    continue;
                }
                if (!call.name.equals(m.getName())) {
                    continue;
                }

                /*
                    Not going to look at the full method descriptor,
                    as return types in our replacements are going to
                    be different (eg from boolean to int)
                 */
                String paramDesc = getParameterDescriptor(m, false);
                String paramReduced = getParameterDescriptor(m, true);

                if ((br.replacingStatic() && call.desc.startsWith(paramDesc)) ||
                        (!br.replacingStatic() && call.desc.startsWith(paramReduced))) {

                    MethodInsnNode replacingCall = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(getClass()),
                            m.getName(),
                            Type.getMethodDescriptor(m),
                            false);
                    mn.instructions.insertBefore(node, replacingCall);
                    mn.instructions.remove(node);

                    changed = true;
                    break;
                }
            }
        }

        return changed;
    }

    private static String getParameterDescriptor(Method m, boolean reduced) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');

        int start = 0;
        if (reduced) {
            //skipping first parameter
            start = 1;
        }

        for (int i = start; i < parameters.length; ++i) {
            buf.append(Type.getDescriptor(parameters[i]));
        }
        buf.append(')');

        return buf.toString();
    }


    private boolean isReplaceMethod(AbstractInsnNode node) {
        if (node == null) {
            return false;
        }
        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
            return methodInsnNode.owner.equals(Type.getInternalName(getClass()));
        }
        return false;
    }


    /**
     * a positive, non-0 value is considered a boolean "true"
     *
     * @param x
     * @return
     */
    public static boolean convertIntToBoolean(int x) {
        return x > 0;
    }
}
