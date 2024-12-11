package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.evomaster.client.java.instrumentation.shared.StringSpecialization.JSON_ARRAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonObjectMapperTest {

    @BeforeEach
    public void init(){
        ObjectiveRecorder.reset(false);
        ObjectiveRecorder.setBooting(false);
        ExecutionTracer.reset();
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
}