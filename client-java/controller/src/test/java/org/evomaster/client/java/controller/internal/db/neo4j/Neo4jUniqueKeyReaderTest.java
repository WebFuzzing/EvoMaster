package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jExistingDataDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityPropertyDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jUniqueKeyDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.evomaster.client.java.controller.neo4j.ReflectionBasedNeo4jClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jUniqueKeyReaderTest {

    private static final int NEO4J_BOLT_PORT = 7687;

    private static final GenericContainer<?> neo4jContainer = new GenericContainer<>("neo4j:5")
            .withExposedPorts(NEO4J_BOLT_PORT)
            .withEnv("NEO4J_AUTH", "none");

    private static Driver driver;
    private static ReflectionBasedNeo4jClient client;

    @BeforeAll
    public static void initClass() {
        neo4jContainer.start();
        String boltUrl = "bolt://" + neo4jContainer.getHost() + ":" + neo4jContainer.getMappedPort(NEO4J_BOLT_PORT);
        driver = GraphDatabase.driver(boltUrl, AuthTokens.none());
        driver.verifyConnectivity();
        client = new ReflectionBasedNeo4jClient(driver);
    }

    @AfterAll
    public static void closeClass() {
        if (driver != null) {
            driver.close();
        }
        neo4jContainer.stop();
    }

    @AfterEach
    public void cleanUp() {
        client.detachDeleteAll();
        dropAllConstraints();
    }

    @Test
    public void testReadsSingleAndCompositeUniquenessConstraintsWithTheirValues() {
        run("CREATE CONSTRAINT player_id IF NOT EXISTS FOR (n:Player) REQUIRE n.id IS UNIQUE");
        run("CREATE CONSTRAINT match_key IF NOT EXISTS FOR (n:Match) REQUIRE (n.white, n.black) IS UNIQUE");
        run("CREATE (:Player {id: 1}), (:Player {id: 'two'}), (:Player {name: 'no id'}), "
                + "(:Match {white: 'a', black: 'b'})");

        Neo4jExistingDataDto dto = new Neo4jUniqueKeyReader().read(client, Collections.emptyList());

        assertEquals(2, dto.uniqueKeys.size());

        Neo4jUniqueKeyDto player = find(dto, "Player");
        assertEquals(Collections.singletonList("id"), player.properties);
        assertEquals(Arrays.asList("INTEGER=1", "STRING=two"), render(player).stream().sorted().collect(Collectors.toList()));

        Neo4jUniqueKeyDto match = find(dto, "Match");
        assertEquals(Arrays.asList("white", "black"), match.properties);
        assertEquals(Collections.singletonList("STRING=a,STRING=b"), render(match));
    }

    @Test
    public void testEntityIdsAreKeysOnTheirPrimaryLabelEvenWithoutConstraints() {
        run("CREATE (:Adopter:Person {id: 'gQNkuJUNO_nF', name: 'Ana'}), (:Adopter {id: 'other'}), "
                + "(:Person {id: 'not an adopter'})");

        Neo4jExistingDataDto dto = new Neo4jUniqueKeyReader().read(client, Collections.singletonList(
                entity(Arrays.asList("Adopter", "Person"), property("id", true), property("name", false))));

        assertEquals(1, dto.uniqueKeys.size());
        Neo4jUniqueKeyDto adopter = find(dto, "Adopter");
        assertEquals(Collections.singletonList("id"), adopter.properties);
        assertEquals(Arrays.asList("STRING=gQNkuJUNO_nF", "STRING=other"),
                render(adopter).stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void testAKeyFromBothSourcesIsReportedOnce() {
        run("CREATE CONSTRAINT player_id IF NOT EXISTS FOR (n:Player) REQUIRE n.id IS UNIQUE");
        run("CREATE (:Player {id: 1.5})");

        Neo4jExistingDataDto dto = new Neo4jUniqueKeyReader().read(client, Collections.singletonList(
                entity(Collections.singletonList("Player"), property("id", true))));

        assertEquals(1, dto.uniqueKeys.size());
        assertEquals(Collections.singletonList("FLOAT=1.5"), render(find(dto, "Player")));
    }

    @Test
    public void testValuesOfTypesAnInsertionCannotHoldAreLeftOut() {
        run("CREATE (:Adopter {id: [1, 2]}), (:Adopter {id: date('2026-10-20')}), (:Adopter {id: true})");

        Neo4jExistingDataDto dto = new Neo4jUniqueKeyReader().read(client, Collections.singletonList(
                entity(Collections.singletonList("Adopter"), property("id", true))));

        assertEquals(Collections.singletonList("BOOLEAN=true"), render(find(dto, "Adopter")));
    }

    private static Neo4jUniqueKeyDto find(Neo4jExistingDataDto dto, String label) {
        return dto.uniqueKeys.stream().filter(k -> k.label.equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError("No key for " + label));
    }

    /** Each existing value as "TYPE=value" entries joined by commas. */
    private static List<String> render(Neo4jUniqueKeyDto key) {
        List<String> rendered = new ArrayList<>();
        for (List<Neo4jInsertionEntryDto> entries : key.existingValues) {
            rendered.add(entries.stream().map(e -> e.type + "=" + e.value).collect(Collectors.joining(",")));
        }
        return rendered;
    }

    private static Neo4jEntityDto entity(List<String> labels, Neo4jEntityPropertyDto... properties) {
        Neo4jEntityDto dto = new Neo4jEntityDto();
        dto.className = "Entity";
        dto.labels = new ArrayList<>(labels);
        dto.properties = new ArrayList<>(Arrays.asList(properties));
        return dto;
    }

    private static Neo4jEntityPropertyDto property(String name, boolean id) {
        Neo4jEntityPropertyDto dto = new Neo4jEntityPropertyDto();
        dto.name = name;
        dto.type = Neo4jPropertyTypeDto.STRING;
        dto.isId = id;
        dto.isGenerated = false;
        return dto;
    }

    private static void run(String cypher) {
        try (Session session = driver.session()) {
            session.run(cypher).consume();
        }
    }

    private static void dropAllConstraints() {
        try (Session session = driver.session()) {
            List<String> names = session.run("SHOW CONSTRAINTS YIELD name").list(r -> r.get("name").asString());
            for (String name : names) {
                session.run("DROP CONSTRAINT `" + name + "`").consume();
            }
        }
    }
}
