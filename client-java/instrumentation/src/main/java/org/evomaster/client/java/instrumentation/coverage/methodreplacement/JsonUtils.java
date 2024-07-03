package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.JsonTaint;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        JsonTaint.handlePossibleJsonTaint(content, type);
        return JsonUtils.toStream(content);
    }
}
