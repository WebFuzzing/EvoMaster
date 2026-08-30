package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.DynamoDbClassReplacement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Bytecode-level tests for method-reference replacement in {@link MethodReplacementMethodVisitor}.
 */
public class MethodReplacementMethodVisitorTest {

    private static final String SYNC_CLIENT = "software/amazon/awssdk/services/dynamodb/DynamoDbClient";
    private static final String ASYNC_CLIENT = "software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient";
    private static final String MODEL = "software/amazon/awssdk/services/dynamodb/model/";
    private static final Handle METAFACTORY = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                    + "Ljava/lang/invoke/CallSite;",
            false);

    private String previousCategories;

    /**
     * Enables the DynamoDB and base replacement categories for each test.
     */
    @BeforeEach
    public void enableReplacementCategories() {
        previousCategories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,DYNAMODB");
    }

    /**
     * Restores the replacement categories after each test.
     */
    @AfterEach
    public void restoreReplacementCategories() {
        if (previousCategories == null) {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, previousCategories);
        }
    }

    /**
     * Verifies that all supported synchronous and asynchronous DynamoDB method-reference handles are rewritten.
     */
    @Test
    public void shouldRewriteSupportedDynamoDbMethodReferences() {
        List<String> operations = Arrays.asList(
                "GetItem", "BatchGetItem", "PutItem", "UpdateItem", "DeleteItem", "Query", "Scan");

        for (String operation : operations) {
            assertRewritten(SYNC_CLIENT, operation, MODEL + operation + "Response",
                    Type.getInternalName(DynamoDbClassReplacement.Sync.class));
            assertRewritten(ASYNC_CLIENT, operation, "java/util/concurrent/CompletableFuture",
                    Type.getInternalName(DynamoDbClassReplacement.Async.class));
        }
    }

    /**
     * Verifies that paginator, unrelated, and non-tracker handles remain unchanged.
     */
    @Test
    public void shouldKeepUnsupportedMethodReferencesUnchanged() {
        assertUnchanged(SYNC_CLIENT, "queryPaginator",
                "(L" + MODEL + "QueryRequest;)Lsoftware/amazon/awssdk/services/dynamodb/paginators/QueryIterable;");
        assertUnchanged(ASYNC_CLIENT, "scanPaginator",
                "(L" + MODEL + "ScanRequest;)Lsoftware/amazon/awssdk/services/dynamodb/paginators/ScanPublisher;");
        assertUnchanged(SYNC_CLIENT, "batchGetItemPaginator",
                "(L" + MODEL + "BatchGetItemRequest;)"
                        + "Lsoftware/amazon/awssdk/services/dynamodb/paginators/BatchGetItemIterable;");
        assertUnchanged("java/lang/Runnable", "run", "()V");
        assertUnchanged("java/lang/String", "equals", "(Ljava/lang/Object;)Z");
        assertIncompatibleUnchanged();
    }

    /**
     * Verifies that compatible trackers are not rewritten unless they explicitly opt in.
     */
    @Test
    public void shouldKeepNonOptInTrackerMethodReferencesUnchanged() {
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,DYNAMODB,SQL");

        assertUnchanged("java/sql/Statement", "executeQuery", "(Ljava/lang/String;)Ljava/sql/ResultSet;");
    }

    /**
     * Asserts that a supported low-level client handle is replaced with its tracker handle.
     *
     * @param owner original client owner
     * @param operation operation name with an initial uppercase character
     * @param responseOwner original response owner
     * @param replacementOwner expected replacement owner
     */
    private void assertRewritten(String owner, String operation, String responseOwner, String replacementOwner) {
        String methodName = Character.toLowerCase(operation.charAt(0)) + operation.substring(1);
        String requestOwner = MODEL + operation + "Request";
        Handle original = new Handle(Opcodes.H_INVOKEINTERFACE, owner, methodName,
                "(L" + requestOwner + ";)L" + responseOwner + ";", true);
        Type samType = Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;");
        Type instantiatedType = Type.getMethodType(original.getDesc());

        RecordingMethodVisitor recording = visit(owner, original, samType, instantiatedType);

        assertEquals("(Ljava/lang/Object;)Ljava/util/function/Function;", recording.descriptor);
        assertEquals(new Handle(Opcodes.H_INVOKESTATIC, replacementOwner, methodName,
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false), recording.arguments[1]);
        assertSame(samType, recording.arguments[0]);
        assertSame(instantiatedType, recording.arguments[2]);
    }

    /**
     * Asserts that an unsupported implementation handle and its invokedynamic descriptor remain unchanged.
     *
     * @param owner original handle owner
     * @param methodName original method name
     * @param methodDescriptor original method descriptor
     */
    private void assertUnchanged(String owner, String methodName, String methodDescriptor) {
        boolean isInterface = !"java/lang/String".equals(owner);
        int tag = isInterface ? Opcodes.H_INVOKEINTERFACE : Opcodes.H_INVOKEVIRTUAL;
        Handle original = new Handle(tag, owner, methodName, methodDescriptor, isInterface);
        Type samType = Type.getMethodType(methodDescriptor);
        Type instantiatedType = Type.getMethodType(methodDescriptor);

        RecordingMethodVisitor recording = visit(owner, original, samType, instantiatedType);

        assertEquals("(L" + owner + ";)Ljava/util/function/Function;", recording.descriptor);
        assertSame(original, recording.arguments[1]);
        assertSame(samType, recording.arguments[0]);
        assertSame(instantiatedType, recording.arguments[2]);
    }

    /**
     * Asserts that a registered target with an incompatible functional-interface arity remains unchanged.
     */
    private void assertIncompatibleUnchanged() {
        String descriptor = "(L" + MODEL + "GetItemRequest;)L" + MODEL + "GetItemResponse;";
        Handle original = new Handle(Opcodes.H_INVOKEINTERFACE, SYNC_CLIENT, "getItem", descriptor, true);
        Type incompatibleSamType = Type.getMethodType("()Ljava/lang/Object;");
        Type instantiatedType = Type.getMethodType(descriptor);

        RecordingMethodVisitor recording = visit(SYNC_CLIENT, original, incompatibleSamType, instantiatedType);

        assertEquals("(L" + SYNC_CLIENT + ";)Ljava/util/function/Function;", recording.descriptor);
        assertSame(original, recording.arguments[1]);
    }

    /**
     * Applies the visitor to a synthetic bound method reference.
     *
     * @param owner captured receiver owner
     * @param implementationHandle implementation method handle
     * @param samType erased functional-interface signature
     * @param instantiatedType instantiated functional-interface signature
     * @return recorded downstream invokedynamic instruction
     */
    private RecordingMethodVisitor visit(String owner, Handle implementationHandle, Type samType, Type instantiatedType) {
        RecordingMethodVisitor recording = new RecordingMethodVisitor();
        MethodReplacementMethodVisitor visitor = new MethodReplacementMethodVisitor(
                false, false, recording, "example/WorldCupRepository", "loadPlayers", "()V");
        visitor.visitInvokeDynamicInsn(
                "apply",
                "(L" + owner + ";)Ljava/util/function/Function;",
                METAFACTORY,
                samType,
                implementationHandle,
                instantiatedType);
        return recording;
    }

    /**
     * Captures the invokedynamic instruction emitted by the visitor under test.
     */
    private static class RecordingMethodVisitor extends MethodVisitor {

        private String descriptor;
        private Object[] arguments;

        /**
         * Creates a recorder using EvoMaster's ASM version.
         */
        private RecordingMethodVisitor() {
            super(Constants.ASM);
        }

        /**
         * Records the emitted invokedynamic descriptor and bootstrap arguments.
         *
         * @param name invokedynamic name
         * @param descriptor invokedynamic descriptor
         * @param bootstrapMethodHandle bootstrap handle
         * @param bootstrapMethodArguments bootstrap arguments
         */
        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                           Object... bootstrapMethodArguments) {
            this.descriptor = descriptor;
            this.arguments = bootstrapMethodArguments;
        }
    }
}
