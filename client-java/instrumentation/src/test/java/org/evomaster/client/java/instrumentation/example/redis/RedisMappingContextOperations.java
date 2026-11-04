package org.evomaster.client.java.instrumentation.example.redis;

/**
 * Lets a test, which is loaded by the regular classloader, make Spring Data Redis build the persistent
 * entity of a class inside an {@code InstrumentingClassLoader}, ie where the creation of
 * {@code BasicRedisPersistentEntity} is replaced and its schema captured.
 */
public interface RedisMappingContextOperations {

    /**
     * @param entityClassName name of the entity class, which is loaded by the classloader of the implementation
     */
    void createPersistentEntity(String entityClassName) throws ClassNotFoundException;
}
