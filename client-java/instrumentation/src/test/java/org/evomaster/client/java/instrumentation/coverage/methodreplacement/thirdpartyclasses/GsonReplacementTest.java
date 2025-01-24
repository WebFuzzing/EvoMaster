package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GsonReplacementTest {

    @BeforeEach
    public void initTest() throws Exception {
        UnitsInfoRecorder.reset();
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();
    }

    @Test
    public void testFromJsonClassOfT() {
        String json = "{\n\"color\": \"red\"\n}";

        Gson gson = new Gson();
        Object gsonTestDto = GsonClassReplacement.fromJson(gson, json, GsonTestDto.class);
        assertTrue(gsonTestDto instanceof GsonTestDto);
        assertEquals("red", ((GsonTestDto) gsonTestDto).color);

        validate();
    }

    @Test
    public void testFromJsonType() {
        String json = "{\n\"color\": \"green\"\n}";

        Gson gson = new Gson();
        Type dtoType = new TypeToken<GsonTestDto>() {}.getType();
        Object gsonTestDto = GsonClassReplacement.fromJson(gson, json, dtoType);
        assertTrue(gsonTestDto instanceof GsonTestDto);
        assertEquals("green", ((GsonTestDto) gsonTestDto).color);

        validate();
    }

    @Test
    public void testFromJsonReaderClassOfT() {
        String json = "{\n\"color\": \"blue\"\n}";

        Reader reader = new StringReader(json);
        Gson gson = new Gson();
        Object gsonTestDto = GsonClassReplacement.fromJson(gson, reader, GsonTestDto.class);
        assertTrue(gsonTestDto instanceof GsonTestDto);
        assertEquals("blue", ((GsonTestDto) gsonTestDto).color);

        validate();
    }

    @Test
    public void testFromJsonReaderType() {
        String json = "{\n\"color\": \"yellow\"\n}";

        Reader reader = new StringReader(json);

        Gson gson = new Gson();
        Type dtoType = new TypeToken<GsonTestDto>() {}.getType();
        Object gsonTestDto = GsonClassReplacement.fromJson(gson, reader, dtoType);
        assertTrue(gsonTestDto instanceof GsonTestDto);
        assertEquals("yellow", ((GsonTestDto) gsonTestDto).color);

        validate();
    }

    private void validate() {
        Map<String, String> parsedDto = UnitsInfoRecorder.getInstance().getParsedDtos();
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();

        Set<String> infoList = new HashSet<>();
        additionalInfoList.forEach(info -> infoList.addAll(info.getParsedDtoNamesView()));
        assertTrue(parsedDto.containsKey(GsonTestDto.class.getName()));
        assertTrue(infoList.contains(GsonTestDto.class.getName()));
    }
}
