package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.JsonTaint;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class JsonUtils {

    /**
     * Size of the chunks a stream is read in
     */
    private static final int READ_BUFFER_SIZE = 4096;

    public static String readStream(InputStream src){
        String content = new BufferedReader(
                new InputStreamReader(src, Charset.defaultCharset()))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

        return content;
    }

    /**
     * Consume the whole stream, keeping its bytes as they are (no decoding, no change of line separators).
     * Java 8 has no InputStream.readAllBytes.
     */
    public static byte[] readAllBytes(InputStream src) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int read;
        while ((read = src.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Decode raw JSON bytes as UTF-8, which is the encoding of JSON (RFC 8259), and the one tainted
     * values are written in.
     */
    public static String decodeUtf8(byte[] content){
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * Same as {@link #decodeUtf8(byte[])}, but only when the bytes could decode into a tainted value
     * (see {@link ExecutionTracer#couldBeTaintInput(byte[])}), so that large payloads are not decoded on
     * every read for nothing.
     *
     * @return the decoded text, or null if it cannot be a tainted value
     */
    public static String decodeUtf8IfCouldBeTaint(byte[] content){
        if(!ExecutionTracer.couldBeTaintInput(content)){
            return null;
        }
        return decodeUtf8(content);
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
