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
}
