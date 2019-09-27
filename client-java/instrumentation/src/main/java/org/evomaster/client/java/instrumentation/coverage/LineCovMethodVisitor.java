package org.evomaster.client.java.instrumentation.coverage;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LineCovMethodVisitor extends MethodVisitor {


    private static final Set<Integer> returnCodes = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    Opcodes.IRETURN,
                    Opcodes.LRETURN,
                    Opcodes.FRETURN,
                    Opcodes.DRETURN,
                    Opcodes.ARETURN,
                    Opcodes.RETURN
            ))
    );


    private final String className;
    private final String methodName;
    private final String descriptor;

    private boolean seenAtLeastOneLine;

    public LineCovMethodVisitor(MethodVisitor mv,
                                String className,
                                String methodName,
                                String descriptor) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
        seenAtLeastOneLine = false;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);

        if (methodName.equals(Constants.CLASS_INIT_METHOD)) {
            return;
        }

        seenAtLeastOneLine = true;

        /*
            After a line, we add our instrumentation to record
            that the line has been just passed/executed.

            Here we push 4 elements on the stack, which
            are used to uniquely identify the line.
            Then, we do a call to ExecutionTracer that
            will pop these 4 elements as input parameters.
         */

        UnitsInfoRecorder.markNewLine();
        ObjectiveRecorder.registerTarget(ObjectiveNaming.lineObjectiveName(className, line));

        this.visitLdcInsn(className);
        this.visitLdcInsn(methodName);
        this.visitLdcInsn(descriptor);
        this.visitLdcInsn(line);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.EXECUTED_LINE_METHOD_NAME,
                ExecutionTracer.EXECUTED_LINE_DESCRIPTOR,
                ExecutionTracer.class.isInterface()); //false
    }



    public void visitInsn(final int opcode) {

        if (seenAtLeastOneLine && returnCodes.contains(opcode)){

            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    ClassName.get(ExecutionTracer.class).getBytecodeName(),
                    ExecutionTracer.COMPLETED_LAST_EXECUTED_STATEMENT_NAME,
                    ExecutionTracer.COMPLETED_LAST_EXECUTED_STATEMENT_DESCRIPTOR,
                    ExecutionTracer.class.isInterface()); //false
        }

        super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /*
            we pushed 4 values on the stack, so we need to tell ASM
            that this instrumented method should have a frame for at
            least 4 elements.

            Note: as here we are instrumenting lines, it can be assumed
            that the frame is empty (isn't it?), and so we do
            Math.max(maxElementsAddedOnStackFrame, maxStack)
            instead of
            maxElementsAddedOnStackFrame +  maxStack
         */
        int maxElementsAddedOnStackFrame = 4;
        super.visitMaxs(Math.max(maxElementsAddedOnStackFrame, maxStack), maxLocals);
    }
}
