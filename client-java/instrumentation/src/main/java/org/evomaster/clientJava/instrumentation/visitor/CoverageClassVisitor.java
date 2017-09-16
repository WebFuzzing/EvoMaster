package org.evomaster.clientJava.instrumentation.visitor;

import org.evomaster.clientJava.instrumentation.ClassName;
import org.evomaster.clientJava.instrumentation.Constants;
import org.evomaster.clientJava.instrumentation.ObjectiveNaming;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Add instrumentations to keep track of which statements/lines
 * are covered during test execution
 */
public class CoverageClassVisitor extends ClassVisitor {

    private final String bytecodeClassName;

    public CoverageClassVisitor(ClassVisitor cv, ClassName className) {
        super(Constants.ASM, cv);
        bytecodeClassName = className.getBytecodeName();
    }

    @Override
    public MethodVisitor visitMethod(int methodAccess,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {

        MethodVisitor mv = super.visitMethod(
                methodAccess, name, descriptor, signature, exceptions);

        /*
            Methods added by the compiler (eg synthetics and bridges) are
            not interesting, so we can just skip them. More info:

            https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html
            http://www.javaworld.com/article/2073578/java-s-synthetic-methods.html
         */
        if (Constants.isMethodSyntheticOrBridge(methodAccess)) {
            return mv;
        }

        /*
            No point in looking at the coverage of static initializers.
            Furthermore, if SUT is restarted in the same JVM, then those
            initializers might not be re-executed, as classes already loaded.
            In such cases, using them in coverage score would be very misleading,
            as only the first test executed would cover them
         */
        if (name.equals(Constants.CLASS_INIT_METHOD)) {
            return mv;
        }

        ObjectiveRecorder.registerTarget(ObjectiveNaming.classObjectiveName(bytecodeClassName));

        mv = new LineCovMethodVisitor(mv, bytecodeClassName, name, descriptor);
        mv = new BranchCovMethodVisitor(mv, bytecodeClassName, name, descriptor);
        mv = new SuccessCallMethodVisitor(mv, bytecodeClassName, name, descriptor);

        return mv;
    }
}
