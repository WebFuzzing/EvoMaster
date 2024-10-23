package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GsonReplacementTest {

    @Test
    public void testFromJsonClassOfT() {
        String json = "{\n\"color\": \"red\"\n}";

        Gson gson = new Gson();
        GsonClassReplacement.fromJson(gson, json, GsonTestDto.class);
        validate();
    }

    @Test
    public void testFromJsonType() {
        String json = "{\n\"color\": \"red\"\n}";

        Gson gson = new Gson();
        Type dtoType = new TypeToken<GsonTestDto>() {}.getType();
        GsonClassReplacement.fromJson(gson, json, dtoType);
        validate();
    }

    @Test
    public void testFromJsonReaderClassOfT() {
        String json = "{\n\"color\": \"red\"\n}";

        Reader reader = new StringReader(json);
        Gson gson = new Gson();
        GsonClassReplacement.fromJson(gson, reader, GsonTestDto.class);
        validate();
    }

    @Test
    public void testFromJsonReaderType() {
        String json = "{\n\"color\": \"red\"\n}";

        Reader reader = new StringReader(json);

        Gson gson = new Gson();
        Type dtoType = new TypeToken<GsonTestDto>() {}.getType();
        GsonClassReplacement.fromJson(gson, reader, dtoType);

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
