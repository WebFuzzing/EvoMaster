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
    /**
     * Lightweight DTO returned by {@link #kafkaAwaitReplyEnvelope}. The
     * payload field is the consumed message body; correlationId is the
     * value of the configured correlation header, or null when the schema
     * doesn't declare a correlation header or the message lacks one.
     *
     * Used by generated tests (M11-PR2 fix E) that want to assert the
     * correlation id on the actual reply rather than rely on the
     * upstream consumer to silently drop wrong-correlation messages.
     */
    public static final class ReplyEnvelope {
        public final byte[] payload;
        public final String correlationId;
        /**
         * All headers carried on the consumed message, decoded UTF-8.
         * Empty (not null) when the message had no headers. Populated by
         * {@link #kafkaAwaitReplyEnvelope} since M11-PR6 so the writer can
         * assert per-header facets (presence, enum, format, …) declared in
         * the AsyncAPI `headers:` schema.
         */
        public final java.util.Map<String, String> headers;
        public ReplyEnvelope(byte[] payload, String correlationId,
                              java.util.Map<String, String> headers) {
            this.payload = payload;
            this.correlationId = correlationId;
            this.headers = headers == null ? java.util.Collections.emptyMap() : headers;
        }
        // Back-compat constructor used by callers that haven't been updated
        // to pass headers. Will be removed once the writer-side wiring lands.
        public ReplyEnvelope(byte[] payload, String correlationId) {
            this(payload, correlationId, java.util.Collections.emptyMap());
        }
    }

    /**
     * Subscribe to {@code topic} and return the first message that arrives
     * during the {@code timeoutMs} window, *regardless of correlation
     * header*, packaged with whatever correlation value the message carries
     * (or null when the message has no correlation header).
     *
     * Differs from {@link #kafkaAwaitReply} in that the latter silently
     * filters out messages with mismatching correlation; this method returns
     * whatever arrives first so the caller can assert correlation
     * explicitly. Returns null only when no message arrived at all.
     */
    public static ReplyEnvelope kafkaAwaitReplyEnvelope(
            String bootstrapServers,
            String topic,
            String correlationHeaderName,
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
                        // Decode every header into a String→String map so
                        // generated tests can assert facets declared in the
                        // schema's `headers:` block. UTF-8 mirrors how
                        // EvoMaster encodes outbound headers in kafkaPublish.
                        java.util.Map<String, String> hmap = new java.util.LinkedHashMap<>();
                        for (org.apache.kafka.common.header.Header h : r.headers()) {
                            if (h.value() != null) {
                                hmap.put(h.key(), new String(h.value(), java.nio.charset.StandardCharsets.UTF_8));
                            }
                        }
                        String corr = null;
                        if (correlationHeaderName != null && !correlationHeaderName.isEmpty()) {
                            corr = hmap.get(correlationHeaderName);
                        }
                        return new ReplyEnvelope(r.value(), corr, hmap);
                    }
                }
                return null;
            }
        } catch (NoClassDefFoundError nf) {
            throw new IllegalStateException(
                    "kafka-clients not on the test classpath; add `org.apache.kafka:kafka-clients` to run AsyncAPI tests",
                    nf);
        } catch (Exception e) {
            throw new RuntimeException("kafkaAwaitReplyEnvelope failed: " + e.getMessage(), e);
        }
    }

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
        if (node == null) return false;
        com.fasterxml.jackson.databind.JsonNode walked = walkDottedPath(node, field);
        return walked != null && !walked.isMissingNode() && !walked.isNull();
    }

    /**
     * Walk a dotted path (`comment.author.name`) into a JSON node, returning
     * null when any intermediate hop is missing or not an object. Used by
     * the path-aware reply / output assertion helpers (M11-PR2 fix A).
     */
    private static com.fasterxml.jackson.databind.JsonNode walkDottedPath(
            com.fasterxml.jackson.databind.JsonNode root,
            String path
    ) {
        if (root == null || path == null) return null;
        com.fasterxml.jackson.databind.JsonNode cur = root;
        for (String segment : path.split("\\.")) {
            if (cur == null) return null;
            if (cur.isObject()) {
                cur = cur.get(segment);
            } else {
                return null;
            }
        }
        return cur;
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
        if (node == null) return null;
        com.fasterxml.jackson.databind.JsonNode v = walkDottedPath(node, field);
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
        if (node == null) return null;
        com.fasterxml.jackson.databind.JsonNode v = walkDottedPath(node, field);
        if (v == null || !v.isNumber()) return null;
        return v.asDouble();
    }

    /**
     * Inspection helper: return the array-length of {@code field} in the
     * JSON object {@code reply}, or -1 when absent / non-array / parse
     * failure. Used by ARRAY_MIN_ITEMS / ARRAY_MAX_ITEMS assertions
     * (M11-PR2 fix B).
     */
    public static int replyArrayLength(byte[] reply, String field) {
        if (reply == null) return -1;
        com.fasterxml.jackson.databind.JsonNode node = parseReply(reply);
        if (node == null) return -1;
        com.fasterxml.jackson.databind.JsonNode v = walkDottedPath(node, field);
        if (v == null || !v.isArray()) return -1;
        return v.size();
    }

    /**
     * Inspection helper: check whether the array {@code field} in the JSON
     * object {@code reply} contains only distinct elements (compared by
     * Jackson's textual representation). Returns true when the field is
     * absent or not an array (fail-open). Used by ARRAY_UNIQUE assertions.
     */
    public static boolean replyArrayUnique(byte[] reply, String field) {
        if (reply == null) return true;
        com.fasterxml.jackson.databind.JsonNode node = parseReply(reply);
        if (node == null) return true;
        com.fasterxml.jackson.databind.JsonNode v = walkDottedPath(node, field);
        if (v == null || !v.isArray()) return true;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (com.fasterxml.jackson.databind.JsonNode e : v) {
            if (!seen.add(e.toString())) return false;
        }
        return true;
    }

    /**
     * Inspection helper: check whether the textual value of {@code field}
     * matches the regex {@code pattern}. Returns true when the field is
     * absent (fail-open). Returns false on pattern compile errors so
     * the caller surfaces a clear assertion failure rather than silently
     * passing on a malformed schema-supplied regex. Used by PATTERN
     * assertions (M11-PR2 fix B).
     */
    public static boolean replyPatternMatches(byte[] reply, String field, String pattern) {
        String s = replyText(reply, field);
        if (s == null) return true;
        if (pattern == null) return true;
        try {
            return java.util.regex.Pattern.compile(pattern).matcher(s).find();
        } catch (java.util.regex.PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Inspection helper: check whether {@code field}'s numeric value is a
     * multiple of {@code divisor} (within a small epsilon for IEEE 754
     * imprecision). Returns true when the field is absent (fail-open).
     */
    // ----- Header-side assertion helpers (M11-PR6) ----------------------
    // Mirror the payload assertion helpers but operate on the raw
    // String→String header map exposed by ReplyEnvelope.headers.
    // Fail-open on missing headers so a single absent header surfaces as
    // exactly one REQUIRED-style violation rather than a cascade.

    public static boolean replyHeaderHas(java.util.Map<String, String> headers, String name) {
        return headers != null && headers.containsKey(name);
    }

    public static String replyHeaderText(java.util.Map<String, String> headers, String name) {
        return headers == null ? null : headers.get(name);
    }

    public static Double replyHeaderNumber(java.util.Map<String, String> headers, String name) {
        String v = replyHeaderText(headers, name);
        if (v == null) return null;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return null; }
    }

    public static int replyHeaderTextLength(java.util.Map<String, String> headers, String name) {
        String v = replyHeaderText(headers, name);
        return v == null ? -1 : v.length();
    }

    public static boolean replyHeaderFormatMatches(java.util.Map<String, String> headers,
                                                    String name, String format) {
        String v = replyHeaderText(headers, name);
        if (v == null) return true;
        // Delegate to the payload-side format checker — same set of formats
        // supported (date / date-time / email / uuid / uri / ipv4 / ipv6).
        return formatMatches(v, format);
    }

    public static boolean replyHeaderPatternMatches(java.util.Map<String, String> headers,
                                                     String name, String pattern) {
        String v = replyHeaderText(headers, name);
        if (v == null) return true;
        if (pattern == null) return true;
        try {
            return java.util.regex.Pattern.compile(pattern).matcher(v).find();
        } catch (java.util.regex.PatternSyntaxException e) {
            return false;
        }
    }

    private static boolean formatMatches(String s, String format) {
        // Implementation shared between replyFormatMatches and
        // replyHeaderFormatMatches. Extracted so the header path doesn't
        // require parsing the payload as JSON just to validate a header.
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

    public static boolean replyMultipleOf(byte[] reply, String field, double divisor) {
        Double v = replyNumber(reply, field);
        if (v == null) return true;
        if (divisor == 0.0) return false;
        double remainder = Math.IEEEremainder(v, divisor);
        return Math.abs(remainder) < 1e-9;
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
        return formatMatches(s, format);
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

    // ----- WebSocket helpers (M11-PR8) -----------------------------------
    //
    // Mirror the kafka* helpers for WebSocket transport. Implemented via
    // reflection against `java.net.http.WebSocket` so this file keeps
    // compiling under JDK 8 (the test-utils-java module's source level);
    // runtime requires JDK 11+, which is also the minimum for kafka-clients
    // 3.x so AsyncAPI-generated tests don't gain a new floor.
    //
    // Headers are encoded into a JSON envelope `{ "headers": {...},
    // "payload": "..." }` because raw WebSocket frames have no native
    // headers. Receive-side decoders detect the envelope shape and fall
    // back to treating the entire frame as the payload when absent, so
    // servers that don't know about the envelope still round-trip.

    private static java.net.URI resolveWsUri(String wsBase, String channel) {
        if (channel.startsWith("ws://") || channel.startsWith("wss://")) {
            return java.net.URI.create(channel);
        }
        String origin = wsBase;
        while (origin.endsWith("/")) origin = origin.substring(0, origin.length() - 1);
        String suffix = channel.startsWith("/") ? channel : "/" + channel;
        return java.net.URI.create(origin + suffix);
    }

    private static String encodeWsFrame(byte[] payload, java.util.Map<String, byte[]> headers) {
        if (headers == null || headers.isEmpty()) {
            return new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"headers\":{");
        boolean first = true;
        for (java.util.Map.Entry<String, byte[]> e : headers.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(jsonEscape(e.getKey())).append("\":\"")
                    .append(jsonEscape(new String(e.getValue(), java.nio.charset.StandardCharsets.UTF_8)))
                    .append('"');
        }
        sb.append("},\"payload\":\"")
                .append(jsonEscape(new String(payload, java.nio.charset.StandardCharsets.UTF_8)))
                .append("\"}");
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Result of {@link #webSocketAwaitFrame}: the raw text payload of the
     * received frame and any headers that were carried in the JSON
     * envelope. {@code headers} is empty (not null) when the frame is a
     * bare-text payload that did not use the envelope shape.
     */
    public static final class WsFrame {
        public final byte[] payload;
        public final java.util.Map<String, String> headers;
        public WsFrame(byte[] payload, java.util.Map<String, String> headers) {
            this.payload = payload;
            this.headers = headers == null ? java.util.Collections.emptyMap() : headers;
        }
    }

    private static WsFrame decodeWsFrame(byte[] frame) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(frame);
            if (node != null && node.isObject() && node.has("headers") && node.has("payload")) {
                java.util.Map<String, String> hMap = new java.util.LinkedHashMap<>();
                node.get("headers").fields().forEachRemaining(e -> {
                    if (e.getValue().isTextual()) hMap.put(e.getKey(), e.getValue().asText());
                });
                byte[] payload = node.get("payload").asText()
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                return new WsFrame(payload, hMap);
            }
        } catch (Exception ignored) {
            // fall through to raw-payload return
        }
        return new WsFrame(frame, java.util.Collections.emptyMap());
    }

    /**
     * Reflectively obtain `java.net.http.HttpClient.newWebSocketBuilder()
     * .buildAsync(uri, listener).get()`. Returns the live `WebSocket`
     * instance opaquely typed as Object. Throws an IllegalStateException
     * with a clear message when the JDK does not expose
     * java.net.http.WebSocket (i.e. JDK 8 / 9 / 10 runtime).
     */
    private static Object openWebSocket(java.net.URI uri, java.util.Queue<byte[]> incoming) {
        try {
            Class<?> wsListenerClass = Class.forName("java.net.http.WebSocket$Listener");
            // Build a dynamic proxy that captures TEXT frames into `incoming`.
            final StringBuilder partial = new StringBuilder();
            java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if ("onOpen".equals(name)) {
                    args[0].getClass().getMethod("request", long.class).invoke(args[0], 1L);
                    return null;
                }
                if ("onText".equals(name)) {
                    Object socket = args[0];
                    CharSequence data = (CharSequence) args[1];
                    boolean last = (Boolean) args[2];
                    partial.append(data);
                    if (last) {
                        synchronized (incoming) {
                            incoming.add(partial.toString()
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            incoming.notifyAll();
                        }
                        partial.setLength(0);
                    }
                    socket.getClass().getMethod("request", long.class).invoke(socket, 1L);
                    return null;
                }
                if ("onClose".equals(name) || "onError".equals(name)) {
                    synchronized (incoming) { incoming.notifyAll(); }
                    return null;
                }
                // onPing / onPong / onBinary not used by AsyncAPI helpers;
                // returning null is the default contract.
                return null;
            };
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    wsListenerClass.getClassLoader(),
                    new Class<?>[]{wsListenerClass},
                    handler
            );
            Class<?> httpClientClass = Class.forName("java.net.http.HttpClient");
            Object client = httpClientClass.getMethod("newHttpClient").invoke(null);
            Object builder = httpClientClass.getMethod("newWebSocketBuilder").invoke(client);
            Object cf = builder.getClass()
                    .getMethod("buildAsync", java.net.URI.class, wsListenerClass)
                    .invoke(builder, uri, listener);
            return ((java.util.concurrent.CompletableFuture<?>) cf)
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (ClassNotFoundException notFound) {
            throw new IllegalStateException(
                    "WebSocket AsyncAPI helpers require JDK 11+ at test runtime; current JDK lacks java.net.http.WebSocket",
                    notFound
            );
        } catch (Exception e) {
            throw new RuntimeException("WebSocket open to " + uri + " failed: " + e.getMessage(), e);
        }
    }

    private static void sendText(Object webSocket, String text) {
        try {
            Object cf = webSocket.getClass()
                    .getMethod("sendText", CharSequence.class, boolean.class)
                    .invoke(webSocket, text, true);
            ((java.util.concurrent.CompletableFuture<?>) cf)
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("WebSocket sendText failed: " + e.getMessage(), e);
        }
    }

    private static void closeQuietly(Object webSocket) {
        if (webSocket == null) return;
        try {
            Object cf = webSocket.getClass()
                    .getMethod("sendClose", int.class, String.class)
                    .invoke(webSocket, 1000 /* NORMAL_CLOSURE */, "done");
            ((java.util.concurrent.CompletableFuture<?>) cf)
                    .get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // best-effort close
        }
    }

    /**
     * Open a WebSocket connection, send {@code payload} as a single TEXT
     * frame, and close. Mirrors {@link #kafkaPublish} so the writer can
     * target either transport interchangeably. Headers are JSON-enveloped
     * when present (servers that ignore the envelope still see a
     * JSON-shaped TEXT frame).
     */
    public static void webSocketPublish(
            String wsBase,
            String channel,
            byte[] payload,
            java.util.Map<String, byte[]> headers
    ) {
        java.util.Queue<byte[]> sink = new java.util.LinkedList<>();
        Object ws = openWebSocket(resolveWsUri(wsBase, channel), sink);
        try {
            sendText(ws, encodeWsFrame(payload, headers));
        } finally {
            closeQuietly(ws);
        }
    }

    /**
     * Open a WebSocket connection and return the first frame that arrives
     * within {@code timeoutMs}, or null on timeout. Equivalent contract
     * to {@link #kafkaAwaitReply} but over WS. Closes the connection on
     * exit (success or timeout).
     */
    public static WsFrame webSocketAwaitFrame(
            String wsBase,
            String channel,
            long timeoutMs
    ) {
        java.util.LinkedList<byte[]> queue = new java.util.LinkedList<>();
        Object ws = openWebSocket(resolveWsUri(wsBase, channel), queue);
        try {
            long deadline = System.currentTimeMillis() + timeoutMs;
            synchronized (queue) {
                while (queue.isEmpty()) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) return null;
                    queue.wait(remaining);
                }
                return decodeWsFrame(queue.poll());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("webSocketAwaitFrame interrupted", ie);
        } finally {
            closeQuietly(ws);
        }
    }

    /**
     * Open a WebSocket connection, leave it open for {@code windowMs}, and
     * return every TEXT frame that arrived during the window (in order).
     * Equivalent to {@link #kafkaCollectAllWithin}.
     */
    public static byte[][] webSocketCollectAllWithin(
            String wsBase,
            String channel,
            long windowMs
    ) {
        java.util.LinkedList<byte[]> queue = new java.util.LinkedList<>();
        Object ws = openWebSocket(resolveWsUri(wsBase, channel), queue);
        try {
            Thread.sleep(windowMs);
            synchronized (queue) {
                return queue.toArray(new byte[0][]);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("webSocketCollectAllWithin interrupted", ie);
        } finally {
            closeQuietly(ws);
        }
    }

    // ----- AMQP 0-9-1 helpers (M11-PR9) ----------------------------------
    //
    // Mirror the kafka* helpers but talk AMQP 0-9-1 via the RabbitMQ
    // amqp-client. Each call opens a fresh connection + channel,
    // performs the I/O, and closes them — short-lived sessions keep the
    // per-test surface as simple as the Kafka helpers do.
    //
    // The AsyncAPI binding model is mapped to the simplest AMQP shape:
    // the channel address is treated as the routing key on the default
    // exchange (""). Named-exchange routing with declared bindings is a
    // follow-up; the bookworm-family SUTs in the validation corpus all
    // use the default-exchange-as-queue pattern.
    //
    // `amqpBase` accepts either a full
    // `amqp://user:pass@host:port/vhost` URI or a bare `host:port`
    // (treated as `amqp://host:port`).

    private static java.net.URI resolveAmqpUri(String amqpBase) {
        String trimmed = amqpBase == null ? "" : amqpBase.trim();
        if (trimmed.startsWith("amqp://") || trimmed.startsWith("amqps://")) {
            return java.net.URI.create(trimmed);
        }
        return java.net.URI.create("amqp://" + trimmed);
    }

    private static com.rabbitmq.client.Connection openAmqp(String amqpBase) {
        try {
            com.rabbitmq.client.ConnectionFactory factory =
                    new com.rabbitmq.client.ConnectionFactory();
            factory.setUri(resolveAmqpUri(amqpBase));
            return factory.newConnection("evomaster-asyncapi-test");
        } catch (NoClassDefFoundError nf) {
            throw new IllegalStateException(
                    "com.rabbitmq:amqp-client not on the test classpath; add it to run AMQP-bound AsyncAPI tests",
                    nf
            );
        } catch (Exception e) {
            throw new RuntimeException("AMQP openConnection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Publish a single AMQP message to the default exchange with
     * {@code channel} as the routing key. Headers go into the message's
     * header table (UTF-8 strings). Mirrors {@link #kafkaPublish}.
     */
    public static void amqpPublish(
            String amqpBase,
            String channel,
            byte[] payload,
            java.util.Map<String, byte[]> headers
    ) {
        com.rabbitmq.client.Connection conn = openAmqp(amqpBase);
        try (com.rabbitmq.client.Channel ch = conn.createChannel()) {
            java.util.Map<String, Object> headerTable = new java.util.LinkedHashMap<>();
            if (headers != null) {
                for (java.util.Map.Entry<String, byte[]> e : headers.entrySet()) {
                    headerTable.put(e.getKey(),
                            new String(e.getValue(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            com.rabbitmq.client.AMQP.BasicProperties props =
                    new com.rabbitmq.client.AMQP.BasicProperties.Builder()
                            .contentType("application/json")
                            .headers(headerTable)
                            .build();
            ch.basicPublish("", channel, props, payload);
        } catch (Exception e) {
            throw new RuntimeException("amqpPublish failed: " + e.getMessage(), e);
        } finally {
            try { conn.close(2_000); } catch (Exception ignored) { /* best-effort */ }
        }
    }

    /**
     * Lightweight DTO returned by {@link #amqpAwaitReplyEnvelope}.
     * Mirrors {@link ReplyEnvelope}: payload + correlation id read from
     * the {@code correlationHeaderName} header (when supplied) + the
     * full decoded header map.
     */
    public static final class AmqpReplyEnvelope {
        public final byte[] payload;
        public final String correlationId;
        public final java.util.Map<String, String> headers;
        public AmqpReplyEnvelope(byte[] payload, String correlationId,
                                  java.util.Map<String, String> headers) {
            this.payload = payload;
            this.correlationId = correlationId;
            this.headers = headers == null ? java.util.Collections.emptyMap() : headers;
        }
    }

    /**
     * Subscribe to the queue named {@code channel} (declared on the
     * default exchange), block up to {@code timeoutMs}, and return the
     * first message wrapped in an {@link AmqpReplyEnvelope}. Returns
     * null on timeout. Mirrors {@link #kafkaAwaitReplyEnvelope}.
     */
    public static AmqpReplyEnvelope amqpAwaitReplyEnvelope(
            String amqpBase,
            String channel,
            String correlationHeaderName,
            long timeoutMs
    ) {
        com.rabbitmq.client.Connection conn = openAmqp(amqpBase);
        try (com.rabbitmq.client.Channel ch = conn.createChannel()) {
            try { ch.queueDeclare(channel, false, false, true, null); }
            catch (Exception ignored) { /* queue may already exist with different props */ }
            final java.util.concurrent.BlockingQueue<AmqpReplyEnvelope> queue =
                    new java.util.concurrent.LinkedBlockingQueue<>();
            final String consumerTag = ch.basicConsume(channel, true,
                new com.rabbitmq.client.DefaultConsumer(ch) {
                    @Override
                    public void handleDelivery(String tag,
                                                com.rabbitmq.client.Envelope env,
                                                com.rabbitmq.client.AMQP.BasicProperties props,
                                                byte[] body) {
                        java.util.Map<String, String> hMap = new java.util.LinkedHashMap<>();
                        if (props.getHeaders() != null) {
                            for (java.util.Map.Entry<String, Object> e : props.getHeaders().entrySet()) {
                                if (e.getValue() != null) hMap.put(e.getKey(), e.getValue().toString());
                            }
                        }
                        String corr = (correlationHeaderName != null && !correlationHeaderName.isEmpty())
                                ? hMap.get(correlationHeaderName)
                                : null;
                        queue.offer(new AmqpReplyEnvelope(body, corr, hMap));
                    }
                });
            try {
                return queue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            } finally {
                try { ch.basicCancel(consumerTag); } catch (Exception ignored) { /* best-effort */ }
            }
        } catch (Exception e) {
            throw new RuntimeException("amqpAwaitReplyEnvelope failed: " + e.getMessage(), e);
        } finally {
            try { conn.close(2_000); } catch (Exception ignored) { /* best-effort */ }
        }
    }

    /**
     * Bare-payload version of {@link #amqpAwaitReplyEnvelope}. Returns
     * the message body (or null on timeout). Mirrors
     * {@link #kafkaAwaitReply}.
     */
    public static byte[] amqpAwaitReply(
            String amqpBase,
            String channel,
            String correlationHeaderName,
            String expectedCorrelationId,
            long timeoutMs
    ) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) return null;
            AmqpReplyEnvelope env = amqpAwaitReplyEnvelope(amqpBase, channel,
                    correlationHeaderName, remaining);
            if (env == null) return null;
            if (correlationHeaderName == null || correlationHeaderName.isEmpty()
                    || expectedCorrelationId == null
                    || expectedCorrelationId.equals(env.correlationId)) {
                return env.payload;
            }
            // wrong correlation; keep polling for the remaining window
        }
        return null;
    }

    /**
     * Subscribe to the queue named {@code channel} and collect every
     * message that arrives during the next {@code windowMs} window.
     * Mirrors {@link #kafkaCollectAllWithin}.
     */
    public static byte[][] amqpCollectAllWithin(
            String amqpBase,
            String channel,
            long windowMs
    ) {
        com.rabbitmq.client.Connection conn = openAmqp(amqpBase);
        try (com.rabbitmq.client.Channel ch = conn.createChannel()) {
            try { ch.queueDeclare(channel, false, false, true, null); }
            catch (Exception ignored) { /* queue may already exist */ }
            final java.util.List<byte[]> collected =
                    java.util.Collections.synchronizedList(new java.util.ArrayList<>());
            String consumerTag = ch.basicConsume(channel, true,
                new com.rabbitmq.client.DefaultConsumer(ch) {
                    @Override
                    public void handleDelivery(String tag,
                                                com.rabbitmq.client.Envelope env,
                                                com.rabbitmq.client.AMQP.BasicProperties props,
                                                byte[] body) {
                        collected.add(body);
                    }
                });
            try {
                Thread.sleep(windowMs);
                synchronized (collected) {
                    return collected.toArray(new byte[0][]);
                }
            } finally {
                try { ch.basicCancel(consumerTag); } catch (Exception ignored) { /* best-effort */ }
            }
        } catch (Exception e) {
            throw new RuntimeException("amqpCollectAllWithin failed: " + e.getMessage(), e);
        } finally {
            try { conn.close(2_000); } catch (Exception ignored) { /* best-effort */ }
        }
    }
}
