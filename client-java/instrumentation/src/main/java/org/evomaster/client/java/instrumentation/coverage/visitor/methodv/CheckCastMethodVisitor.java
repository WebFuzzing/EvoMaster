package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CheckCastMethodVisitor extends MethodVisitor {


    private final String className;
    private final String methodName;

    private int currentLine;
    private int currentIndex;

    public CheckCastMethodVisitor(MethodVisitor mv,
                                  String className,
                                  String methodName) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
        currentLine = 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);

        currentLine = line;
        currentIndex = 0; //reset it for current line
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {

        //don't instrument static initializers
        if(methodName.equals(Constants.CLASS_INIT_METHOD) || opcode != Opcodes.CHECKCAST){
            super.visitTypeInsn(opcode, type);
            return;
        }

        int index = currentIndex++;

        String targetId = ObjectiveNaming.checkcastObjectiveName(className, currentLine, index);
        ObjectiveRecorder.registerTarget(targetId);

        this.visitLdcInsn(type);
        this.visitLdcInsn(className);
        this.visitLdcInsn(currentLine);
        this.visitLdcInsn(index);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.EXECUTING_CHECKCAST_METHOD_NAME,
                ExecutionTracer.EXECUTING_CHECKCAST_DESCRIPTOR,
                ExecutionTracer.class.isInterface());

        super.visitTypeInsn(opcode, type);

        this.visitInsn(Opcodes.DUP);
        this.visitLdcInsn(className);
        this.visitLdcInsn(currentLine);
        this.visitLdcInsn(index);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.EXECUTED_CHECKCAST_METHOD_NAME,
                ExecutionTracer.EXECUTED_CHECKCAST_DESCRIPTOR,
                ExecutionTracer.class.isInterface());
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        int maxElementsAddedOnStackFrame = 5;
        super.visitMaxs(Math.max(maxElementsAddedOnStackFrame, maxStack), maxLocals);
    }
}
