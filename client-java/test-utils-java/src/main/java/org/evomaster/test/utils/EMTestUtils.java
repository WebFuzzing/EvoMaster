package org.evomaster.test.utils;

/*
    Note: this class is in this module, as to make sure that the exact same code
    is used in the EvoMaster Core (eg, when making HTTP calls) and
    as well in the generated tests

    WARNING: if you change any method name/signature, need to make sure that
    the code generation is updated as well

    Note: this code needs to be kept in sync among the different programming
    languages, eg, Java, JavaScript and C#.
 */

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class containing utility functions that can be used in the
 * automatically generated tests
 */
public class EMTestUtils {

    /**
     *
     * @param locationHeader a URI-reference, coming from a "location" header. See RFC 7231.
     *                       Note: it can be a relative reference
     * @param expectedTemplate a full URI of the target resource, but with some path elements
     *                         that might (or might not) be unresolved. If {@code locationHeader} is not
     *                         empty, it will replace the beginning of this template.
     * @return a fully resolved URI for the target resource. If there are problems, just
     *          return the input locationHeader. If this latter is empty/null, then return the template
     */
    public static String resolveLocation(String locationHeader, String expectedTemplate){

        if(locationHeader==null || locationHeader.isEmpty()){
            return expectedTemplate;
        }

        URI locationURI;
        try{
            locationURI = URI.create(locationHeader);
        } catch (Exception e){
            return locationHeader;
        }

        /*
            Default behavior of split() is "peculiar", to say the least...
            /a
            would be "/" split into ["",a]
            the same as
            /a/
            !!!!!!!!
            to get trailing "", need to put a negative limit...
         */
        int wtfJava = -1;

        String locationPath = locationURI.getPath();
        String[] locationTokens = locationPath.split("/",wtfJava);


        //the template is not a valid URL, due to {}
        String normalizedTemplate = expectedTemplate.replace("{","").replace("}","");
        URI templateURI = URI.create(normalizedTemplate);
        String templatePath = templateURI.getPath();
        String[] templateTokens = templatePath.split("/",wtfJava);


        String targetPath = locationPath;

        if(templateTokens.length > locationTokens.length){
            /*
                This is to handle cases like:

                POST /elements
                PUT  /elements/{id}/x

                where the location header of POST does point to

                /elements/{id}

                and not directly to "x"
             */

            for(int i=locationTokens.length; i < templateTokens.length; i++){
                targetPath += "/" + templateTokens[i];
            }
        }


        URI targetURI;

        try {
            if (locationURI.isAbsolute() || locationURI.getHost() != null) {

                targetURI = new URI(
                        locationURI.getScheme(),
                        locationURI.getUserInfo(),
                        locationURI.getHost(),
                        locationURI.getPort(),
                        targetPath,
                        locationURI.getQuery(),
                        locationURI.getFragment());

            } else {
                targetURI = new URI(
                        templateURI.getScheme(),
                        templateURI.getUserInfo(),
                        templateURI.getHost(),
                        templateURI.getPort(),
                        targetPath,
                        templateURI.getQuery(),
                        templateURI.getFragment());
            }
        }catch (Exception e){
            //shouldn't really happen
            throw new RuntimeException(e);
        }

        return targetURI.toString();
    }


    /**
     * @param uri a string representing a URI
     * @return whether the given input string is either empty or a valid URI
     */
    public static boolean isValidURIorEmpty(String uri){

        if(uri == null || uri.trim().isEmpty()){
            return true;
        }

        try{
            URI.create(uri);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * Resolves the absolute path to the Java executable using the given
     * JDK environment variable name.
     *
     * <p>This method expects the environment variable (e.g. {@code JAVA_HOME})
     * to point to a JDK installation directory. It appends {@code "bin"} and
     * {@code "java"} to construct the full path to the Java executable.</p>
     *
     *
     * @param jdkEnvVarName the name of the JDK environment variable
     *                      (e.g. {@code "JAVA_HOME"})
     * @return the absolute path to the Java executable as a String
     * @throws RuntimeException if the environment variable is not defined or empty
     */
    public static String extractJDKPathWithEnvVarName(String jdkEnvVarName){
        return extractPathWithEnvVar(jdkEnvVarName, "bin", "java").toString();
    }

    /**
     * Resolves the absolute path to a System-Under-Test (SUT) JAR file
     * using environment variables.
     *
     *
     * @param sutDistEnvVarName the environment variable that contains the base
     *                          directory of the SUT distribution
     * @param sutJarEnvVarName the name of the JAR file (or relative path inside the distribution)
     * @return the absolute path to the SUT JAR file as a String
     * @throws RuntimeException if the distribution environment variable is not defined or empty
     */
    public static String extractSutJarNameWithEnvVarName(String sutDistEnvVarName, String sutJarEnvVarName){
        return extractPathWithEnvVar(sutDistEnvVarName, sutJarEnvVarName).toString();
    }

    /**
     * Resolves an absolute {@link Path} using the value of a given environment variable
     * as the base directory and appending additional path segments.
     *
     * <p>For example, if {@code envVarName} is {@code "JAVA_HOME"} and
     * {@code others} contains {@code "bin", "java"}, this method will return:</p>
     *
     * <pre>
     * $JAVA_HOME/bin/java   (Linux/macOS)
     * %JAVA_HOME%\bin\java  (Windows)
     * </pre>
     *
     * <p>The resulting path is converted to an absolute path.</p>
     *
     * @param envVarName the name of the environment variable (e.g. {@code "JAVA_HOME"})
     * @param others additional path segments to append to the environment variable path
     * @return the resolved absolute {@link Path}
     * @throws RuntimeException if the environment variable is not defined or empty
     */
    private static Path extractPathWithEnvVar(String envVarName, String... others){
        String javaHome = System.getenv(envVarName);

        if (javaHome == null || javaHome.isEmpty()) {
            throw new IllegalArgumentException("Environment variable does not seem to be defined: " + envVarName);
        }

        Path javaExecutable = Paths.get(javaHome, others);
        return javaExecutable.toAbsolutePath();
    }

    // --------------------------------------------------------------------
    // AsyncAPI / Kafka helpers used by EvoMaster-generated tests.
    //
    // These wrap kafka-clients so the generated test files stay readable
    // (one call per action instead of ~20 lines of producer/consumer
    // setup).  They are deliberately reflective so a project can pull in
    // EMTestUtils without taking a hard kafka-clients dep — the methods
    // throw IllegalStateException with a clear message if kafka-clients
    // is missing at runtime.
    //
    // WARNING: code generation in
    // core/.../output/service/AsyncAPITestCaseWriter.kt emits literal
    // calls to these methods.  Any signature change here must be
    // mirrored there.
    // --------------------------------------------------------------------

    /**
     * Publish a single record onto a Kafka topic.  Used by AsyncAPI-generated
     * tests as a one-liner replacement for opening a producer per action.
     *
     * @param bootstrapServers comma-separated `host:port` list
     * @param topic            destination topic (already rendered if it had channel-parameter placeholders)
     * @param key              partition-routing key, or null
     * @param payload          record value (typically the JSON-serialised AsyncAPI payload)
     * @param headers          additional message headers (correlation id + user-defined headers)
     */
    public static void kafkaPublish(
            String bootstrapServers,
            String topic,
            String key,
            byte[] payload,
            java.util.Map<String, byte[]> headers
    ) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
            props.put("acks", "all");
            try (org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> producer =
                         new org.apache.kafka.clients.producer.KafkaProducer<>(props)) {
                org.apache.kafka.clients.producer.ProducerRecord<String, byte[]> record =
                        new org.apache.kafka.clients.producer.ProducerRecord<>(topic, key, payload);
                if (headers != null) {
                    for (java.util.Map.Entry<String, byte[]> e : headers.entrySet()) {
                        record.headers().add(e.getKey(), e.getValue());
                    }
                }
                producer.send(record).get(5, java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (NoClassDefFoundError nf) {
            throw new IllegalStateException(
                    "kafka-clients not on the test classpath; add `org.apache.kafka:kafka-clients` to run AsyncAPI tests",
                    nf);
        } catch (Exception e) {
            throw new RuntimeException("kafkaPublish failed: " + e.getMessage(), e);
        }
    }

    /**
     * Subscribe to {@code topic} and return the first record whose
     * {@code correlationHeaderName} header equals {@code expectedCorrelationId}.
     * Returns {@code null} on timeout — the caller decides whether that's a
     * test failure or an expected fire-and-forget outcome.
     */
    public static byte[] kafkaAwaitReply(
            String bootstrapServers,
            String topic,
            String correlationHeaderName,
            String expectedCorrelationId,
            long timeoutMs
    ) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("group.id", "evm-test-" + java.util.UUID.randomUUID());
            props.put("auto.offset.reset", "latest");
            props.put("enable.auto.commit", "false");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            try (org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> consumer =
                         new org.apache.kafka.clients.consumer.KafkaConsumer<>(props)) {
                consumer.subscribe(java.util.Collections.singletonList(topic));
                long deadline = System.currentTimeMillis() + timeoutMs;
                while (System.currentTimeMillis() < deadline) {
                    long remaining = Math.max(50L, deadline - System.currentTimeMillis());
                    org.apache.kafka.clients.consumer.ConsumerRecords<String, byte[]> records =
                            consumer.poll(java.time.Duration.ofMillis(Math.min(remaining, 500L)));
                    for (org.apache.kafka.clients.consumer.ConsumerRecord<String, byte[]> r : records) {
                        if (correlationHeaderName == null || expectedCorrelationId == null) {
                            return r.value();
                        }
                        org.apache.kafka.common.header.Header h = r.headers().lastHeader(correlationHeaderName);
                        if (h != null && expectedCorrelationId.equals(new String(h.value(), java.nio.charset.StandardCharsets.UTF_8))) {
                            return r.value();
                        }
                    }
                }
                return null;
            }
        } catch (NoClassDefFoundError nf) {
            throw new IllegalStateException(
                    "kafka-clients not on the test classpath; add `org.apache.kafka:kafka-clients` to run AsyncAPI tests",
                    nf);
        } catch (Exception e) {
            throw new RuntimeException("kafkaAwaitReply failed: " + e.getMessage(), e);
        }
    }

    /**
     * Inspection helper for assertions on AsyncAPI reply payloads. Returns
     * true when {@code field} is a top-level property of the JSON object in
     * {@code reply}. Returns false when reply is null, not JSON, or not an
     * object. Used by per-field reply assertion oracles (M9-PR5).
     */
    public static boolean replyHas(byte[] reply, String field) {
        if (reply == null) return false;
        com.fasterxml.jackson.databind.JsonNode node = parseReply(reply);
        return node != null && node.isObject() && node.has(field);
    }

    /**
     * Inspection helper: return the textual value of {@code field} in the
     * JSON object {@code reply}, or null when absent / non-textual / parse
     * failure. Convenient for {@code assertEquals(Set.of(...), Set.of(...))}-
     * style assertions against an enum.
     */
    public static String replyText(byte[] reply, String field) {
        if (reply == null) return null;
        com.fasterxml.jackson.databind.JsonNode node = parseReply(reply);
        if (node == null || !node.isObject()) return null;
        com.fasterxml.jackson.databind.JsonNode v = node.get(field);
        if (v == null || !v.isTextual()) return null;
        return v.asText();
    }

    private static com.fasterxml.jackson.databind.JsonNode parseReply(byte[] reply) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(reply);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * Inspection helper: return the numeric value of {@code field} in the
     * JSON object {@code reply}, or null when absent / non-numeric / parse
     * failure. Used by generated MIN/MAX assertions.
     */
    public static Double replyNumber(byte[] reply, String field) {
        if (reply == null) return null;
        com.fasterxml.jackson.databind.JsonNode node = parseReply(reply);
        if (node == null || !node.isObject()) return null;
        com.fasterxml.jackson.databind.JsonNode v = node.get(field);
        if (v == null || !v.isNumber()) return null;
        return v.asDouble();
    }

    /**
     * Inspection helper: return the textual length of {@code field} in the
     * JSON object {@code reply}, or -1 when absent / non-textual / parse
     * failure. Used by generated MIN_LENGTH/MAX_LENGTH assertions; the
     * sentinel value of -1 (rather than null) lets callers compare with
     * a single primitive predicate.
     */
    public static int replyTextLength(byte[] reply, String field) {
        String s = replyText(reply, field);
        return s == null ? -1 : s.length();
    }

    /**
     * Inspection helper: best-effort check that {@code reply.field} matches
     * the JSON Schema {@code format} keyword. Returns true when the field
     * is absent (no constraint to violate), when the format is unknown to
     * this helper (fail-open), or when the textual value matches the
     * format's regex/parser. Returns false only on positive mismatch.
     *
     * Supported formats: {@code date}, {@code date-time}, {@code email},
     * {@code uuid}, {@code uri}, {@code ipv4}, {@code ipv6}. Other declared
     * formats pass through unchecked to avoid spurious test failures on
     * formats outside this helper's coverage.
     */
    public static boolean replyFormatMatches(byte[] reply, String field, String format) {
        String s = replyText(reply, field);
        if (s == null) return true;
        if (format == null) return true;
        switch (format) {
            case "date":
                return s.matches("\\d{4}-\\d{2}-\\d{2}");
            case "date-time":
                try {
                    java.time.OffsetDateTime.parse(s);
                    return true;
                } catch (java.time.format.DateTimeParseException e) {
                    try {
                        java.time.Instant.parse(s);
                        return true;
                    } catch (java.time.format.DateTimeParseException e2) {
                        return false;
                    }
                }
            case "email":
                return s.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
            case "uuid":
                try {
                    java.util.UUID.fromString(s);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            case "uri":
                try {
                    new java.net.URI(s);
                    return true;
                } catch (java.net.URISyntaxException e) {
                    return false;
                }
            case "ipv4":
                return s.matches("(\\d{1,3}\\.){3}\\d{1,3}");
            case "ipv6":
                return s.contains(":") && s.length() <= 39;
            default:
                return true;
        }
    }

    /**
     * Subscribe to {@code topic} and collect the payload bytes of every
     * record that arrives during the next {@code windowMs} millisecond
     * window. Returns an array (possibly empty); never null.
     *
     * Used by AsyncAPI output-observation tests (M9-PR4) to capture every
     * event the SUT emits on a SUT-produced channel and assert against
     * expected shapes. No correlation filtering — the caller filters
     * payloads itself.
     */
    public static byte[][] kafkaCollectAllWithin(
            String bootstrapServers,
            String topic,
            long windowMs
    ) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("group.id", "evm-test-" + java.util.UUID.randomUUID());
            props.put("auto.offset.reset", "latest");
            props.put("enable.auto.commit", "false");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            try (org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> consumer =
                         new org.apache.kafka.clients.consumer.KafkaConsumer<>(props)) {
                consumer.subscribe(java.util.Collections.singletonList(topic));
                java.util.List<byte[]> collected = new java.util.ArrayList<>();
                long deadline = System.currentTimeMillis() + windowMs;
                while (System.currentTimeMillis() < deadline) {
                    long remaining = Math.max(50L, deadline - System.currentTimeMillis());
                    org.apache.kafka.clients.consumer.ConsumerRecords<String, byte[]> records =
                            consumer.poll(java.time.Duration.ofMillis(Math.min(remaining, 500L)));
                    for (org.apache.kafka.clients.consumer.ConsumerRecord<String, byte[]> r : records) {
                        collected.add(r.value());
                    }
                }
                return collected.toArray(new byte[0][]);
            }
        } catch (NoClassDefFoundError nf) {
            throw new IllegalStateException(
                    "kafka-clients not on the test classpath; add `org.apache.kafka:kafka-clients` to run AsyncAPI tests",
                    nf);
        } catch (Exception e) {
            throw new RuntimeException("kafkaCollectAllWithin failed: " + e.getMessage(), e);
        }
    }
}
