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

        while (true) {

            if (node.getOpcode() == Opcodes.IFNE) {
                handleIFNE(node);
            } else if (node.getOpcode() == Opcodes.IFEQ) {
                handleIFEQ(node);
            } else if (node.getOpcode() == Opcodes.IF_ICMPEQ) {
                handleIF_ICMPEQ(mn, node);
            } else if (node.getOpcode() == Opcodes.IF_ICMPNE) {
                handleIF_ICMPNE(mn, node);
            } else if (node.getOpcode() == Opcodes.IRETURN) {
                handleIRETURN(mn, node);
            }

            if (node == mn.instructions.getLast()) {
                break;
            }

            node = node.getNext();
        }

        return true;
    }

    //TODO write explanation

    private void handleIRETURN(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IRETURN;
        if (isReplaceMethod(node.getPrevious())) {
            MethodInsnNode n = new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(BooleanMethodTransformer.class),
                    "convertIntToBoolean",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE,
                            new Type[]{Type.INT_TYPE}), false);

            mn.instructions.insertBefore(node, n);
        }
    }

    private void handleIF_ICMPNE(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IF_ICMPNE;
        JumpInsnNode branch = (JumpInsnNode) node;
        if (isReplaceMethod(node.getPrevious().getPrevious())) {
            if (node.getPrevious().getOpcode() == Opcodes.ICONST_0) {
                branch.setOpcode(Opcodes.IFGT);
                mn.instructions.remove(node.getPrevious());
            } else if (node.getPrevious().getOpcode() == Opcodes.ICONST_1) {
                branch.setOpcode(Opcodes.IFLE);
                mn.instructions.remove(node.getPrevious());
            }
        }
    }

    private void handleIF_ICMPEQ(MethodNode mn, AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IF_ICMPEQ;
        JumpInsnNode branch = (JumpInsnNode) node;
        if (isReplaceMethod(node.getPrevious().getPrevious())) {
            if (node.getPrevious().getOpcode() == Opcodes.ICONST_0) {
                branch.setOpcode(Opcodes.IFLE);
                mn.instructions.remove(node.getPrevious());
            } else if (node.getPrevious().getOpcode() == Opcodes.ICONST_1) {
                branch.setOpcode(Opcodes.IFGT);
                mn.instructions.remove(node.getPrevious());
            }
        }
    }

    private void handleIFEQ(AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IFEQ;
        JumpInsnNode branch = (JumpInsnNode) node;
        if (isReplaceMethod(node.getPrevious())) {
            branch.setOpcode(Opcodes.IFLE);
        }
    }

    private void handleIFNE(AbstractInsnNode node) {
        assert node.getOpcode() == Opcodes.IFNE;
        JumpInsnNode branch = (JumpInsnNode) node;
        if (isReplaceMethod(node.getPrevious())) {
            branch.setOpcode(Opcodes.IFGT);
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

            String replacementClass = Type.getInternalName(getClass());

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

                String desc = Type.getMethodDescriptor(m);
                String reduced = getReducedMethodDescriptor(m);

                if ((br.replacingStatic() && call.desc.equals(desc)) ||
                        (!br.replacingStatic() && call.desc.equals(reduced))) {

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

    private static String getReducedMethodDescriptor(final Method m) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        //skipping first parameter
        for (int i = 1; i < parameters.length; ++i) {
            buf.append(Type.getDescriptor(parameters[i]));
        }
        buf.append(')');
        buf.append(Type.getDescriptor(m.getReturnType()));
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
