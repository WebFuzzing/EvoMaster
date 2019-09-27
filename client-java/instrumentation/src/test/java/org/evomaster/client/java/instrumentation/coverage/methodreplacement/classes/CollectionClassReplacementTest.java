package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by jgaleotti on 29-Ago-19.
 */
public class CollectionClassReplacementTest {

    @Test
    public void testIsEmpty() {
        List<Object> emptyList = Collections.emptyList();
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate");
        assertTrue(isEmptyValue);
    }

    @Test
    public void testIsNotEmpty() {
        List<Object> emptyList = Collections.singletonList("Hello World");
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate");
        assertFalse(isEmptyValue);
    }


}
