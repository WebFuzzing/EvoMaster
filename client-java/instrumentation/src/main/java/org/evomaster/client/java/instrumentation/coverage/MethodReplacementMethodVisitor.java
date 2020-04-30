package org.evomaster.client.java.instrumentation.coverage;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ReplacementList;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class MethodReplacementMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;
    private final boolean registerNewTargets;
    private final boolean applyTrackingMethods;

    private int currentLine;
    private int currentIndex;

    public MethodReplacementMethodVisitor(boolean registerNewTargets,
                                          boolean applyTrackingMethods,
                                          MethodVisitor mv,
                                          String className,
                                          String methodName,
                                          String descriptor) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
        this.registerNewTargets = registerNewTargets;
        this.applyTrackingMethods = applyTrackingMethods;
        currentLine = 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);

        currentLine = line;
        currentIndex = 0; //reset it for current line
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc, boolean itf) {

        //don't instrument static initializers
        if (methodName.equals(Constants.CLASS_INIT_METHOD)) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }


        /*
         * This is a very special case. This can happen when we replace a method
         * for X.foo() in a class Y, when Y extends X, and then it calls foo in
         * the superclass X with super.foo()
         * As it is now, this would lead to an infinite recursion in the replaced
         * method if we call foo() there (as it would be executed on the instance of Y,
         * not the super method in X).
         * Given an instance of Y, it is not possible to directly call X.foo() from outside Y.
         * TODO: Maybe there can be a general solution for this, but, considering it is likely rare,
         * we can do it for later. Eg, it would mainly affect the use of special containers like
         * in Guava when they have "super.()" calls.
         * For now, we just skip them.
         */
        if (opcode == Opcodes.INVOKESPECIAL) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }


//        if (!owner.startsWith("java/")) {
//            super.visitMethodInsn(opcode, owner, name, desc, itf);
//            return;
//        }
//
//        /*
//            Loading class here could have side-effects if code is executed in static initializer.
//         */
//        Class<?> klass = null;
//        try {
//            klass = this.getClass().getClassLoader().loadClass(ClassName.get(owner).getFullNameWithDots());
//        } catch (ClassNotFoundException e) {
//            //shouldn't really happen
//            SimpleLogger.error(e.toString());
//            throw new RuntimeException(e);
//        }

        List<MethodReplacementClass> candidateClasses = ReplacementList.getReplacements(owner);

        if (candidateClasses.isEmpty()) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        Optional<Method> r = candidateClasses.stream()
                .flatMap(i -> Stream.of(i.getClass().getDeclaredMethods()))
                .filter(m -> m.getDeclaredAnnotation(Replacement.class) != null)
                .filter(m -> m.getName().equals(name))
                .filter(m -> {
                    Replacement br = m.getAnnotation(Replacement.class);
                    if(!applyTrackingMethods && br.type() == ReplacementType.TRACKER){
                        return false;
                    }

                    int skipFirst = br.replacingStatic() ? 0 : 1;
                    int skipLast = br.type() == ReplacementType.TRACKER ? 0 : 1;
                    return desc.equals(getDescriptor(m, skipFirst, skipLast));
                })
                .findAny();

        if (!r.isPresent()) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        Method m = r.get();
        replaceMethod(m);
        Replacement a = m.getAnnotation(Replacement.class);

        if (a.type() == ReplacementType.TRACKER) {
            UnitsInfoRecorder.markNewTrackedMethod();
        } else {
            if (registerNewTargets) {
                UnitsInfoRecorder.markNewReplacedMethodInSut();
            } else {
                UnitsInfoRecorder.markNewReplacedMethodInThirdParty();
            }
        }
    }

    private void replaceMethod(Method m) {

        Replacement br = m.getAnnotation(Replacement.class);

        /*
                    In the case of replacing a non-static method a.foo(x,y),
                    we will need a replacement bar(a,x,y,id)

                    So, the stack
                    a
                    x
                    y
                    foo # non-static

                    will be replaced by
                    a
                    x
                    y
                    id
                    bar # static

                    This means we do not need to handle "a", but still need to create
                    "id" and replace "foo" with "bar".
        */
        if(br.type() != ReplacementType.TRACKER) {
            //tracker methods do not add a template id

            if (registerNewTargets) {
                String idTemplate = ObjectiveNaming.methodReplacementObjectiveNameTemplate(
                        className, currentLine, currentIndex
                );

                currentIndex++;

                String idTrue = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, true, br.type());
                String idFalse = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, false, br.type());
                ObjectiveRecorder.registerTarget(idTrue);
                ObjectiveRecorder.registerTarget(idFalse);

                this.visitLdcInsn(idTemplate);

            } else {
                //this.visitLdcInsn(null);
                this.visitInsn(Opcodes.ACONST_NULL);
            }
        }

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(m.getDeclaringClass()),
                m.getName(),
                Type.getMethodDescriptor(m),
                false);
    }


    private static String getDescriptor(Method m, int skipFirsts, int skipLast) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');

        //skipping first parameter(s)
        int start = skipFirsts;
        int end = parameters.length - skipLast;

        /*
            we might skip the first (if replacing non-static), and
            skipping the last (id template)
         */
        for (int i = start; i < end; ++i) {
            buf.append(Type.getDescriptor(parameters[i]));
        }
        buf.append(')');
        buf.append(Type.getDescriptor(m.getReturnType()));

        return buf.toString();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /*
            We pushed 1 value on stack before a method call,
            so we need to increase maxStack by at least 1
         */
        super.visitMaxs(maxStack + 1, maxLocals);
    }
}
