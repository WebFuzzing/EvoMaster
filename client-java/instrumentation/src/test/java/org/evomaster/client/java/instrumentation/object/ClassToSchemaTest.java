package org.evomaster.client.java.instrumentation.object;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.evomaster.client.java.instrumentation.object.dtos.*;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.evomaster.client.java.instrumentation.shared.ClassToSchemaUtils.OPENAPI_REF_PATH;
import static org.junit.jupiter.api.Assertions.*;

public class ClassToSchemaTest {

    private static final Gson GSON = new Gson();

    private JsonObject parse(String schema){
        return JsonParser.parseString("{" + schema + "}").getAsJsonObject();
    }

    @Test
    public void testBase(){

        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoBase.class);
        JsonObject json = parse(schema);

        JsonObject obj = json.get(DtoBase.class.getName()).getAsJsonObject();
        assertNotNull(obj);
        assertNotNull(obj.get("type"));
        assertNotNull(obj.get("properties"));
        assertEquals(2, obj.entrySet().size());
        assertEquals("object", obj.get("type").getAsString());

        assertEquals(1, obj.get("properties").getAsJsonObject().entrySet().size());
        assertEquals("string", obj.get("properties").getAsJsonObject()
                .get("foo").getAsJsonObject().get("type").getAsString());
    }


    private void verifyTypeOfFieldInProperties(JsonObject obj, String expected, String fieldName){
        assertEquals(expected, obj.get("properties").getAsJsonObject()
                .get(fieldName).getAsJsonObject().get("type").getAsString());
    }

    private void verifyTypeAndFormatOfFieldInProperties(JsonObject obj, String type, String format, String fieldName){
        JsonObject field = obj.get("properties").getAsJsonObject().get(fieldName).getAsJsonObject();
        assertEquals(type, field.get("type").getAsString());
        assertEquals(format, field.get("format").getAsString());
    }

    private void verifyEnumOfFieldInProperties(JsonObject obj, String fieldName, String[] items){
        JsonObject field = obj.get("properties").getAsJsonObject().get(fieldName).getAsJsonObject();
        assertEquals("string", field.get("type").getAsString());
        assertTrue(field.get("enum").isJsonArray());
        JsonArray array = field.get("enum").getAsJsonArray();
        assertEquals(items.length, array.size());
        for (int i = 0; i < items.length; i++){
            assertEquals(items[i], array.get(i).getAsString());
        }
    }

    @Test
    public void testNumeric(){

        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoNumeric.class);
        JsonObject json = parse(schema);

        JsonObject obj = json.get(DtoNumeric.class.getName()).getAsJsonObject();
        assertNotNull(obj);
        assertNotNull(obj.get("type"));
        assertNotNull(obj.get("properties"));
        assertEquals(2, obj.entrySet().size());
        assertEquals("object", obj.get("type").getAsString());

        assertEquals(12, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int8", "byte_p");
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int8", "byte_w");
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int16", "short_p");
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int16", "short_w");
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int32", "integer_p");
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int32", "integer_w");
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int64", "long_p");
        verifyTypeAndFormatOfFieldInProperties(obj, "integer", "int64", "long_w");
        verifyTypeAndFormatOfFieldInProperties(obj, "number", "float", "float_p");
        verifyTypeAndFormatOfFieldInProperties(obj, "number", "float", "float_w");
        verifyTypeAndFormatOfFieldInProperties(obj, "number", "double", "double_p");
        verifyTypeAndFormatOfFieldInProperties(obj, "number", "double", "double_w");
    }


    private void verifyTypeInArray(JsonObject obj, String expected, String fieldName){

        JsonObject array = obj.get("properties").getAsJsonObject()
                .get(fieldName).getAsJsonObject();

        assertEquals("array", array.get("type").getAsString());
        assertEquals(expected, array.get("items").getAsJsonObject().get("type").getAsString());
    }

    @Test
    public void testExtending(){
        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoExtending.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoExtending.class.getName()).getAsJsonObject();

        // WARN: this is limitation in GSON, as it does not have entryList(), so we
        // cannot verify that entries are unique, and not repeated by mistake :(
        assertEquals(3, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(obj, "string", "foo");
        verifyTypeOfFieldInProperties(obj, "boolean", "boolean_p");
        verifyTypeOfFieldInProperties(obj, "boolean", "boolean_w");
    }




    @Test
    public void testArray(){

        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoArray.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoArray.class.getName()).getAsJsonObject();

        checkDtoArray(obj);
    }

    @Test
    public void testIgnore(){

        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoIgnore.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoIgnore.class.getName()).getAsJsonObject();

        assertEquals(1, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(obj, "string", "a");
    }

    @Test
    public void testNaming(){

        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoNaming.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoNaming.class.getName()).getAsJsonObject();

        assertEquals(3, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(obj, "string", "foo_bar");
        verifyTypeOfFieldInProperties(obj, "string", "hello_world");
        verifyTypeOfFieldInProperties(obj, "string", "foo");
    }

    @Test
    public void testMapDto(){
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(DtoMap.class);
        JsonObject all = parse(schema);
        JsonObject jsonMapAndArray = all.get(DtoMap.class.getName()).getAsJsonObject();
        assertEquals(2, jsonMapAndArray.size());
        checkMapDto(jsonMapAndArray.get(DtoMap.class.getName()).getAsJsonObject());
        checkDtoArray(jsonMapAndArray.get(DtoArray.class.getName()).getAsJsonObject());
    }

    @Test
    public void testPureJvmMap(){
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(Map.class);
        JsonObject all = parse(schema);
        JsonObject jvmMap = all.get(Map.class.getName()).getAsJsonObject().get(Map.class.getName()).getAsJsonObject();
        verifyMapField(jvmMap, "string", false);
    }

    @Test
    public void testCycleDto(){
        UnitsInfoRecorder.reset();
        assertTrue(UnitsInfoRecorder.getInstance().getParsedDtos().isEmpty());
        ClassToSchema.registerSchemaIfNeeded(CycleDtoA.class);
        assertEquals(2, UnitsInfoRecorder.getInstance().getParsedDtos().size());

        List<Class<?>> embedded = new ArrayList<>();
        String cycleDtoASchema = ClassToSchema.getOrDeriveSchema(CycleDtoA.class, embedded);
        JsonObject json = parse(cycleDtoASchema);
        JsonObject obj = json.get(CycleDtoA.class.getName()).getAsJsonObject();
        checkCycleA(obj);

        String cycleDtoBSchema = ClassToSchema.getOrDeriveSchema(CycleDtoB.class, embedded);
        JsonObject jsonB = parse(cycleDtoBSchema);
        JsonObject objB = jsonB.get(CycleDtoB.class.getName()).getAsJsonObject();
        checkCycleB(objB);

        String allNested = ClassToSchema.getOrDeriveSchemaWithItsRef(CycleDtoA.class);
        JsonObject all = parse(allNested);
        JsonObject jsonAandB = all.get(CycleDtoA.class.getName()).getAsJsonObject();
        assertEquals(2, jsonAandB.size());
        checkCycleA(jsonAandB.get(CycleDtoA.class.getName()).getAsJsonObject());
        checkCycleB(jsonAandB.get(CycleDtoB.class.getName()).getAsJsonObject());

    }

    @Test
    public void testDtoEnum(){
        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoEnum.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoEnum.class.getName()).getAsJsonObject();
        assertEquals(2, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(obj, "string", "foo");
        verifyEnumOfFieldInProperties(obj, "bar", new String[]{"ONE", "TWO", "THREE"});
    }

    @Test
    public void testDate(){
        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoDate.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoDate.class.getName()).getAsJsonObject();
        assertEquals(1, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeAndFormatOfFieldInProperties(obj, "string", "date", "foo");
    }

    @Test
    public void testLocalDateTime(){
        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoLocalDateTime.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoLocalDateTime.class.getName()).getAsJsonObject();
        assertEquals(1, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeAndFormatOfFieldInProperties(obj, "string", "local-date-time", "foo");
    }

    @Test
    public void testLocalDate(){
        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoLocalDate.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoLocalDate.class.getName()).getAsJsonObject();
        assertEquals(1, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeAndFormatOfFieldInProperties(obj, "string", "local-date", "foo");
    }

    public void testLocalTime(){
        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoLocalTime.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoLocalTime.class.getName()).getAsJsonObject();
        assertEquals(1, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeAndFormatOfFieldInProperties(obj, "string", "local-date", "foo");
    }

    @Test
    public void testObjectRequiredFields(){

        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoObj.class, true, Collections.emptyList());
        JsonObject json = parse(schema);

        JsonObject obj = json.get(DtoObj.class.getName()).getAsJsonObject();
        assertNotNull(obj);
        assertNotNull(obj.get("required"));
        assertEquals(1, obj.get("required").getAsJsonArray().size());
        assertEquals("foo", obj.get("required").getAsJsonArray().get(0).getAsString());
    }

    @Test
    public void testCollectionField() {
        String schema = ClassToSchema.getOrDeriveNonNestedSchema(DtoCollection.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoCollection.class.getName()).getAsJsonObject();

        checkDtoCollection(obj);
    }


    private void checkDtoArray(JsonObject obj){
        assertEquals(5, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeInArray(obj, "string", "array");
        verifyTypeInArray(obj, "integer", "set");
        verifyTypeInArray(obj, "string", "set_raw");
        verifyTypeInArray(obj, "boolean", "list");
        verifyTypeInArray(obj, "string", "list_raw");
    }

    private void checkDtoCollection(JsonObject obj){
        assertEquals(2, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeInArray(obj, "boolean", "collection");
        verifyTypeInArray(obj, "string", "collection_raw");
    }

    private void checkMapDto(JsonObject obj){
        assertEquals(2, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyMapFieldInProperties(obj, OPENAPI_REF_PATH+""+DtoArray.class.getName(),true, "mapDtoArray");
        verifyMapFieldInProperties(obj, "integer",false, "mapInteger");
    }

    private void checkJvmMap(JsonObject obj){
        assertEquals(2, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyMapFieldInProperties(obj, OPENAPI_REF_PATH+""+DtoArray.class.getName(),true, "mapDtoArray");
        verifyMapFieldInProperties(obj, "integer",false, "mapInteger");
    }

    private void verifyMapFieldInProperties(JsonObject obj, String valueType, boolean isRef, String fieldName){
        JsonObject field = obj.get("properties").getAsJsonObject()
                .get(fieldName).getAsJsonObject();
        verifyMapField(field, valueType, isRef);
    }

    private void verifyMapField(JsonObject field, String valueType, boolean isRef){
        assertEquals("object", field.get("type").getAsString());
        assertTrue(field.has("additionalProperties"));
        String actualValueType;
        if (isRef)
            actualValueType = field.get("additionalProperties").getAsJsonObject().get("$ref").getAsString();
        else
            actualValueType = field.get("additionalProperties").getAsJsonObject().get("type").getAsString();
        assertEquals(valueType, actualValueType);
    }

    private void checkCycleA(JsonObject obj){
        assertEquals(2, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(obj, "string", "cycleAId");
        verifyRefOfFieldInProperties(obj, OPENAPI_REF_PATH+""+CycleDtoB.class.getName(), "cycleDtoB");
    }

    private void checkCycleB(JsonObject objB){
        assertEquals(2, objB.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(objB, "string", "cycleBId");
        verifyRefOfFieldInProperties(objB, OPENAPI_REF_PATH+""+CycleDtoA.class.getName(), "cycleDtoA");
    }

    private void verifyRefOfFieldInProperties(JsonObject obj, String expected, String fieldName){
        assertEquals(expected, obj.get("properties").getAsJsonObject()
                .get(fieldName).getAsJsonObject().get("$ref").getAsString());
    }
}