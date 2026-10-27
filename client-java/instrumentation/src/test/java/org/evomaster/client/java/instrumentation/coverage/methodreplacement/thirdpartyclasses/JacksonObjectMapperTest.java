package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.instrumentation.Action;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.JsonUtils;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.evomaster.client.java.instrumentation.shared.StringSpecialization.JSON_ARRAY;
import static org.evomaster.client.java.instrumentation.shared.StringSpecialization.JSON_OBJECT;
import static org.junit.jupiter.api.Assertions.*;

public class JacksonObjectMapperTest {

    private static final String DTO_JSON = "{\n\"count\": 10\n}";

    /**
     * Not ASCII, to check that the bytes Jackson reads are the original ones
     */
    private static final String NON_ASCII_JSON = "{\"count\": \"ñandú €\"}";

    private static final String NON_ASCII_VALUE = "ñandú €";

    private static final int TAINT_ID = 42;

    private static final String ARRAY_SCHEMA = "\"type\":\"array\"";

    /**
     * Length of a JSON string value that makes a document too long to be a tainted value
     */
    private static final int LARGE_VALUE_LENGTH = 10_000;

    private static final String ACTION_NAME = "JacksonObjectMapperTest_action";

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

    @Test
    public void testReadValue() throws Throwable {
        String json = "{\n\"count\": 10\n}";
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonObjectMapperClassReplacement.readValue(objectMapper,json, JacksonTestDto.class);

        Map<String, String> parsedDto = UnitsInfoRecorder.getInstance().getParsedDtos();
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();

        Set<String> infoList = new HashSet<>();
        additionalInfoList.forEach(info -> {
            infoList.addAll(info.getParsedDtoNamesView());
        });
        assertTrue(parsedDto.containsKey(JacksonTestDto.class.getName()));
        assertTrue(infoList.contains(JacksonTestDto.class.getName()));
    }

    @Test
    public void testReadValueStringJavaType() throws Throwable {
        String json = "{\n\"count\": 10\n}";


        ObjectMapper objectMapper = new ObjectMapper();
        JacksonObjectMapperClassReplacement.readValue_EM_0(objectMapper,json, objectMapper.constructType(JacksonTestDto.class));

        Map<String, String> parsedDto = UnitsInfoRecorder.getInstance().getParsedDtos();
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();

        Set<String> infoList = new HashSet<>();
        additionalInfoList.forEach(info -> {
            infoList.addAll(info.getParsedDtoNamesView());
        });
        assertTrue(parsedDto.containsKey(JacksonTestDto.class.getName()));
        assertTrue(infoList.contains(JacksonTestDto.class.getName()));
    }


    @Test
    public void testReadValueStringTypeReference() throws Throwable {
        String json = "{\n\"count\": 10\n}";

        ObjectMapper objectMapper = new ObjectMapper();
        JacksonObjectMapperClassReplacement.readValue_EM_1(objectMapper,json,new TypeReference<JacksonTestDto>() { });

        Map<String, String> parsedDto = UnitsInfoRecorder.getInstance().getParsedDtos();
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();

        Set<String> infoList = new HashSet<>();
        additionalInfoList.forEach(info -> {
            infoList.addAll(info.getParsedDtoNamesView());
        });
        assertTrue(parsedDto.containsKey(JacksonTestDto.class.getName()));
        assertTrue(infoList.contains(JacksonTestDto.class.getName()));
    }


    @Test
    public void testReadIntegerList() throws Throwable {

        String json = TaintInputName.getTaintName(42);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JacksonObjectMapperClassReplacement.readValue_EM_1(objectMapper, json, new TypeReference<List<Integer>>() {});
        }catch (Exception e){
            //expected
        }

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Map<String, Set<StringSpecializationInfo>> ssi = additionalInfoList.get(0).getStringSpecializationsView();
        Set<StringSpecializationInfo> ss = ssi.get(json);
        assertEquals(1, ss.size());
        StringSpecializationInfo info = ss.iterator().next();
        assertEquals(JSON_ARRAY, info.getStringSpecialization());
        assertTrue(info.getValue().contains("integer"), "Invalid type: " + info.getValue());
    }


    @Test
    public void testReadDtoList() throws Throwable {

        String json = TaintInputName.getTaintName(42);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JacksonObjectMapperClassReplacement.readValue_EM_1(objectMapper, json, new TypeReference<ArrayList<JacksonTestDto>>() {});
        }catch (Exception e){
            //expected
        }

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Map<String, Set<StringSpecializationInfo>> ssi = additionalInfoList.get(0).getStringSpecializationsView();
        Set<StringSpecializationInfo> ss = ssi.get(json);
        assertEquals(1, ss.size());
        StringSpecializationInfo info = ss.iterator().next();
        assertEquals(JSON_ARRAY, info.getStringSpecialization());
        assertTrue(info.getValue().contains("JacksonTestDto"), "Invalid type: " + info.getValue());
    }


    private static void assertParsed(Class<?> dto){
        Map<String, String> parsedDto = UnitsInfoRecorder.getInstance().getParsedDtos();
        Set<String> infoList = new HashSet<>();
        ExecutionTracer.exposeAdditionalInfoList().forEach(info -> infoList.addAll(info.getParsedDtoNamesView()));
        assertTrue(parsedDto.containsKey(dto.getName()), "No schema registered for " + dto.getName());
        assertTrue(infoList.contains(dto.getName()), dto.getName() + " not recorded as parsed in this action");
    }

    /**
     * @return the only string specialization recorded for the given taint
     */
    private static StringSpecializationInfo singleSpecialization(String taint){
        Set<StringSpecializationInfo> ss = ExecutionTracer.exposeAdditionalInfoList().get(0)
                .getStringSpecializationsView().get(taint);
        assertNotNull(ss, "No string specialization for " + taint);
        assertEquals(1, ss.size());
        return ss.iterator().next();
    }

    private static byte[] utf8(String s){
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void testReadValueBytesClass() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTestDto dto = JacksonObjectMapperClassReplacement.readValue(objectMapper, utf8(DTO_JSON), JacksonTestDto.class);

        assertEquals("10", dto.count);
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testReadValueBytesJavaType() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTestDto dto = JacksonObjectMapperClassReplacement.readValue_EM_2(objectMapper, utf8(DTO_JSON),
                objectMapper.constructType(JacksonTestDto.class));

        assertEquals("10", dto.count);
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testReadValueBytesTypeReference() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTestDto dto = JacksonObjectMapperClassReplacement.readValue_EM_3(objectMapper, utf8(DTO_JSON),
                new TypeReference<JacksonTestDto>() { });

        assertEquals("10", dto.count);
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testReadValueInputStreamTypeReference() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream src = new ByteArrayInputStream(utf8(DTO_JSON));
        JacksonTestDto dto = JacksonObjectMapperClassReplacement.readValue_EM_4(objectMapper, src,
                new TypeReference<JacksonTestDto>() { });

        assertEquals("10", dto.count);
        assertParsed(JacksonTestDto.class);
    }

    @Test
    public void testReadValueBytesPassesTheOriginalBytes() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();

        JacksonTestDto fromClass = JacksonObjectMapperClassReplacement.readValue(objectMapper, utf8(NON_ASCII_JSON), JacksonTestDto.class);
        JacksonTestDto fromJavaType = JacksonObjectMapperClassReplacement.readValue_EM_2(objectMapper, utf8(NON_ASCII_JSON),
                objectMapper.constructType(JacksonTestDto.class));
        JacksonTestDto fromTypeReference = JacksonObjectMapperClassReplacement.readValue_EM_3(objectMapper, utf8(NON_ASCII_JSON),
                new TypeReference<JacksonTestDto>() { });
        JacksonTestDto fromStream = JacksonObjectMapperClassReplacement.readValue_EM_4(objectMapper,
                new ByteArrayInputStream(utf8(NON_ASCII_JSON)), new TypeReference<JacksonTestDto>() { });

        assertEquals(NON_ASCII_VALUE, fromClass.count);
        assertEquals(NON_ASCII_VALUE, fromJavaType.count);
        assertEquals(NON_ASCII_VALUE, fromTypeReference.count);
        assertEquals(NON_ASCII_VALUE, fromStream.count);
    }

    @Test
    public void testTaintedBytes() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectMapper objectMapper = new ObjectMapper();

        // a taint is not valid JSON, so Jackson fails after the analysis
        assertThrows(Exception.class, () ->
                JacksonObjectMapperClassReplacement.readValue(objectMapper, utf8(taint), JacksonTestDto.class));

        StringSpecializationInfo info = singleSpecialization(taint);
        assertEquals(JSON_OBJECT, info.getStringSpecialization());
        assertTrue(info.getValue().contains("JacksonTestDto"), "Invalid type: " + info.getValue());
    }

    @Test
    public void testNonTaintedBytes() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonObjectMapperClassReplacement.readValue(objectMapper, utf8(DTO_JSON), JacksonTestDto.class);

        assertTrue(ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().isEmpty(),
                "A document that is not a taint must not be specialized");
    }

    @Test
    public void testReadDtoListFromBytes() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(Exception.class, () ->
                JacksonObjectMapperClassReplacement.readValue_EM_3(objectMapper, utf8(taint),
                        new TypeReference<ArrayList<JacksonTestDto>>() {}));

        StringSpecializationInfo info = singleSpecialization(taint);
        assertEquals(JSON_ARRAY, info.getStringSpecialization());
        assertTrue(info.getValue().contains("JacksonTestDto"), "Invalid type: " + info.getValue());
    }

    @Test
    public void testReadDtoArrayFromBytes() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(Exception.class, () ->
                JacksonObjectMapperClassReplacement.readValue_EM_2(objectMapper, utf8(taint),
                        objectMapper.constructType(JacksonTestDto[].class)));

        /*
            As for a JacksonTestDto[] class given directly: a JSON object specialization whose schema
            is an array of JacksonTestDto
         */
        StringSpecializationInfo info = singleSpecialization(taint);
        assertEquals(JSON_OBJECT, info.getStringSpecialization());
        assertTrue(info.getValue().contains(ARRAY_SCHEMA), "Invalid type: " + info.getValue());
        assertTrue(info.getValue().contains("JacksonTestDto"), "Invalid type: " + info.getValue());
    }

    @Test
    public void testReadDtoListFromStream() throws Throwable {
        String taint = TaintInputName.getTaintName(TAINT_ID);
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(Exception.class, () ->
                JacksonObjectMapperClassReplacement.readValue_EM_4(objectMapper, new ByteArrayInputStream(utf8(taint)),
                        new TypeReference<List<JacksonTestDto>>() {}));

        StringSpecializationInfo info = singleSpecialization(taint);
        assertEquals(JSON_ARRAY, info.getStringSpecialization());
        assertTrue(info.getValue().contains("JacksonTestDto"), "Invalid type: " + info.getValue());
    }

    @Test
    public void testNullBytes() {
        ObjectMapper objectMapper = new ObjectMapper();

        // whatever Jackson does with a null input, it is not the replacement that fails on it
        assertThrows(IllegalArgumentException.class, () ->
                JacksonObjectMapperClassReplacement.readValue(objectMapper, (byte[]) null, JacksonTestDto.class));
        assertThrows(IllegalArgumentException.class, () ->
                JacksonObjectMapperClassReplacement.readValue_EM_2(objectMapper, null, objectMapper.constructType(JacksonTestDto.class)));
    }

    @Test
    public void testReadValueInputStreamTypeReferenceClosesTheSource() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        CloseTrackingStream src = new CloseTrackingStream(utf8(DTO_JSON));

        JacksonObjectMapperClassReplacement.readValue_EM_4(objectMapper, src, new TypeReference<JacksonTestDto>() { });

        assertTrue(src.closed, "AUTO_CLOSE_SOURCE is on by default, so the caller's stream must be closed");
    }

    @Test
    public void testReadValueInputStreamTypeReferenceLeavesTheSourceOpen() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        CloseTrackingStream src = new CloseTrackingStream(utf8(DTO_JSON));

        JacksonTestDto dto = JacksonObjectMapperClassReplacement.readValue_EM_4(objectMapper, src,
                new TypeReference<JacksonTestDto>() { });

        assertEquals("10", dto.count);
        assertFalse(src.closed, "With AUTO_CLOSE_SOURCE off, the caller's stream must stay open");
    }

    @Test
    public void testReadValueInputStreamTypeReferenceClosesTheSourceOnAFailedRead() {
        ObjectMapper objectMapper = new ObjectMapper();
        FailingStream src = new FailingStream();

        assertThrows(IOException.class, () -> JacksonObjectMapperClassReplacement.readValue_EM_4(objectMapper, src,
                new TypeReference<JacksonTestDto>() { }));

        assertTrue(src.closed, "Jackson closes the source when a read fails and AUTO_CLOSE_SOURCE is on");
    }

    @Test
    public void testReadValueInputStreamTypeReferenceLeavesTheSourceOpenOnAFailedRead() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        FailingStream src = new FailingStream();

        assertThrows(IOException.class, () -> JacksonObjectMapperClassReplacement.readValue_EM_4(objectMapper, src,
                new TypeReference<JacksonTestDto>() { }));

        assertFalse(src.closed, "With AUTO_CLOSE_SOURCE off, the caller's stream must stay open");
    }

    @Test
    public void testLargeBytesAreNotDecodedButRegisterTheSchema() throws Throwable {
        // far longer than any taint name, and there are no input variables
        String json = "{\"count\": \"" + new String(new char[LARGE_VALUE_LENGTH]).replace('\0', 'a') + "\"}";
        ObjectMapper objectMapper = new ObjectMapper();

        // what the replacements call to get the text to check: null means no decoding
        assertNull(JsonUtils.decodeUtf8IfCouldBeTaint(utf8(json)));

        JacksonObjectMapperClassReplacement.readValue(objectMapper, utf8(json), JacksonTestDto.class);
        JacksonObjectMapperClassReplacement.readValue_EM_2(objectMapper, utf8(json), objectMapper.constructType(JacksonTestDto.class));
        JacksonObjectMapperClassReplacement.readValue_EM_3(objectMapper, utf8(json), new TypeReference<JacksonTestDto>() { });

        assertParsed(JacksonTestDto.class);
        assertTrue(ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().isEmpty());
    }

    @Test
    public void testBytesMatchingAnInputVariable() throws Throwable {
        // an input variable is any value the test registers, not necessarily a TaintInputName one
        String variable = NON_ASCII_VALUE;
        ExecutionTracer.setAction(new Action(0, ACTION_NAME, Collections.singleton(variable),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList()));
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(Exception.class, () ->
                JacksonObjectMapperClassReplacement.readValue(objectMapper, utf8(variable), JacksonTestDto.class));

        StringSpecializationInfo info = singleSpecialization(variable);
        assertEquals(JSON_OBJECT, info.getStringSpecialization());
    }

    /**
     * A stream that fails on every read, and remembers whether it was closed
     */
    private static class FailingStream extends InputStream {

        private boolean closed = false;

        @Override
        public int read() throws IOException {
            throw new IOException("the stream cannot be read");
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /**
     * A stream that remembers whether it was closed
     */
    private static class CloseTrackingStream extends ByteArrayInputStream {

        private boolean closed = false;

        CloseTrackingStream(byte[] content) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
