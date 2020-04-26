package org.evomaster.client.java.instrumentation.tracker;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.Constants;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Instrumentation done to keep track of when some specific methods
 * are called
 */
@Deprecated
public class TrackerClassVisitor extends ClassVisitor {

    private final String bytecodeClassName;


    public TrackerClassVisitor(ClassVisitor classVisitor, ClassName className) {
        super(Constants.ASM, classVisitor);
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
            No point in looking at static initializers.
         */
        if (name.equals(Constants.CLASS_INIT_METHOD)) {
            return mv;
        }

        mv = new TrackerMethodVisitor(mv, bytecodeClassName, name, descriptor);

        return mv;
    }

}
