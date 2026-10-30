package org.evomaster.client.java.instrumentation.example.dynamodb;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Enhanced Client reaches Function.apply replacements through instrumentation.
 */
public class DynamoDbEnhancedFunctionInstrumentedTest {

    private static final String REPLACEMENT_CATEGORIES = "BASE,DYNAMODB";
    private static final String EXAMPLE_PACKAGE = "com.foo.somedifferentpackage.examples.dynamodb";
    private static final String EXAMPLE_CLASS = EXAMPLE_PACKAGE + ".EnhancedFunctionApplyExample";
    private static final String UPDATE_VERSIONED_PRODUCT = "updateVersionedProduct";
    private static final String EXECUTE_PLAYER_OPERATION = "executePlayerOperation";
    private static final String GET_ITEM = "getItem";
    private static final String PUT_ITEM = "putItem";
    private static final String DELETE_ITEM = "deleteItem";
    private static final String CONDITION_EXPRESSION_METHOD = "conditionExpression";
    private static final String EXPRESSION_ATTRIBUTE_VALUES_METHOD = "expressionAttributeValues";
    private static final String NUMBER_VALUE_METHOD = "n";
    private static final String PRODUCT_VERSION = "3";

    private String previousCategories;

    /** Enables DynamoDB tracking and clears previous commands. */
    @BeforeEach
    public void setup() {
        previousCategories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, REPLACEMENT_CATEGORIES);
        ExecutionTracer.reset();
    }

    /** Restores the surrounding replacement configuration. */
    @AfterEach
    public void teardown() {
        if (previousCategories == null) {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, previousCategories);
        }
    }

    /** Both clients must capture the final request's version condition as a serializable snapshot. */
    @Test
    public void testRecordVersionedUpdatesFromInstrumentedEnhancedClient() throws Exception {
        InstrumentingClassLoader loader = new InstrumentingClassLoader(EXAMPLE_PACKAGE);
        Class<?> example = loader.loadClass(EXAMPLE_CLASS);
        Method update = example.getMethod(UPDATE_VERSIONED_PRODUCT, boolean.class);

        for (boolean async : new boolean[]{false, true}) {
            ExecutionTracer.reset();
            Object request = update.invoke(null, async);

            List<DynamoDbCommand> commands = new ArrayList<>();
            for (AdditionalInfo info : ExecutionTracer.exposeAdditionalInfoList()) {
                commands.addAll(info.getDynamoDbInfoData());
            }
            assertEquals(1, commands.size(), async ? "async" : "sync");
            DynamoDbCommand command = commands.get(0);
            assertEquals(DynamoDbOperationNames.UPDATE_ITEM, command.getOperationName());
            assertEquals("Products", command.getTableNames().get(0));
            assertTrue(command.isSuccessfullyExecuted());

            String condition = (String) request.getClass().getMethod(CONDITION_EXPRESSION_METHOD).invoke(request);
            assertFalse(condition.isEmpty());
            assertEquals(condition, command.getDdbRequest().filterExpression());
            Map<?, ?> values = (Map<?, ?>) request.getClass()
                    .getMethod(EXPRESSION_ATTRIBUTE_VALUES_METHOD).invoke(request);
            assertTrue(values.values().stream().anyMatch(value -> {
                try {
                    return PRODUCT_VERSION.equals(value.getClass().getMethod(NUMBER_VALUE_METHOD).invoke(value));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            }));
            assertTrue(command.getDdbRequest().expressionAttributeValues().values().stream()
                    .anyMatch(value -> PRODUCT_VERSION.equals(value.getPayload())));
        }
    }

    /** Get, put, and delete must capture request snapshots for both Enhanced Clients. */
    @Test
    public void testRecordEveryEagerPlayerOperation() throws Exception {
        InstrumentingClassLoader loader = new InstrumentingClassLoader(EXAMPLE_PACKAGE);
        Class<?> example = loader.loadClass(EXAMPLE_CLASS);
        Method execute = example.getMethod(EXECUTE_PLAYER_OPERATION, String.class, boolean.class);

        for (String operation : new String[]{GET_ITEM, PUT_ITEM, DELETE_ITEM}) {
            for (boolean async : new boolean[]{false, true}) {
                ExecutionTracer.reset();
                execute.invoke(null, operation, async);
                List<DynamoDbCommand> commands = new ArrayList<>();
                for (AdditionalInfo info : ExecutionTracer.exposeAdditionalInfoList()) {
                    commands.addAll(info.getDynamoDbInfoData());
                }

                String context = operation + (async ? " async" : " sync");
                assertEquals(1, commands.size(), context);
                DynamoDbCommand command = commands.get(0);
                assertEquals("WorldCupPlayers", command.getDdbRequest().tableName(), context);
                if (!PUT_ITEM.equals(operation)) {
                    assertEquals("Argentina", command.getDdbRequest().key().get("country").getPayload(), context);
                    assertEquals("Lionel Messi", command.getDdbRequest().key().get("name").getPayload(), context);
                }
                assertEquals(operationName(operation), command.getOperationName(), context);
                assertEquals("WorldCupPlayers", command.getTableNames().get(0), context);
                assertTrue(command.isSuccessfullyExecuted(), context);
            }
        }
    }

    /** @return the tracked DynamoDB operation for an eager client method */
    private static DynamoDbOperationNames operationName(String operation) {
        switch (operation) {
            case GET_ITEM: return DynamoDbOperationNames.GET_ITEM;
            case PUT_ITEM: return DynamoDbOperationNames.PUT_ITEM;
            case DELETE_ITEM: return DynamoDbOperationNames.DELETE_ITEM;
            default: throw new IllegalArgumentException(operation);
        }
    }
}
