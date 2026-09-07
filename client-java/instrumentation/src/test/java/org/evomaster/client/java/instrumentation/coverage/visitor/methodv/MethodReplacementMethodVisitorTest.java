package org.evomaster.client.java.instrumentation.coverage.visitor.methodv;

import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.DynamoDbEnhancedClientClassReplacement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests enhanced DynamoDB call-site interception performed by the method-replacement visitor.
 */
public class MethodReplacementMethodVisitorTest {

    private static final String TABLE = "software/amazon/awssdk/enhanced/dynamodb/DynamoDbTable";
    private static final String ASYNC_TABLE = "software/amazon/awssdk/enhanced/dynamodb/DynamoDbAsyncTable";
    private static final String CLIENT = "software/amazon/awssdk/enhanced/dynamodb/DynamoDbEnhancedClient";
    private static final String ASYNC_CLIENT = "software/amazon/awssdk/enhanced/dynamodb/DynamoDbEnhancedAsyncClient";
    private String previousCategories;

    /**
     * Enables DynamoDB replacements for each test.
     */
    @BeforeEach
    public void enableDynamoDbReplacements() {
        previousCategories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,DYNAMODB");
    }

    /**
     * Restores the replacement categories configured by the surrounding test process.
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
     * Verifies interception for synchronous and asynchronous table operations and client batch gets.
     */
    @Test
    public void shouldInterceptEnhancedDynamoDbCalls() {
        assertIntercepted(TABLE, "getItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/Key;)Ljava/lang/Object;");
        assertIntercepted(TABLE, "getItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/GetItemEnhancedRequest;)Ljava/lang/Object;");
        assertIntercepted(TABLE, "putItem", "(Ljava/lang/Object;)V");
        assertIntercepted(TABLE, "putItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/PutItemEnhancedRequest;)V");
        assertIntercepted(TABLE, "query",
                "(Ljava/util/function/Consumer;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/PageIterable;");
        assertIntercepted(TABLE, "query",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/QueryEnhancedRequest;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/PageIterable;");
        assertIntercepted(TABLE, "scan",
                "(Ljava/util/function/Consumer;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/PageIterable;");
        assertIntercepted(TABLE, "scan",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/ScanEnhancedRequest;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/PageIterable;");
        assertIntercepted(ASYNC_TABLE, "updateItem",
                "(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;");
        assertIntercepted(ASYNC_TABLE, "updateItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/UpdateItemEnhancedRequest;)Ljava/util/concurrent/CompletableFuture;");
        assertIntercepted(ASYNC_TABLE, "deleteItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/Key;)Ljava/util/concurrent/CompletableFuture;");
        assertIntercepted(ASYNC_TABLE, "deleteItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/DeleteItemEnhancedRequest;)Ljava/util/concurrent/CompletableFuture;");
        assertIntercepted(CLIENT, "batchGetItem",
                "(Ljava/util/function/Consumer;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/BatchGetResultPageIterable;");
        assertIntercepted(CLIENT, "batchGetItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/BatchGetItemEnhancedRequest;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/BatchGetResultPageIterable;");
        assertIntercepted(ASYNC_CLIENT, "batchGetItem",
                "(Ljava/util/function/Consumer;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/BatchGetResultPagePublisher;");
        assertIntercepted(ASYNC_CLIENT, "batchGetItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/model/BatchGetItemEnhancedRequest;)Lsoftware/amazon/awssdk/enhanced/dynamodb/model/BatchGetResultPagePublisher;");
    }

    /**
     * Verifies unrelated enhanced-client operations retain their original invocation.
     */
    @Test
    public void shouldNotInterceptUnsupportedEnhancedDynamoDbCalls() {
        RecordingMethodVisitor recording = visit(TABLE, "createTable", "()V");

        MethodCall call = recording.calls.get(recording.calls.size() - 1);
        assertEquals(Opcodes.INVOKEINTERFACE, call.opcode);
        assertEquals(TABLE, call.owner);
        assertEquals("createTable", call.name);
        assertEquals("()V", call.descriptor);
    }

    /**
     * Verifies enhanced calls remain untouched when DynamoDB replacements are disabled.
     */
    @Test
    public void shouldRespectDisabledDynamoDbReplacements() {
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE");

        RecordingMethodVisitor recording = visit(TABLE, "getItem",
                "(Lsoftware/amazon/awssdk/enhanced/dynamodb/Key;)Ljava/lang/Object;");

        MethodCall call = recording.calls.get(recording.calls.size() - 1);
        assertEquals(TABLE, call.owner);
        assertEquals("getItem", call.name);
    }

    /**
     * Asserts that one enhanced-client call is redirected to its annotated replacement.
     *
     * @param owner invoked interface owner
     * @param name invoked method name
     * @param descriptor invoked method descriptor
     */
    private void assertIntercepted(String owner, String name, String descriptor) {
        RecordingMethodVisitor recording = visit(owner, name, descriptor);

        MethodCall call = recording.calls.get(recording.calls.size() - 1);
        assertEquals(Opcodes.INVOKESTATIC, call.opcode);
        assertEquals(expectedReplacementOwner(owner), call.owner);
        assertEquals(name, call.name.split("_EM_")[0]);
    }

    /**
     * Resolves the replacement class expected for an enhanced-client owner.
     *
     * @param owner enhanced-client interface owner
     * @return internal JVM name of its replacement class
     */
    private String expectedReplacementOwner(String owner) {
        Class<?> replacement;
        if (TABLE.equals(owner)) {
            replacement = DynamoDbEnhancedClientClassReplacement.SyncTable.class;
        } else if (ASYNC_TABLE.equals(owner)) {
            replacement = DynamoDbEnhancedClientClassReplacement.AsyncTable.class;
        } else if (CLIENT.equals(owner)) {
            replacement = DynamoDbEnhancedClientClassReplacement.SyncClient.class;
        } else if (ASYNC_CLIENT.equals(owner)) {
            replacement = DynamoDbEnhancedClientClassReplacement.AsyncClient.class;
        } else {
            throw new IllegalArgumentException("Unsupported test owner: " + owner);
        }
        return replacement.getName().replace('.', '/');
    }

    /**
     * Applies the visitor to one synthetic interface invocation.
     *
     * @param owner invoked interface owner
     * @param name invoked method name
     * @param descriptor invoked method descriptor
     * @return downstream instruction recording
     */
    private RecordingMethodVisitor visit(String owner, String name, String descriptor) {
        RecordingMethodVisitor recording = new RecordingMethodVisitor();
        MethodReplacementMethodVisitor visitor = new MethodReplacementMethodVisitor(
                false, true, recording, "com/foo/worldcup/PlayerRepository", "findPlayer", "()V");
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, descriptor, true);
        return recording;
    }

    private static class RecordingMethodVisitor extends MethodVisitor {

        private final List<MethodCall> calls = new ArrayList<>();

        /**
         * Creates an empty instruction recording.
         */
        private RecordingMethodVisitor() {
            super(Opcodes.ASM9);
        }

        /**
         * Records a method invocation.
         */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            calls.add(new MethodCall(opcode, owner, name, descriptor));
        }
    }

    private static class MethodCall {

        private final int opcode;
        private final String owner;
        private final String name;
        private final String descriptor;

        /**
         * Creates a recorded invocation.
         */
        private MethodCall(int opcode, String owner, String name, String descriptor) {
            this.opcode = opcode;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }
    }
}
