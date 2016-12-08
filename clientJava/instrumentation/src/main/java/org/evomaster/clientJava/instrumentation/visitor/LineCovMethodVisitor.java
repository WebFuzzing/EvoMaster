package org.evomaster.clientJava.instrumentation.visitor;

import org.evomaster.clientJava.instrumentation.ClassName;
import org.evomaster.clientJava.instrumentation.Constants;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LineCovMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;

    /**
        method name + descriptor
        <br>
        The descriptor defines input/output types
     */
    private final String fullMethodName;


    public LineCovMethodVisitor(MethodVisitor mv,
                                String className,
                                String methodName,
                                String descriptor) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
        this.fullMethodName = methodName + descriptor;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);

        if(methodName.equals(Constants.CLASS_INIT_METHOD)){
            return;
        }

        /*
            After a line, we add our instrumentation to record
            that the line has been just passed/executed.

            Here we push 3 elements on the stack, which
            are used to uniquely identify the line.
            Then, we do a call to ExecutionTracer that
            will pull these 3 elements as input parameters.
         */

        this.visitLdcInsn(className);
        this.visitLdcInsn(fullMethodName);
        this.visitLdcInsn(line);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.EXECUTED_LINE_METHOD_NAME,
                ExecutionTracer.EXECUTED_LINE_DESCRIPTOR,
                false); //ExecutionTracer is not an interface
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        int addedInstructions = 3;
        super.visitMaxs(Math.max(addedInstructions, maxStack), maxLocals);
    }
}
