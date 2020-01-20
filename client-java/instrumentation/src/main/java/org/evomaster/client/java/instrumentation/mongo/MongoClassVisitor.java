package org.evomaster.client.java.instrumentation.mongo;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Instrumentation done to keep track of when MongoDB
 * methods are invoked
 */
public class MongoClassVisitor extends ClassVisitor {

    private final String bytecodeClassName;


    public MongoClassVisitor(ClassVisitor classVisitor, ClassName className) {
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

        mv = new MongoMethodVisitor(mv, bytecodeClassName, name, descriptor);

        return mv;
    }

}

