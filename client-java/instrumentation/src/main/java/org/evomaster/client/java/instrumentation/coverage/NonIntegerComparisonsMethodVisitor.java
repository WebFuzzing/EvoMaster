package org.evomaster.client.java.instrumentation.coverage;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.coverage.noninteger.NonIntegerComparisons;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by arcuri82 on 02-Mar-20.
 */
public class NonIntegerComparisonsMethodVisitor extends MethodVisitor {

    private static final Set<Integer> codes = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    Opcodes.LCMP,
                    Opcodes.DCMPG,
                    Opcodes.DCMPL,
                    Opcodes.FCMPG,
                    Opcodes.FCMPL
            ))
    );


    private final String className;
    private final String methodName;

    private int currentLine;
    private int currentIndex;

    public NonIntegerComparisonsMethodVisitor(
            MethodVisitor mv,
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
    public void visitInsn(final int opcode) {

        if (!codes.contains(opcode)) {
            super.visitInsn(opcode);
            return;
        }

        String id = className + "_" + currentLine + "_" + currentIndex;
        currentIndex++;

        /*
            we push the id on stack, and then replace the instruction with a method call
            to our own API
         */
        this.visitLdcInsn(id);

        String name;
        String descriptor;

        if (opcode == Opcodes.LCMP) {
            name = "replaceLCMP";
            //recall that in bytecode "long" uses symbol "J"
            descriptor = "(JJLjava/lang/String;)I";
        } else if (opcode == Opcodes.DCMPG) {
            name = "replaceDCMPG";
            descriptor = "(DDLjava/lang/String;)I";
        } else if (opcode == Opcodes.DCMPL) {
            name = "replaceDCMPL";
            descriptor = "(DDLjava/lang/String;)I";
        } else if (opcode == Opcodes.FCMPG) {
            name = "replaceFCMPG";
            descriptor = "(FFLjava/lang/String;)I";
        } else if (opcode == Opcodes.FCMPL) {
            name = "replaceFCMPG";
            descriptor = "(FFLjava/lang/String;)I";
        } else {
            throw new IllegalStateException("BUG: unrecognized code " + opcode);
        }


        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ClassName.get(NonIntegerComparisons.class).getBytecodeName(),
                name,
                descriptor,
                NonIntegerComparisons.class.isInterface()); //false
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /*
            We pushed 1 new value on stack before a method call,
            so we need to increase maxStack by at least 1
         */
        super.visitMaxs(maxStack + 1, maxLocals);
    }
}
