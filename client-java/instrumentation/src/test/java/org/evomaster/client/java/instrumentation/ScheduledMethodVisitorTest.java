package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.coverage.visitor.methodv.ScheduledMethodVisitor;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledMethodVisitorTest {

    private String previousCategories;

    @BeforeEach
    void isolateCategories() {
        previousCategories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,NET");
        UnitsInfoRecorder.reset();
    }

    @AfterEach
    void restoreCategories() {
        if (previousCategories == null) {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, previousCategories);
        }
        UnitsInfoRecorder.reset();
    }

    @Test
    void preservesSchedulingAnnotationsWhenEnabled() {
        String previous = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        try {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,NET,SCHEDULE");
            AnnotationVisitor annotation = new AnnotationVisitor(Constants.ASM) {};
            MethodVisitor delegate = new MethodVisitor(Constants.ASM) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return annotation;
                }
            };
            ScheduledMethodVisitor visitor = new ScheduledMethodVisitor(delegate, "example/Jobs", "run", "()V");
            assertSame(annotation, visitor.visitAnnotation("Lorg/springframework/scheduling/annotation/Scheduled;", true));
            assertSame(annotation, visitor.visitAnnotation("Lorg/springframework/scheduling/annotation/Schedules;", true));
            assertSame(annotation, visitor.visitAnnotation("Ljavax/ejb/Schedule;", true));
            assertTrue(UnitsInfoRecorder.getInstance().getScheduledMethods().contains("example.Jobs#run()V"));
        } finally {
            if (previous == null) {
                System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
            } else {
                System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, previous);
            }
            UnitsInfoRecorder.reset();
        }
    }

    @Test
    void recordsSpringAnnotationsWithoutExecutingMethods() {
        UnitsInfoRecorder.reset();
        ScheduledMethodVisitor visitor = new ScheduledMethodVisitor(null, "example/Jobs", "run", "()V");
        assertNull(visitor.visitAnnotation("Lorg/springframework/scheduling/annotation/Scheduled;", true));
        assertNull(visitor.visitAnnotation("Lorg/springframework/scheduling/annotation/Schedules;", true));
        assertEquals(1, UnitsInfoRecorder.getInstance().getScheduledMethods().size());
        assertTrue(UnitsInfoRecorder.getInstance().getScheduledMethods().contains("example.Jobs#run()V"));

        ExecutionTracer.reset();
        assertEquals(1, UnitsInfoRecorder.getInstance().getScheduledMethods().size());
        assertThrows(UnsupportedOperationException.class,
                () -> UnitsInfoRecorder.getInstance().getScheduledMethods().clear());
        UnitsInfoRecorder.reset();
        assertTrue(UnitsInfoRecorder.getInstance().getScheduledMethods().isEmpty());
    }

    @Test
    void forwardsOtherAnnotationsAndDistinguishesOverloads() {
        UnitsInfoRecorder.reset();
        AnnotationVisitor annotation = new AnnotationVisitor(Constants.ASM) {};
        MethodVisitor delegate = new MethodVisitor(Constants.ASM) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return annotation;
            }
        };
        ScheduledMethodVisitor visitor = new ScheduledMethodVisitor(delegate, "example/Jobs", "run", "()V");
        assertSame(annotation, visitor.visitAnnotation("Ljava/lang/Deprecated;", true));
        assertNull(visitor.visitAnnotation("Ljavax/ejb/Schedule;", true));
        assertTrue(UnitsInfoRecorder.getInstance().getScheduledMethods().isEmpty());
        visitor.visitAnnotation("Lorg/springframework/scheduling/annotation/Scheduled;", true);
        new ScheduledMethodVisitor(delegate, "example/Jobs", "run", "(I)V")
                .visitAnnotation("Lorg/springframework/scheduling/annotation/Scheduled;", true);
        assertEquals(2, UnitsInfoRecorder.getInstance().getScheduledMethods().size());
        UnitsInfoRecorder.reset();
    }
}
