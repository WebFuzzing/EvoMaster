/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster.
 */
package org.evomaster.client.java.instrumentation.graphs.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Minimal class visitor that wraps each method with CFGMethodVisitor to build graphs,
 * while delegating to the downstream visitor chain unchanged.
 */
public class CFGClassVisitor extends ClassVisitor {

    private final ClassLoader classLoader;
    private String classNameWithDots;

    public CFGClassVisitor(ClassLoader classLoader, ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
        this.classLoader = classLoader;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classNameWithDots = name == null ? null : name.replace('/', '.');
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor downstream = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new CFGMethodVisitor(
                classLoader,
                classNameWithDots,
                access,
                name,
                descriptor,
                signature,
                exceptions,
                downstream
        );
    }
}


