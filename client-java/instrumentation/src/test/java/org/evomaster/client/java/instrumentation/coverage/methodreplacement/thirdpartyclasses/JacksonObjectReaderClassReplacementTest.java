package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JacksonObjectReaderClassReplacementTest {

    private static final String JSON = "{\"count\": \"10\"}";

    /**
     * Not ASCII, to check that the bytes Jackson reads are the original ones
     */
    private static final String NON_ASCII_VALUE = "ñandú €";

    private static final int TAINT_ID = 42;

    private static final String ARRAY_SCHEMA = "\"type\":\"array\"";

    @BeforeEach
    public void init(){
        ObjectiveRecorder.reset(false);
        ObjectiveRecorder.setBooting(false);
        ExecutionTracer.reset();
        // otherwise a schema registered by an earlier test would make the parsed DTO checks pass on their own
        UnitsInfoRecorder.reset();
        // force the state as executing action
        ExecutionTracer.setExecutingAction(true);
        assertEquals(0 , ExecutionTracer.getNumberOfObjectives());
    }

    private static Set<String> parsedDtoNames(){
        Set<String> names = new HashSet<>();
        ExecutionTracer.exposeAdditionalInfoList().forEach(info -> names.addAll(info.getParsedDtoNamesView()));
        return names;
    }

    private static void assertParsed(Class<?> dto){
        Map<String, String> parsedDto = UnitsInfoRecorder.getInstance().getParsedDtos();
        assertTrue(parsedDto.containsKey(dto.getName()), "No schema registered for " + dto.getName());
        assertTrue(parsedDtoNames().contains(dto.getName()), dto.getName() + " not recorded as parsed in this action");
    }

    /**
     * @return the only string specialization recorded for the given taint
     */
    private static StringSpecializationInfo singleSpecialization(String taint){
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<StringSpecializationInfo> ss = additionalInfoList.get(0).getStringSpecializationsView().get(taint);
        assertNotNull(ss, "No string specialization for " + taint);
        assertEquals(1, ss.size());
        return ss.iterator().next();
    }

    private static int countSpecializations(){
        int count = 0;
        for (AdditionalInfo info : ExecutionTracer.exposeAdditionalInfoList()) {
            count += info.getStringSpecializationsView().size();
        }
        return count;
    }

    @Test
    public void testReadValueBytes() throws Throwable {
        ObjectReader reader = new ObjectMapper().readerFor(JacksonTestDto.class);

        JacksonTestDto dto = JacksonObjectReaderClassReplacement.readValue(reader, JSON.getBytes(StandardCharsets.UTF_8));

        assertEquals("10", dto.count);
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testReadValueString() throws Throwable {
        ObjectReader reader = new ObjectMapper().readerFor(JacksonTestDto.class);

        JacksonTestDto dto = JacksonObjectReaderClassReplacement.readValue(reader, JSON);

        assertEquals("10", dto.count);
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testReadValueBytesPassesTheOriginalBytes() throws Throwable {
        ObjectReader reader = new ObjectMapper().readerFor(JacksonTestDto.class);
        byte[] content = ("{\"count\": \"" + NON_ASCII_VALUE + "\"}").getBytes(StandardCharsets.UTF_8);

        JacksonTestDto dto = JacksonObjectReaderClassReplacement.readValue(reader, content);

        assertEquals(NON_ASCII_VALUE, dto.count);
    }

    @Test
    public void testTaintedBytes() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectReader reader = new ObjectMapper().readerFor(JacksonTestDto.class);

        // a taint is not valid JSON, so Jackson fails after the analysis
        assertThrows(Exception.class, () ->
                JacksonObjectReaderClassReplacement.readValue(reader, taint.getBytes(StandardCharsets.UTF_8)));

        StringSpecializationInfo info = singleSpecialization(taint);
        assertEquals(StringSpecialization.JSON_OBJECT, info.getStringSpecialization());
        assertTrue(info.getValue().contains(JacksonTestDto.class.getSimpleName()), "Invalid type: " + info.getValue());
    }

    @Test
    public void testNonTaintedBytes() throws Throwable {
        ObjectReader reader = new ObjectMapper().readerFor(JacksonTestDto.class);

        JacksonObjectReaderClassReplacement.readValue(reader, JSON.getBytes(StandardCharsets.UTF_8));

        assertEquals(0, countSpecializations(), "A document that is not a taint must not be specialized");
    }

    @Test
    public void testTaintedBytesIntoList() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<JacksonTestDto>>() {});

        assertThrows(Exception.class, () ->
                JacksonObjectReaderClassReplacement.readValue(reader, taint.getBytes(StandardCharsets.UTF_8)));

        StringSpecializationInfo info = singleSpecialization(taint);
        assertEquals(StringSpecialization.JSON_ARRAY, info.getStringSpecialization());
        assertTrue(info.getValue().contains(JacksonTestDto.class.getSimpleName()), "Invalid type: " + info.getValue());
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testTaintedBytesIntoArray() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectReader reader = new ObjectMapper().readerFor(JacksonTestDto[].class);

        assertThrows(Exception.class, () ->
                JacksonObjectReaderClassReplacement.readValue(reader, taint.getBytes(StandardCharsets.UTF_8)));

        /*
            As in JacksonObjectMapperClassReplacement for a JacksonTestDto[] type: a JSON object specialization
            whose schema is an array of JacksonTestDto
         */
        StringSpecializationInfo info = singleSpecialization(taint);
        assertEquals(StringSpecialization.JSON_OBJECT, info.getStringSpecialization());
        assertTrue(info.getValue().contains(ARRAY_SCHEMA), "Invalid type: " + info.getValue());
        assertTrue(info.getValue().contains(JacksonTestDto.class.getSimpleName()), "Invalid type: " + info.getValue());
        assertParsed(JacksonTestDto[].class);
    }

    @Test
    public void testReadListFromBytes() throws Throwable {
        ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<JacksonTestDto>>() {});

        List<JacksonTestDto> dtos = JacksonObjectReaderClassReplacement.readValue(reader,
                ("[" + JSON + "," + JSON + "]").getBytes(StandardCharsets.UTF_8));

        assertEquals(2, dtos.size());
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testReaderWithoutValueType() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectReader reader = new ObjectMapper().reader();

        /*
            Nothing to analyze, and Jackson itself refuses to read without a type: the replacement must
            let that very same exception through.
         */
        assertThrows(InvalidDefinitionException.class, () ->
                JacksonObjectReaderClassReplacement.readValue(reader, JSON.getBytes(StandardCharsets.UTF_8)));
        // a taint fails even earlier, as it is not JSON
        assertThrows(Exception.class, () ->
                JacksonObjectReaderClassReplacement.readValue(reader, taint.getBytes(StandardCharsets.UTF_8)));

        assertTrue(parsedDtoNames().isEmpty());
        assertEquals(0, countSpecializations());
    }

    @Test
    public void testNullBytes() {
        ObjectReader reader = new ObjectMapper().readerFor(JacksonTestDto.class);

        // whatever Jackson does with a null input, it is not the replacement that fails on it
        assertThrows(IllegalArgumentException.class, () ->
                JacksonObjectReaderClassReplacement.readValue(reader, (byte[]) null));
        assertTrue(parsedDtoNames().isEmpty());
    }
}
