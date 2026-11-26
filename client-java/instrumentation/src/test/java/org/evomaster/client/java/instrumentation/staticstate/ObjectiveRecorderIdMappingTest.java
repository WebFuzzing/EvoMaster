package org.evomaster.client.java.instrumentation.staticstate;

import org.evomaster.client.java.instrumentation.InstrumentationController;
import org.evomaster.client.java.instrumentation.TargetInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectiveRecorderIdMappingTest {

    @BeforeEach
    @AfterEach
    public void reset() {
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();
    }

    private static Map<String, Integer> mapping(Object... pairs) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    @Test
    public void testRestoredIdsKeepTheirTargets() {
        ObjectiveRecorder.restoreIdMapping(mapping("A", 5, "B", 7));

        assertEquals("A", ObjectiveRecorder.getDescriptiveId(5));
        assertEquals("B", ObjectiveRecorder.getDescriptiveId(7));
        assertEquals(5, ObjectiveRecorder.getMappedId("A"));
        assertEquals(7, ObjectiveRecorder.getMappedId("B"));
    }

    @Test
    public void testNewTargetsDoNotReuseRestoredIds() {
        ObjectiveRecorder.restoreIdMapping(mapping("A", 5, "B", 7));

        assertEquals(8, ObjectiveRecorder.getMappedId("C"));
        assertEquals(9, ObjectiveRecorder.getMappedId("D"));
    }

    @Test
    public void testNoRestoreOnceTargetsHaveIds() {
        ObjectiveRecorder.update("X", 0.5);
        assertEquals(0, ObjectiveRecorder.getMappedId("X"));

        ObjectiveRecorder.restoreIdMapping(mapping("A", 0));

        assertEquals("X", ObjectiveRecorder.getDescriptiveId(0));
        assertFalse(ObjectiveRecorder.isMapped(1));
    }

    @Test
    public void testEmptyMappingChangesNothing() {
        assertEquals(0, ObjectiveRecorder.getMappedId("X"));

        ObjectiveRecorder.restoreIdMapping(Collections.emptyMap());

        assertEquals(0, ObjectiveRecorder.getMappedId("X"));
        assertEquals(1, ObjectiveRecorder.getMappedId("Y"));
    }

    @Test
    public void testUnknownIdIsNotReached() {
        assertFalse(ObjectiveRecorder.isMapped(42));

        List<TargetInfo> infos = InstrumentationController.getTargetInfos(Collections.singletonList(42), false, false);

        assertEquals(1, infos.size());
        assertEquals(42, infos.get(0).mappedId);
        assertEquals(0d, infos.get(0).value);
    }
}
