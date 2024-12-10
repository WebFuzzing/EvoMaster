package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.Constants;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

public class ScheduledMethodVisitor extends MethodVisitor {


    public ScheduledMethodVisitor(MethodVisitor methodVisitor) {
        super(Constants.ASM, methodVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible){

        if(descriptor.equals("Lorg/springframework/scheduling/annotation/Scheduled;")
                || descriptor.equals("Ljavax/ejb/Schedule;")){
            /*
                TODO for now we just skip them... might want to do something with them though...
             */
            return null;
        }

        return super.visitAnnotation(descriptor, visible);
    }
}
