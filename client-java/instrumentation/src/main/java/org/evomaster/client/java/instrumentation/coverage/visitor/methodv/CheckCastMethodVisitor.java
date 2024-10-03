package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CheckCastMethodVisitor extends MethodVisitor {

    public CheckCastMethodVisitor(MethodVisitor mv) {
        super(Constants.ASM, mv);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.CHECKCAST) {
            this.visitLdcInsn(type);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    ClassName.get(ExecutionTracer.class).getBytecodeName(),
                    ExecutionTracer.EXECUTING_CHECKCAST_METHOD_NAME,
                    ExecutionTracer.EXECUTING_CHECKCAST_DESCRIPTOR,
                    ExecutionTracer.class.isInterface());
        }
        super.visitTypeInsn(opcode, type);
    }
}
