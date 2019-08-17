package org.evomaster.client.java.instrumentation.coverage;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.ClassName;
import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.deprecated_testabilityexception.ExceptionHeuristicsRegistry;
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

        String targetId = ObjectiveNaming.successCallObjectiveName(className, currentLine, index);

        ObjectiveRecorder.registerTarget(targetId);

        if(!ExceptionHeuristicsRegistry.shouldHandle(owner, name, desc)) {
            addBaseInstrumentation(index, false);
        } else {
            //special heuristics to avoid throwing exception
            addHeuristicInstrumentation(targetId, owner, name, desc);
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);
        addBaseInstrumentation(index, true);
    }

    private void addHeuristicInstrumentation(String targetId, String owner, String name, String desc){

        int inputs = ExceptionHeuristicsRegistry.numberOfInputs(owner, name, desc);
        if(inputs != 1){
            throw new IllegalStateException("Bug in code instrumentation: number of inputs is " + inputs);
        }

        /*
            need to duplicate the inputs of the target method, as we will consume them
            before the method is called

            TODO: if input is a primitive (eg "int"), likely ll need instruction to cast it to a wrapper
         */

        if(inputs == 1){
            this.visitInsn(Opcodes.DUP);
        } else if(inputs == 2){
            this.visitInsn(Opcodes.DUP2);
        } else {
            //TODO: there is no native support for duplicate more than 2 elements
        }

        this.visitLdcInsn(targetId);
        this.visitLdcInsn(owner);
        this.visitLdcInsn(name);
        this.visitLdcInsn(desc);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(ExecutionTracer.class).getBytecodeName(),
                ExecutionTracer.EXECUTING_EXCEPTION_METHOD_METHOD_NAME,
                ExecutionTracer.EXECUTING_EXCEPTION_METHOD_DESCRIPTOR_1,
                ExecutionTracer.class.isInterface());
    }

    private void addBaseInstrumentation(int index, boolean covered){

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
