package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Minimal class adapter that wraps each method with CFGMethodAdapter to build graphs,
 * while delegating to the downstream visitor chain unchanged.
 */
public class CFGClassAdapter extends ClassVisitor {

    private final ClassLoader classLoader;
    private String classNameWithDots;

    public CFGClassAdapter(ClassLoader classLoader, ClassVisitor cv) {
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
        return new CFGMethodAdapter(
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


