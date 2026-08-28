package org.evomaster.client.java.instrumentation.example.dynamodb;

/**
 * JDK-only boundary used to invoke an instrumented AWS enhanced-client fixture.
 */
public interface EnhancedDynamoDbOperations extends AutoCloseable {

    /**
     * Executes one enhanced DynamoDB operation and fully consumes any lazy result.
     *
     * @param operation operation identifier
     * @return number of low-level result pages expected from the operation
     */
    int execute(String operation);

    /**
     * Executes a conditional put that is expected to fail.
     *
     * @param async whether to use the asynchronous enhanced client
     */
    void executeConditionalFailure(boolean async);

    /**
     * Closes the underlying low-level clients.
     */
    @Override
    void close();
}
