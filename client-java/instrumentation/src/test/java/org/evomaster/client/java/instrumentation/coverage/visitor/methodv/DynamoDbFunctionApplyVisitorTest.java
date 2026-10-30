package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that only enhanced-client operation execution rewrites Function.apply.
 */
public class DynamoDbFunctionApplyVisitorTest {

    private static final String FUNCTION = "java/util/function/Function";
    private static final String APPLY_METHOD = "apply";
    private static final String APPLY = "(Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String REPLACEMENT_CATEGORIES = "BASE,DYNAMODB";
    private static final String BASE_CATEGORY = "BASE";
    private static final String OPERATION_PACKAGE =
            "software/amazon/awssdk/enhanced/dynamodb/internal/operations/";
    private static final String REPLACEMENT_OWNER =
            "org/evomaster/client/java/instrumentation/coverage/methodreplacement/classes/DynamoDbFunctionClassReplacement";
    private static final String REPLACEMENT_DESCRIPTOR =
            "(Ljava/util/function/Function;Ljava/lang/Object;)Ljava/lang/Object;";
    private String previousCategories;

    /** Enables the DynamoDB replacement category. */
    @BeforeEach
    public void enableDynamoDb() {
        previousCategories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, REPLACEMENT_CATEGORIES);
    }

    /** Restores the surrounding test process configuration. */
    @AfterEach
    public void restoreCategories() {
        if (previousCategories == null) {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, previousCategories);
        }
    }

    /** Both single-table and database operations must reach the replacement. */
    @Test
    public void testRewriteEnhancedOperationApplyCalls() {
        assertReplacement(OPERATION_PACKAGE + "CommonOperation", true);
        assertReplacement(OPERATION_PACKAGE + "DatabaseOperation", true);
        assertReplacement("com/foo/worldcup/ProductRepository", false);
        assertReplacement(OPERATION_PACKAGE + "PaginatedOperation", false);
    }

    /** Disabling DynamoDB tracking leaves the SDK invocation unchanged. */
    @Test
    public void testRespectReplacementCategory() {
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, BASE_CATEGORY);
        assertReplacement(OPERATION_PACKAGE + "CommonOperation", false);
    }

    /** Applies the visitor to a representative Function.apply call. */
    private void assertReplacement(String caller, boolean expected) {
        RecordingMethodVisitor recording = new RecordingMethodVisitor();
        MethodReplacementMethodVisitor visitor = new MethodReplacementMethodVisitor(
                false, false, recording, caller, "execute", "()V");
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, FUNCTION, APPLY_METHOD, APPLY, true);

        assertEquals(expected ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE, recording.opcode);
        assertEquals(expected
                        ? REPLACEMENT_OWNER
                        : FUNCTION,
                recording.owner);
        assertEquals(APPLY_METHOD, recording.name);
        assertEquals(expected
                        ? REPLACEMENT_DESCRIPTOR
                        : APPLY,
                recording.descriptor);
    }

    /** Captures the last invocation emitted by the visitor. */
    private static class RecordingMethodVisitor extends MethodVisitor {
        private int opcode;
        private String owner;
        private String name;
        private String descriptor;

        /** Creates a recorder using EvoMaster's ASM version. */
        private RecordingMethodVisitor() {
            super(Constants.ASM);
        }

        /** Stores an emitted method instruction. */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            this.opcode = opcode;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }
    }
}
