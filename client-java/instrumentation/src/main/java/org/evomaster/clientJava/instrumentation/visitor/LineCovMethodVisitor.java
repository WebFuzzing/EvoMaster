package org.evomaster.clientJava.instrumentation.visitor;

import org.evomaster.clientJava.instrumentation.ClassName;
import org.evomaster.clientJava.instrumentation.Constants;
import org.evomaster.clientJava.instrumentation.ObjectiveNaming;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LineCovMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;


    public LineCovMethodVisitor(MethodVisitor mv,
                                String className,
                                String methodName,
                                String descriptor) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
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

            Here we push 2 elements on the stack, which
            are used to uniquely identify the line.
            Then, we do a call to ExecutionTracer that
            will pop these 2 elements as input parameters.
         */

        ObjectiveRecorder.registerTarget(ObjectiveNaming.lineObjectiveName(className, line));

        this.visitLdcInsn(className);
        this.visitLdcInsn(line);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.EXECUTED_LINE_METHOD_NAME,
                ExecutionTracer.EXECUTED_LINE_DESCRIPTOR,
                ExecutionTracer.class.isInterface()); //false
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /*
            we pushed 2 values on the stack, so we need to tell ASM
            that this instrumented method should have a frame for at
            least 2 elements.

            Note: as here we are instrumenting lines, it can be assumed
            that the frame is empty (isn't it?), and so we do
            Math.max(maxElementsAddedOnStackFrame, maxStack)
            instead of
            maxElementsAddedOnStackFrame +  maxStack
         */
        int maxElementsAddedOnStackFrame = 2;
        super.visitMaxs(Math.max(maxElementsAddedOnStackFrame, maxStack), maxLocals);
    }
}
