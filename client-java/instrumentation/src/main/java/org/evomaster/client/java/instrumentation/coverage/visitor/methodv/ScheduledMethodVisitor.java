package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

public class ScheduledMethodVisitor extends MethodVisitor {


    private final String methodId;
    private final boolean enableSchedule;

    public ScheduledMethodVisitor(MethodVisitor methodVisitor, String className, String methodName, String methodDescriptor) {
        super(Constants.ASM, methodVisitor);
        methodId = className.replace('/', '.') + "#" + methodName + methodDescriptor;
        enableSchedule = Arrays.stream(System.getProperty(InputProperties.REPLACEMENT_CATEGORIES, "").split(","))
                .anyMatch(ReplacementCategory.SCHEDULE.toString()::equals);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible){

        if(descriptor.equals("Lorg/springframework/scheduling/annotation/Scheduled;")
                || descriptor.equals("Lorg/springframework/scheduling/annotation/Schedules;")){
            UnitsInfoRecorder.registerScheduledMethod(methodId);
            return enableSchedule ? super.visitAnnotation(descriptor, visible) : null;
        }

        if(descriptor.equals("Ljavax/ejb/Schedule;")){
            return enableSchedule ? super.visitAnnotation(descriptor, visible) : null;
        }

        return super.visitAnnotation(descriptor, visible);
    }
}
