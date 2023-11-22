package org.evomaster.client.java.instrumentation.example.bson;

import com.foo.somedifferentpackage.examples.bson.ObjectIdBuilderImp;
import org.bson.types.ObjectId;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.shared.TaintType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectIdBuilderTest {

    private static String defaultReplacement;

    protected ObjectIdBuilder getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (ObjectIdBuilder)
                cl.loadClass(ObjectIdBuilderImp.class.getName()).getConstructor().newInstance();
    }

    @BeforeAll
    public static void initClass() {
        defaultReplacement = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        if (defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement + ",MONGO");
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,NET,MONGO");
        }
    }

    @BeforeEach
    public void prepareTest() {
        UnitsInfoRecorder.reset();
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();
    }

    @Test
    public void testInstrumentObjectIdBuilder() throws Exception {

        assertEquals(0, UnitsInfoRecorder.getInstance().getNumberOfTrackedMethods());

        ObjectIdBuilder sut = getInstance();

        String hexString = "5fc49e85baf1ef532821f8e1";
        ObjectId objectId = sut.buildNewObjectId(hexString);
        assertNotNull(objectId);

        assertEquals(1, UnitsInfoRecorder.getInstance().getNumberOfTrackedMethods());
    }

    @Test
    public void testInstrumentationOnInvalidHexString() throws Exception {

        assertEquals(0, UnitsInfoRecorder.getInstance().getNumberOfTrackedMethods());

        ObjectIdBuilder sut = getInstance();

        String hexString = "hi!";
        assertThrows(IllegalArgumentException.class, () -> sut.buildNewObjectId(hexString));

        assertEquals(1, UnitsInfoRecorder.getInstance().getNumberOfTrackedMethods());
    }


    @Test
    public void testStringSpecialization() throws Exception {

        ObjectIdBuilder sut = getInstance();


        // no string specialization yet
        assertTrue(ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().isEmpty());

        String notTaintedInput = "5fc49e85baf1ef532821f8e1";
        ObjectId objectId = sut.buildNewObjectId(notTaintedInput);
        assertNotNull(objectId);

        // no taint value leads to no specialization
        assertTrue(ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().isEmpty());


        String taint = TaintInputName.getTaintName(0);

        assertThrows(IllegalArgumentException.class, () -> sut.buildNewObjectId(taint));

        // string specialization should have been recorded due to taint value
        assertFalse(ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().isEmpty());
        Set<StringSpecializationInfo> s = ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().get(taint);

        assertEquals(1, s.size());
        assertTrue(s.stream().findFirst().isPresent());
        StringSpecializationInfo specializationInfo = s.stream().findFirst().get();
        assertEquals(StringSpecialization.REGEX_WHOLE, specializationInfo.getStringSpecialization());
        assertEquals(TaintType.FULL_MATCH, specializationInfo.getType());
        assertEquals("^[0-9a-fA-F]{24}$", specializationInfo.getValue());

    }


    @AfterAll
    public static void tearDown() {
        if (defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement);
        }
    }
}