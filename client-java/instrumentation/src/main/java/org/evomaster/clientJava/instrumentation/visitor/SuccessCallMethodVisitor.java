package org.evomaster.clientJava.instrumentation.visitor;

import org.evomaster.clientJava.instrumentation.ClassName;
import org.evomaster.clientJava.instrumentation.Constants;
import org.evomaster.clientJava.instrumentation.ObjectiveNaming;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Add test objectives to make sure methods are called without
 * throwing an exception.
 *
 * TODO: should also handle the cases of accessing arrays out
 * of bounds.
 */
public class SuccessCallMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;

    private int currentLine;
    private int currentIndex;

    public SuccessCallMethodVisitor(MethodVisitor mv,
                                String className,
                                String methodName,
                                String descriptor) {
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
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc, boolean itf) {

        //don't instrument static initializers
        if(methodName.equals(Constants.CLASS_INIT_METHOD)){
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        int index = currentIndex++;

        ObjectiveRecorder.registerTarget(
                ObjectiveNaming.successCallObjectiveName(className, currentLine, index));

        addInstrumentation(index, false);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        addInstrumentation(index, true);
    }


    private void addInstrumentation(int index, boolean covered){

        this.visitLdcInsn(className);
        this.visitLdcInsn(currentLine);
        this.visitLdcInsn(index);
        this.visitLdcInsn(covered);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.EXECUTING_METHOD_METHOD_NAME,
                ExecutionTracer.EXECUTING_METHOD_DESCRIPTOR,
                ExecutionTracer.class.isInterface());
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /*
            We pushed 4 values on stack before a method call,
            so we need to increase maxStack by at least 4
         */
        super.visitMaxs(maxStack + 4, maxLocals);
    }
}
