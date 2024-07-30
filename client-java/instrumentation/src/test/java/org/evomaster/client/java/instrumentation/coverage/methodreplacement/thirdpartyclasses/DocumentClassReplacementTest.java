package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.bson.Document;
import org.bson.json.JsonParseException;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.evomaster.client.java.instrumentation.shared.StringSpecialization.JSON_MAP;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
        UnitsInfoRecorder.reset();
        UnitsInfoRecorder.getInstance().registerClassLoader("org.bson.Document", Document.class.getClassLoader());
    }

    @AfterEach
    public void resetTracers() {
        ExecutionTracer.reset();
        UnitsInfoRecorder.reset();
    }

    @Test
    public void testParse() {
        // create the expected document after parsing
        Document expectedDocument = new Document();
        expectedDocument.put("key", "value");

        // Define the JSON string to be parsed
        String jsonString = "{\"key\":\"value\"}";

        Object document = DocumentClassReplacement.parse(jsonString);
        assertEquals(expectedDocument, document);
    }

    @Test
    public void testTaint() {
        final String taintString = "_EM_42_XYZ_";
        assertTrue(TaintInputName.isTaintInput(taintString));
        assertThrows(JsonParseException.class, () -> DocumentClassReplacement.parse(taintString));
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        AdditionalInfo additionalInfo = additionalInfoList.get(0);
        assertEquals(1, additionalInfo.getStringSpecializationsView().size());
        assertTrue(additionalInfo.getStringSpecializationsView().containsKey(taintString));
        Set<StringSpecializationInfo> stringSpecializations = additionalInfo.getStringSpecializationsView().get(taintString);
        assertEquals(1, stringSpecializations.size());
        StringSpecializationInfo stringSpecialization = stringSpecializations.iterator().next();
        assertEquals(JSON_MAP, stringSpecialization.getStringSpecialization());
    }


}
