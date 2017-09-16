package org.evomaster.clientJava.instrumentation.visitor;

import org.evomaster.clientJava.instrumentation.ClassName;
import org.evomaster.clientJava.instrumentation.Constants;
import org.evomaster.clientJava.instrumentation.ObjectiveNaming;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BranchCovMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;

    private int latestVisitLine;
    private int jumpSinceLastLine;

    public BranchCovMethodVisitor(MethodVisitor mv,
                                  String className,
                                  String methodName,
                                  String descriptor) {
        super(Constants.ASM, mv);
        this.className = className;
        this.methodName = methodName;
        latestVisitLine = 0;
        jumpSinceLastLine = 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        latestVisitLine = line;
        jumpSinceLastLine = 0;
    }


    @Override
    public void visitJumpInsn(int opcode, Label label) {

        if (methodName.equals(Constants.CLASS_INIT_METHOD)) {
            super.visitJumpInsn(opcode, label);
            return;
        }

        int branchId = jumpSinceLastLine;
        jumpSinceLastLine++;

        /*
            Before we do the jump, we add instrumentation for
            branch coverage
         */

        ObjectiveRecorder.registerTarget(
                ObjectiveNaming.branchObjectiveName(className, latestVisitLine, branchId, true));
        ObjectiveRecorder.registerTarget(
                ObjectiveNaming.branchObjectiveName(className, latestVisitLine, branchId, false));

        switch (opcode) {
            //comparisons with 0
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
                this.visitInsn(Opcodes.DUP);
                this.visitLdcInsn(opcode);
                this.visitLdcInsn(className);
                this.visitLdcInsn(latestVisitLine);
                this.visitLdcInsn(branchId);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ClassName.get(ExecutionTracer.class).getBytecodeName(),
                        ExecutionTracer.EXECUTING_BRANCH_JUMP_METHOD_NAME,
                        ExecutionTracer.JUMP_DESC_1_VALUE,
                        ExecutionTracer.class.isInterface()); //false
                break;

            //comparisons of 2 values
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
                this.visitInsn(Opcodes.DUP2);
                this.visitLdcInsn(opcode);
                this.visitLdcInsn(className);
                this.visitLdcInsn(latestVisitLine);
                this.visitLdcInsn(branchId);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ClassName.get(ExecutionTracer.class).getBytecodeName(),
                        ExecutionTracer.EXECUTING_BRANCH_JUMP_METHOD_NAME,
                        ExecutionTracer.JUMP_DESC_2_VALUES,
                        ExecutionTracer.class.isInterface()); //false
                break;

            //object comparison
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
                this.visitInsn(Opcodes.DUP2);
                this.visitLdcInsn(opcode);
                this.visitLdcInsn(className);
                this.visitLdcInsn(latestVisitLine);
                this.visitLdcInsn(branchId);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ClassName.get(ExecutionTracer.class).getBytecodeName(),
                        ExecutionTracer.EXECUTING_BRANCH_JUMP_METHOD_NAME,
                        ExecutionTracer.JUMP_DESC_OBJECTS,
                        ExecutionTracer.class.isInterface()); //false
                break;

            //null comparison
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                this.visitInsn(Opcodes.DUP);
                this.visitLdcInsn(opcode);
                this.visitLdcInsn(className);
                this.visitLdcInsn(latestVisitLine);
                this.visitLdcInsn(branchId);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ClassName.get(ExecutionTracer.class).getBytecodeName(),
                        ExecutionTracer.EXECUTING_BRANCH_JUMP_METHOD_NAME,
                        ExecutionTracer.JUMP_DESC_NULL,
                        ExecutionTracer.class.isInterface()); //false
                break;
            default:
                /*
                    NOTE: not so interesting to handle switch statements (at
                    least for the moment...), as
                    quite complex and not so super common anyway...
                 */
        }



        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /*
            as we pushed up to 6 elements on stack in a position on which
            the stack might not be empty (and so potentially full of maxStack
            elements), we need to add them to the maxStack value
         */
        int maxElementsAddedOnStackFrame = 6;
        super.visitMaxs(maxElementsAddedOnStackFrame +  maxStack, maxLocals);
    }
}
