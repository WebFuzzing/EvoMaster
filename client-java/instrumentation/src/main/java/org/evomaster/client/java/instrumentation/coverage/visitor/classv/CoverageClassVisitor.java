package org.evomaster.client.java.instrumentation.coverage.visitor.classv;

import org.evomaster.client.java.instrumentation.coverage.visitor.methodv.*;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * Add instrumentations to keep track of which statements/lines
 * are covered during test execution
 */
public class CoverageClassVisitor extends ClassVisitor {

    private final String bytecodeClassName;

    public CoverageClassVisitor(ClassVisitor cv, ClassName className) {
        super(Constants.ASM, cv);
        bytecodeClassName = className.getBytecodeName();

        UnitsInfoRecorder.markNewUnit(className.getFullNameWithDots());
    }

    @Override
    public MethodVisitor visitMethod(int methodAccess,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {

        MethodVisitor mv = super.visitMethod(
                methodAccess, name, descriptor, signature, exceptions);

        mv = new JSRInlinerAdapter(mv, methodAccess, name, descriptor, signature, exceptions);

        /*
            Methods added by the compiler (eg synthetics and bridges) are
            not interesting, so we can just skip them. More info:

            https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html
            http://www.javaworld.com/article/2073578/java-s-synthetic-methods.html

            Actually, although there are several cases in which synthetic are not useful, we cannot skip them.
            Reason is that lambda expressions (eg used in stream operations) are converted into
            synthetic methods
         */
        if (Constants.isMethodBridge(methodAccess)) {
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

        if(bytecodeClassName.contains("$InterceptedDefinition") ||
                bytecodeClassName.contains("$Definition") ||
                bytecodeClassName.contains("$Introspection")){
            /*
                In general, we should avoid dealing with classes generated on the fly by frameworks like
                Hibernate or Micronaut. But not simple to identify. These classes here are particularly
                troublesome, especially for patio-api. it does not impact performance of EM, but mess up
                experiments.
             */
            return mv;
        }

        ObjectiveRecorder.registerTarget(ObjectiveNaming.classObjectiveName(bytecodeClassName));

        mv = new ScheduledMethodVisitor(mv);
        mv = new LineCovMethodVisitor(mv, bytecodeClassName, name, descriptor);
        mv = new BranchCovMethodVisitor(mv, bytecodeClassName, name, descriptor);
        mv = new SuccessCallMethodVisitor(mv, bytecodeClassName, name, descriptor);
        mv = new MethodReplacementMethodVisitor(true, true, mv, bytecodeClassName, name, descriptor);
        mv = new NonIntegerComparisonsMethodVisitor(mv, bytecodeClassName, name, descriptor);

        return mv;
    }
}
