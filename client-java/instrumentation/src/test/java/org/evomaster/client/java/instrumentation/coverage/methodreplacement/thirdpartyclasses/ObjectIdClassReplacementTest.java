package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.bson.types.ObjectId;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ObjectIdClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testInvalidObjectId() {
        assertThrows(IllegalArgumentException.class, this::createInvalidObjectId);
    }

    private void createInvalidObjectId() {
        new ObjectId("hi");
    }

    @Test
    public void testValidObjectId() {
        String hexString = "655e33e0032ce302a8212c13";
        ObjectId anotherObjectId = new ObjectId(hexString);
        assertEquals(hexString, anotherObjectId.toHexString());
    }

}
