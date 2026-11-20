package org.evomaster.client.java.instrumentation.dynamodb;

import org.junit.jupiter.api.Test;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.DynamoDbEnhancedClientClassReplacement;
import org.evomaster.client.java.utils.SimpleLogger;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.BatchGetItemOperation;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.regions.Region;
import java.net.URI;
import java.time.*;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OptionalAttributeConverter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises schema and empty enhanced-batch boundaries without requiring existing rows. */
public class DynamoDbEnhancedSchemaTest {
    /** A world cup event with an instant sort key. */
    @DynamoDbBean
    public static class Match {
        private String country;
        private Instant kickoff;
        /** Returns the partition key. */
        @DynamoDbPartitionKey public String getCountry() { return country; }
        /** Sets the partition key. */
        public void setCountry(String value) { country = value; }
        /** Returns the timestamp sort key. */
        @DynamoDbSortKey public Instant getKickoff() { return kickoff; }
        /** Sets the timestamp sort key. */
        public void setKickoff(Instant value) { kickoff = value; }
    }

    /** Bean exercising all supported built-in converters alongside plain and deferred types. */
    @DynamoDbBean
    public static class TournamentSchedule {
        private UUID tournamentId;
        private Instant createdAt;
        private OffsetDateTime updatedAt;
        private ZonedDateTime broadcastAt;
        private LocalDateTime gatesOpenAt;
        private LocalDate matchDate;
        private LocalTime kickoffTime;
        private String venue;
        private URI website;

        @DynamoDbPartitionKey public UUID getTournamentId() { return tournamentId; }
        public void setTournamentId(UUID value) { tournamentId = value; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant value) { createdAt = value; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime value) { updatedAt = value; }
        public ZonedDateTime getBroadcastAt() { return broadcastAt; }
        public void setBroadcastAt(ZonedDateTime value) { broadcastAt = value; }
        public LocalDateTime getGatesOpenAt() { return gatesOpenAt; }
        public void setGatesOpenAt(LocalDateTime value) { gatesOpenAt = value; }
        public LocalDate getMatchDate() { return matchDate; }
        public void setMatchDate(LocalDate value) { matchDate = value; }
        public LocalTime getKickoffTime() { return kickoffTime; }
        public void setKickoffTime(LocalTime value) { kickoffTime = value; }
        public String getVenue() { return venue; }
        public void setVenue(String value) { venue = value; }
        public URI getWebsite() { return website; }
        public void setWebsite(URI value) { website = value; }
    }

    /** Converter information is available before the first row exists. */
    @Test
    public void timestampConverterIsAvailableForEmptyTables() {
        try (DynamoDbClient client = DynamoDbClient.builder().region(Region.US_EAST_1).build()) {
            DynamoDbEnhancedClientClassReplacement.Sync.table(
                    DynamoDbEnhancedClient.builder().dynamoDbClient(client).build(),
                    "WorldCupMatches", TableSchema.fromBean(Match.class));
        }
        assertEquals(Collections.singletonMap("kickoff", DynamoDbAttributeFormat.DATE_TIME),
                DynamoDbSchemaRegistry.getFormats("WorldCupMatches"));
    }

    /** Common date/time and UUID converters are recorded while plain and deferred types keep scalar fallback. */
    @Test
    public void commonBuiltInConvertersAreMappedToExpectedFormats() {
        DynamoDbSchemaRegistry.register("TournamentSchedules", TableSchema.fromBean(TournamentSchedule.class));

        Map<String, DynamoDbAttributeFormat> expected = new LinkedHashMap<>();
        expected.put("tournamentId", DynamoDbAttributeFormat.UUID);
        expected.put("createdAt", DynamoDbAttributeFormat.DATE_TIME);
        expected.put("updatedAt", DynamoDbAttributeFormat.DATE_TIME);
        expected.put("broadcastAt", DynamoDbAttributeFormat.DATE_TIME);
        expected.put("gatesOpenAt", DynamoDbAttributeFormat.LOCAL_DATE_TIME);
        expected.put("matchDate", DynamoDbAttributeFormat.DATE);
        expected.put("kickoffTime", DynamoDbAttributeFormat.LOCAL_TIME);

        assertEquals(expected, DynamoDbSchemaRegistry.getFormats("TournamentSchedules"));
    }

    /** Unavailable semantic metadata retains the ordinary scalar fallback. */
    @Test
    public void unavailableConvertersDoNotInventFormats() {
        DynamoDbSchemaRegistry.register("WorldCupTeams", new Object());
        assertTrue(DynamoDbSchemaRegistry.getFormats("WorldCupTeams").isEmpty());
    }

    /** Document schemas without converter support log a warning and retain the scalar fallback. */
    @Test
    public void documentSchemaWithoutConverterSupportLogsWarningAndRetainsFallback() {
        TableSchema<?> documentSchema = TableSchema.documentSchemaBuilder()
                .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id", AttributeValueType.S)
                .build();
        SimpleLogger.Level previousThreshold = SimpleLogger.getThreshold();
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            SimpleLogger.setThreshold(SimpleLogger.Level.INFO);
            System.setOut(new PrintStream(captured));
            DynamoDbSchemaRegistry.register("WorldCupDocuments", documentSchema);
        } finally {
            System.setOut(originalOut);
            SimpleLogger.setThreshold(previousThreshold);
        }
        assertTrue(DynamoDbSchemaRegistry.getFormats("WorldCupDocuments").isEmpty());
        assertTrue(captured.toString().contains("WorldCupDocuments"));
        assertTrue(captured.toString().contains("UnsupportedOperationException"));
    }

    /** An empty application key list produces a null KeysAndAttributes, not lost valid keys. */
    @Test
    public void emptyApplicationReadBatchSurvivesTransport() throws Exception {
        try (DynamoDbClient client = DynamoDbClient.builder().region(Region.US_EAST_1).build()) {
            DynamoDbTable<Match> table = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()
                    .table("WorldCupMatches", TableSchema.fromBean(Match.class));
            ReadBatch empty = ReadBatch.builder(Match.class).mappedTableResource(table).build();
            assertNull(empty.keysAndAttributes());
            Object request = BatchGetItemOperation.create(BatchGetItemEnhancedRequest.builder()
                    .readBatches(empty).build()).generateRequest(null);
            DynamoDbQuery query = DynamoDbQuery.from(request);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(query); }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                DynamoDbQuery transported = (DynamoDbQuery) in.readObject();
                assertTrue(transported.batchRequestItems().get("WorldCupMatches").keys().isEmpty());
            }
        }
    }

    @DynamoDbBean
    public static class NumericVariantsEntity {
        private byte primitiveByte;
        private Byte boxedByte;
        private short primitiveShort;
        private Short boxedShort;
        private int primitiveInt;
        private Integer boxedInt;
        private AtomicInteger atomicInt;
        private long primitiveLong;
        private Long boxedLong;
        private AtomicLong atomicLong;
        private BigInteger bigInteger;
        private float primitiveFloat;
        private Float boxedFloat;
        private double primitiveDouble;
        private Double boxedDouble;
        private BigDecimal bigDecimal;

        @DynamoDbPartitionKey public byte getPrimitiveByte() { return primitiveByte; }
        public void setPrimitiveByte(byte v) { primitiveByte = v; }
        public Byte getBoxedByte() { return boxedByte; }
        public void setBoxedByte(Byte v) { boxedByte = v; }
        public short getPrimitiveShort() { return primitiveShort; }
        public void setPrimitiveShort(short v) { primitiveShort = v; }
        public Short getBoxedShort() { return boxedShort; }
        public void setBoxedShort(Short v) { boxedShort = v; }
        public int getPrimitiveInt() { return primitiveInt; }
        public void setPrimitiveInt(int v) { primitiveInt = v; }
        public Integer getBoxedInt() { return boxedInt; }
        public void setBoxedInt(Integer v) { boxedInt = v; }
        public AtomicInteger getAtomicInt() { return atomicInt; }
        public void setAtomicInt(AtomicInteger v) { atomicInt = v; }
        public long getPrimitiveLong() { return primitiveLong; }
        public void setPrimitiveLong(long v) { primitiveLong = v; }
        public Long getBoxedLong() { return boxedLong; }
        public void setBoxedLong(Long v) { boxedLong = v; }
        public AtomicLong getAtomicLong() { return atomicLong; }
        public void setAtomicLong(AtomicLong v) { atomicLong = v; }
        public BigInteger getBigInteger() { return bigInteger; }
        public void setBigInteger(BigInteger v) { bigInteger = v; }
        public float getPrimitiveFloat() { return primitiveFloat; }
        public void setPrimitiveFloat(float v) { primitiveFloat = v; }
        public Float getBoxedFloat() { return boxedFloat; }
        public void setBoxedFloat(Float v) { boxedFloat = v; }
        public double getPrimitiveDouble() { return primitiveDouble; }
        public void setPrimitiveDouble(double v) { primitiveDouble = v; }
        public Double getBoxedDouble() { return boxedDouble; }
        public void setBoxedDouble(Double v) { boxedDouble = v; }
        public BigDecimal getBigDecimal() { return bigDecimal; }
        public void setBigDecimal(BigDecimal v) { bigDecimal = v; }
    }

    @Test
    public void optionalAndNumericConvertersRecordExpectedFormats() {
        TableSchema<TournamentSchedule> optionalSchema = TableSchema.builder(TournamentSchedule.class)
                .newItemSupplier(TournamentSchedule::new)
                .addAttribute(UUID.class, a -> a.name("tournamentId")
                        .getter(TournamentSchedule::getTournamentId).setter(TournamentSchedule::setTournamentId)
                        .tags(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey()))
                .addAttribute(new EnhancedType<Optional<Instant>>() {}, a -> a.name("optionalCreatedAt")
                        .getter(e -> Optional.ofNullable(e.getCreatedAt()))
                        .setter((e, v) -> e.setCreatedAt(v.orElse(null)))
                        .attributeConverter(OptionalAttributeConverter.create(InstantAsStringAttributeConverter.create())))
                .build();
        DynamoDbSchemaRegistry.register("OptionalTournamentEntries", optionalSchema);
        assertEquals(DynamoDbAttributeFormat.DATE_TIME,
                DynamoDbSchemaRegistry.getFormats("OptionalTournamentEntries").get("optionalCreatedAt"));

        DynamoDbSchemaRegistry.register("NumericVariantsTable", TableSchema.fromBean(NumericVariantsEntity.class));
        Map<String, DynamoDbAttributeFormat> formats = DynamoDbSchemaRegistry.getFormats("NumericVariantsTable");
        assertEquals(DynamoDbAttributeFormat.INT8, formats.get("primitiveByte"));
        assertEquals(DynamoDbAttributeFormat.INT8, formats.get("boxedByte"));
        assertEquals(DynamoDbAttributeFormat.INT16, formats.get("primitiveShort"));
        assertEquals(DynamoDbAttributeFormat.INT16, formats.get("boxedShort"));
        assertEquals(DynamoDbAttributeFormat.INT32, formats.get("primitiveInt"));
        assertEquals(DynamoDbAttributeFormat.INT32, formats.get("boxedInt"));
        assertEquals(DynamoDbAttributeFormat.INT32, formats.get("atomicInt"));
        assertEquals(DynamoDbAttributeFormat.INT64, formats.get("primitiveLong"));
        assertEquals(DynamoDbAttributeFormat.INT64, formats.get("boxedLong"));
        assertEquals(DynamoDbAttributeFormat.INT64, formats.get("atomicLong"));
        assertEquals(DynamoDbAttributeFormat.INTEGER, formats.get("bigInteger"));
        assertFalse(formats.containsKey("primitiveFloat"));
        assertFalse(formats.containsKey("boxedFloat"));
        assertFalse(formats.containsKey("primitiveDouble"));
        assertFalse(formats.containsKey("boxedDouble"));
        assertFalse(formats.containsKey("bigDecimal"));
    }
}
