package org.evomaster.client.java.instrumentation.example.jackson;

import com.foo.somedifferentpackage.examples.jackson.JsonMapImpl;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JsonMapTest {


    protected JsonMap getInstance() throws Exception {

        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,EXT_0");
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (JsonMap) cl.loadClass(JsonMapImpl.class.getName()).newInstance();
    }

    @BeforeEach
    public void initTest() throws Exception {
        UnitsInfoRecorder.reset();
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();
    }

    @Test
    public void testMapFirstStep() throws Exception{

        JsonMap sut = getInstance();

        String taint = TaintInputName.getTaintName(42);

        try{sut.castToList(taint);}catch(Exception e){}

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<StringSpecializationInfo> ss = info.getStringSpecializationsView().get(taint);
        assertEquals(1, ss.size());
        assertEquals(StringSpecialization.JSON_MAP, ss.stream().findFirst().get().getStringSpecialization());
    }

    @Test
    public void testMapSecondWrongStep() throws Exception{

        JsonMap sut = getInstance();

        //String taint = TaintInputName.getTaintName(42);
        String json = "{}";

        try{sut.castToList(json);}catch(Exception e){}

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        assertEquals(0, info.getStringSpecializationsView().size());
    }

    @Test
    public void testMapSecondStep() throws Exception{

        JsonMap sut = getInstance();

        String taint = TaintInputName.getTaintName(42);
        String json = "{\"" + TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER + "\":\"" + taint + "\"}";

        try{sut.castToList(json);}catch(Exception e){}

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<StringSpecializationInfo> ss = info.getStringSpecializationsView().get(taint);
        assertEquals(1, ss.size());
        StringSpecializationInfo ssi = ss.stream().findFirst().get();
        assertEquals(StringSpecialization.JSON_MAP_FIELD, ssi.getStringSpecialization());
        assertEquals("matches", ssi.getValue());
    }


    @Test
    public void testMapThirdStep() throws Exception{

        JsonMap sut = getInstance();

        String taint = TaintInputName.getTaintName(42);
        String matches = TaintInputName.getTaintName(66);
        String json = "{\"" + TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER + "\":\"" + taint + "\"," +
                " \"matches\": \""+matches+"\"}";

        try{sut.castToList(json);}catch(Exception e){}

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<StringSpecializationInfo> sstaint = info.getStringSpecializationsView().get(taint);
        assertNull(sstaint);

        Set<StringSpecializationInfo> ssmatches = info.getStringSpecializationsView().get(matches);
        assertEquals(1, ssmatches.size());

        StringSpecializationInfo ssi = ssmatches.stream().findFirst().get();
        assertEquals(StringSpecialization.CAST_TO_TYPE, ssi.getStringSpecialization());
        assertEquals("java/util/ArrayList", ssi.getValue());
    }


    @Test
    public void testMapFourthStep() throws Exception{

        JsonMap sut = getInstance();

        String taint = TaintInputName.getTaintName(42);
        String json = "{\"" + TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER + "\":\"" + taint + "\"," +
                " \"matches\": []}";

        List list = sut.castToList(json);
        assertTrue(list.isEmpty());
    }


    @Test
    public void testIntArray() throws Exception{

        JsonMap sut = getInstance();

        String taint = TaintInputName.getTaintName(42);
        String matches = TaintInputName.getTaintName(66);
        String json = "{\"" + TaintInputName.TAINTED_MAP_EM_LABEL_IDENTIFIER + "\":\"" + taint + "\"," +
                " \"matches\": \""+matches+"\"}";

        try{sut.castToIntArray(json);}catch(Exception e){}

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<StringSpecializationInfo> sstaint = info.getStringSpecializationsView().get(taint);
        assertNull(sstaint);

        Set<StringSpecializationInfo> ssmatches = info.getStringSpecializationsView().get(matches);
        assertEquals(1, ssmatches.size());

        StringSpecializationInfo ssi = ssmatches.stream().findFirst().get();
        assertEquals(StringSpecialization.CAST_TO_TYPE, ssi.getStringSpecialization());
        assertEquals("[I", ssi.getValue());
    }

    @Test
    public void testNoCheckCast() throws Exception{
        JsonMap sut = getInstance();
        //those should throw no exception
        sut.castIntToInteger(4);
        sut.castIntegerToInt(5);
        sut.castLongToInt(5L);
    }


    /*
        TODO
        public Integer assignedToTypedList(String json) throws Exception {
        public int castIntFromFunction(String json) throws Exception {
     */
}
