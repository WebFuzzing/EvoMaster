package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ReplacementList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The constructor of {@code BasicRedisPersistentEntity} has a replacement for each location of its
 * {@code TypeInformation} input, which depends on the version of Spring Data. As the classpath has only one
 * version of it, exactly one of those replacements must be the one that applies.
 */
public class BasicRedisPersistentEntityVersionsTest {

    @Test
    public void testOnlyTheReplacementOfTheVersionOnTheClasspathIsAvailable() {
        List<MethodReplacementClass> replacements = ReplacementList.getReplacements(
                "org.springframework.data.redis.core.mapping.BasicRedisPersistentEntity", true);

        assertTrue(replacements.size() > 1, "Expected a replacement for each version of Spring Data");

        long available = replacements.stream().filter(MethodReplacementClass::isAvailable).count();
        assertEquals(1, available);
    }
}
