package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MethodReplacementMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;
    private final boolean registerNewTargets;
    private final boolean isInSUT;

    private int currentLine;
    private int currentIndex;

    public MethodReplacementMethodVisitor(boolean registerNewTargets,
                                          boolean isInSUT,
                                          MethodVisitor mv,
                                          String className,
                                          String methodName,
                                          String descriptor) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
        this.registerNewTargets = registerNewTargets;
        this.isInSUT = isInSUT;
        currentLine = 0;
    }


    /*
        Replacing a method is relatively simple... what is tricky is replacing constructors.
        A call like

        new URL(s)

        would generate:

         NEW java/net/URL
         DUP
         ALOAD 1
         INVOKESPECIAL java/net/URL.<init> (Ljava/lang/String;)V
         POP

         the actual creation is done in 2 parts: the instantiation with NEW, and then the actual constructor
         call with <init>.
         The object seems duplicated with DUP, as it is popped by the INVOKESPECIAL.

         So, an approach here would be to delete the NEW, delete the first following DUP, and then replace the
         INVOKESPECIAL with our replacement that return an instance of the object.

         One issue though is that we might not replace all constructors for a given class, based on signature.
         The signature is available on INVOKESPECIAL, but not on NEW.

         Maybe an alternative approach would be for replacement method to get as inputs both instances, ie
         the one created by NEW and the one created by DUP.
         But that does not work, as "uninitialized" objects fail the bytecode verifier when given as input
         to a method :(

         In the end, we did the following:
         - replaced INVOKESPECIAL with a call that does NOT return instance
         - POP2 (to remove NEW and DUP refs from stack)
         - call to consumeInstance() (which push instance from previous call onto stack)
     */


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
         *
         * Still, we do instrument constructors, which uses INVOKESPECIAL
         */
        if (opcode == Opcodes.INVOKESPECIAL && !name.equals(Constants.INIT_METHOD)) {
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

        boolean isConstructor = name.equals(Constants.INIT_METHOD);

        List<MethodReplacementClass> candidateClasses = ReplacementList.getReplacements(owner, isConstructor);

        if (candidateClasses.isEmpty()) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        Optional<Method> r = ReplacementUtils.chooseMethodFromCandidateReplacement(
                isInSUT, name, desc, candidateClasses, false, className);

        if (!r.isPresent()) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        handleLastCallerClass();

        Method m = r.get();
        replaceMethod(m);
        if(isConstructor){
            handleConstruct(m, candidateClasses.get(0).getClass());
        }

        Replacement a = m.getAnnotation(Replacement.class);
        if (a.type() == ReplacementType.TRACKER) {
            UnitsInfoRecorder.markNewTrackedMethod();
        } else {
            if (isInSUT) {
                UnitsInfoRecorder.markNewReplacedMethodInSut();
            } else {
                UnitsInfoRecorder.markNewReplacedMethodInThirdParty();
            }
        }
    }

    private void handleLastCallerClass(){

        this.visitLdcInsn(className);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.SET_LAST_CALLER_CLASS_METHOD_NAME,
                ExecutionTracer.SET_LAST_CALLER_CLASS_DESC,
                ExecutionTracer.class.isInterface()); //false
    }

    private void handleConstruct(Method m, Class<? extends MethodReplacementClass> mrc){

        /*
            This seems working, but need to watch out for possible side-effects
         */
        this.visitInsn(Opcodes.POP2); //pop NEW and DUP refs

        if (!Arrays.stream(mrc.getDeclaredMethods())
                .anyMatch(it -> it.getName().equals(MethodReplacementClass.CONSUME_INSTANCE_METHOD_NAME))) {
            throw new RuntimeException("Class " + mrc.getName() + " must have a definition of method " + MethodReplacementClass.CONSUME_INSTANCE_METHOD_NAME);
        }

        Method consumeInstance = Arrays.stream(mrc.getDeclaredMethods())
                        .filter(it -> it.getName().equals(MethodReplacementClass.CONSUME_INSTANCE_METHOD_NAME))
                                .findFirst().get();

        //call method to retrieve the instance saved inside the replacement class
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(consumeInstance.getDeclaringClass()),
                consumeInstance.getName(),
                Type.getMethodDescriptor(consumeInstance),
                false);

        Replacement br = m.getAnnotation(Replacement.class);

        assert br.replacingConstructor();
        if(!br.castTo().isEmpty()){
            mv.visitTypeInsn(Opcodes.CHECKCAST, ClassName.get(br.castTo()).getBytecodeName());
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
                this.visitInsn(Opcodes.ACONST_NULL);
            }
        }

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(m.getDeclaringClass()),
                m.getName(),
                Type.getMethodDescriptor(m),
                false);

        if(!br.castTo().isEmpty() && !br.replacingConstructor()){
            mv.visitTypeInsn(Opcodes.CHECKCAST, ClassName.get(br.castTo()).getBytecodeName());
        }
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
