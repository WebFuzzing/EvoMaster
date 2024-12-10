package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.JsonTaint;

import java.io.*;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

public class JsonUtils {

    public static String readStream(InputStream src){
        String content = new BufferedReader(
                new InputStreamReader(src, Charset.defaultCharset()))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

        return content;
    }

    public static InputStream toStream(String content){
        return new ByteArrayInputStream(content.getBytes(Charset.defaultCharset()));
    }


    /**
     * Consume the input stream, analyze the class for given type, and return a new stream for the input data
     */
    public static InputStream analyzeClass(InputStream entityStream, Class<Object> type){
        String content = JsonUtils.readStream(entityStream);
        ClassToSchema.registerSchemaIfNeeded(type);
        JsonTaint.handlePossibleJsonTaint(content, type, false);
        return JsonUtils.toStream(content);
    }

    /**
     * Reader the JSON string from Reader.
     */
    public static String getStringFromReader(Reader reader){
        StringBuilder s = new StringBuilder();
        int character;

        try {
            while ((character = reader.read()) != -1) {
                s.append((char) character);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return s.toString();
    }

    /**
     * Returns Reader from JSON string
     */
    public static Reader stringToReader(String content) {
        return new StringReader(content);
    }
}
