package org.evomaster.client.java.instrumentation.object;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.evomaster.client.java.instrumentation.object.dtos.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassToSchemaTest {

    private static final Gson GSON = new Gson();

    private JsonObject parse(String schema){
        return JsonParser.parseString("{" + schema + "}").getAsJsonObject();
    }

    @Test
    public void testBase(){

        String schema = ClassToSchema.getOrDeriveSchema(DtoBase.class);
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



    @Test
    public void testNumeric(){

        String schema = ClassToSchema.getOrDeriveSchema(DtoNumeric.class);
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
        String schema = ClassToSchema.getOrDeriveSchema(DtoExtending.class);
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

        String schema = ClassToSchema.getOrDeriveSchema(DtoArray.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoArray.class.getName()).getAsJsonObject();

        assertEquals(5, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeInArray(obj, "string", "array");
        verifyTypeInArray(obj, "integer", "set");
        verifyTypeInArray(obj, "string", "set_raw");
        verifyTypeInArray(obj, "boolean", "list");
        verifyTypeInArray(obj, "string", "list_raw");
    }

    @Test
    public void testIgnore(){

        String schema = ClassToSchema.getOrDeriveSchema(DtoIgnore.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoIgnore.class.getName()).getAsJsonObject();

        assertEquals(1, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(obj, "string", "a");
    }

    @Test
    public void testNaming(){

        String schema = ClassToSchema.getOrDeriveSchema(DtoNaming.class);
        JsonObject json = parse(schema);
        JsonObject obj = json.get(DtoNaming.class.getName()).getAsJsonObject();

        assertEquals(3, obj.get("properties").getAsJsonObject().entrySet().size());
        verifyTypeOfFieldInProperties(obj, "string", "foo_bar");
        verifyTypeOfFieldInProperties(obj, "string", "hello_world");
        verifyTypeOfFieldInProperties(obj, "string", "foo");
    }
}