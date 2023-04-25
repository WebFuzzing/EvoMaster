package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonObjectMapperTest {

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

    // new TypeReference<List<Integer>>() { }
}