package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.Neo4jNodeSchema;
import org.evomaster.client.java.instrumentation.Neo4jRelationshipSchema;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.testcontainers.containers.GenericContainer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4jTemplateClassReplacementTest {

    private static Driver driver;
    private static Neo4jTemplate neo4jTemplate;
    private static final int NEO4J_BOLT_PORT = 7687;

    private static final GenericContainer<?> neo4j = new GenericContainer<>("neo4j:5")
            .withExposedPorts(NEO4J_BOLT_PORT)
            .withEnv("NEO4J_AUTH", "none");

    @BeforeAll
    public static void initNeo4jClient() {
        neo4j.start();

        String boltUrl = "bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT);
        driver = GraphDatabase.driver(boltUrl, AuthTokens.none());
        driver.verifyConnectivity();

        Neo4jClient neo4jClient = Neo4jClient.create(driver);
        Neo4jMappingContext mappingContext = new Neo4jMappingContext();
        neo4jTemplate = new Neo4jTemplate(neo4jClient, mappingContext);

        ExecutionTracer.reset();
    }

    @AfterAll
    public static void closeDriver() {
        if (driver != null) {
            driver.close();
        }
        neo4j.stop();
        ExecutionTracer.reset();
    }

    @BeforeEach
    public void resetTracer() {
        // Clean up any existing data
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
        ExecutionTracer.reset();
    }

    @Test
    public void testSave() {
        PersonNode person = new PersonNode();
        String NAME = "Jane Austin";
        person.setName(NAME);
        person.setAge(65);

        ExecutionTracer.setExecutingInitNeo4J(false);

        PersonNode savedPerson = Neo4jTemplateClassReplacement.save(neo4jTemplate, person);

        assertNotNull(savedPerson);
        assertNotNull(savedPerson.getId());
        assertEquals(NAME, savedPerson.getName());
        assertEquals(65, savedPerson.getAge());

        Optional<PersonNode> retrieved = neo4jTemplate.findById(savedPerson.getId(), PersonNode.class);
        assertTrue(retrieved.isPresent());
        assertEquals(NAME, retrieved.get().getName());

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(1, neo4jSchemas.size());

        Neo4jNodeSchema nodeSchema = neo4jSchemas.iterator().next();
        assertEquals("PersonNode", nodeSchema.getNodeLabel());
    }

    @Test
    public void testFindById() {
        PersonNode person = new PersonNode();
        String NAME = "John Doe";
        int AGE = 30;
        person.setName(NAME);
        person.setAge(AGE);
        PersonNode savedPerson = neo4jTemplate.save(person);

        ExecutionTracer.setExecutingInitNeo4J(false);

        Optional<PersonNode> retrieved = Neo4jTemplateClassReplacement.findById(neo4jTemplate, savedPerson.getId(), PersonNode.class);

        assertTrue(retrieved.isPresent());
        assertEquals(NAME, retrieved.get().getName());
        assertEquals(AGE, retrieved.get().getAge());

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(1, neo4jSchemas.size());

        Neo4jNodeSchema nodeSchema = neo4jSchemas.iterator().next();
        assertEquals("PersonNode", nodeSchema.getNodeLabel());

        String expectedSchema = ClassToSchema.getOrDeriveSchemaWithItsRef(PersonNode.class, true, Collections.emptyList());
        assertEquals(expectedSchema, nodeSchema.getNodeSchema());
    }

    @Test
    public void testFindByIdNotFound() {
        String RANDOM_ID = UUID.randomUUID().toString();
        ExecutionTracer.setExecutingInitNeo4J(false);

        Optional<PersonNode> retrieved = Neo4jTemplateClassReplacement.findById(neo4jTemplate, RANDOM_ID, PersonNode.class);

        assertFalse(retrieved.isPresent());

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(1, neo4jSchemas.size());
    }

    @Test
    public void testFindAll() {
        // Save multiple persons
        PersonNode person1 = new PersonNode();
        String NAME_1 = "Alice";
        int AGE_1 = 25;
        person1.setName(NAME_1);
        person1.setAge(AGE_1);
        neo4jTemplate.save(person1);

        PersonNode person2 = new PersonNode();
        String NAME_2 = "Bob";
        int AGE_2 = 35;
        person2.setName(NAME_2);
        person2.setAge(AGE_2);
        neo4jTemplate.save(person2);

        ExecutionTracer.setExecutingInitNeo4J(false);

        List<PersonNode> allPersons = Neo4jTemplateClassReplacement.findAll(neo4jTemplate, PersonNode.class);

        assertEquals(2, allPersons.size());

        Set<String> names = new HashSet<>();
        for (PersonNode p : allPersons) {
            names.add(p.getName());
        }
        assertTrue(names.contains(NAME_1));
        assertTrue(names.contains(NAME_2));

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(1, neo4jSchemas.size());
    }

    @Test
    public void testCount() {
        // Save multiple persons
        PersonNode person1 = new PersonNode();
        String NAME_1 = "Alice";
        int AGE_1 = 25;
        person1.setName(NAME_1);
        person1.setAge(AGE_1);
        neo4jTemplate.save(person1);

        PersonNode person2 = new PersonNode();
        String NAME_2 = "Bob";
        int AGE_2 = 35;
        person2.setName(NAME_2);
        person2.setAge(AGE_2);
        neo4jTemplate.save(person2);

        ExecutionTracer.setExecutingInitNeo4J(false);

        long count = Neo4jTemplateClassReplacement.count(neo4jTemplate, PersonNode.class);

        assertEquals(2, count);

        // Verify schema tracking
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(1, neo4jSchemas.size());
    }

    @Test
    public void testDeleteById() {
        // First save a person
        PersonNode person = new PersonNode();
        person.setName("ToDelete");
        person.setAge(40);
        PersonNode savedPerson = neo4jTemplate.save(person);

        // Verify it exists
        assertEquals(1, neo4jTemplate.count(PersonNode.class));

        ExecutionTracer.setExecutingInitNeo4J(false);

        Neo4jTemplateClassReplacement.deleteById(neo4jTemplate, savedPerson.getId(), PersonNode.class);

        // Verify it's deleted
        assertEquals(0, neo4jTemplate.count(PersonNode.class));

        // Verify schema tracking
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(1, neo4jSchemas.size());
    }

    @Test
    public void testMultipleDomainTypes() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        // Query different domain types
        List<PersonNode> persons = Neo4jTemplateClassReplacement.findAll(neo4jTemplate, PersonNode.class);
        List<MovieNode> movies = Neo4jTemplateClassReplacement.findAll(neo4jTemplate, MovieNode.class);

        assertTrue(persons.isEmpty());
        assertTrue(movies.isEmpty());

        // Verify schema tracking captured both types
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(2, neo4jSchemas.size());

        Set<String> nodeLabels = new HashSet<>();
        for (Neo4jNodeSchema schema : neo4jSchemas) {
            nodeLabels.add(schema.getNodeLabel());
        }
        assertTrue(nodeLabels.contains("PersonNode"));
        assertTrue(nodeLabels.contains("MovieNode"));
    }

    @Test
    public void testInitNeo4JNotTracked() {
        ExecutionTracer.setExecutingInitNeo4J(true);

        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, PersonNode.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<Neo4jNodeSchema> neo4jSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(0, neo4jSchemas.size());
    }

    @Test
    public void testRelationshipTracking() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        List<ActorNode> actors = Neo4jTemplateClassReplacement.findAll(neo4jTemplate, ActorNode.class);

        assertTrue(actors.isEmpty());

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> neo4jNodeSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        assertEquals(1, neo4jNodeSchemas.size());

        Neo4jNodeSchema actorSchema = neo4jNodeSchemas.iterator().next();
        assertEquals("ActorNode", actorSchema.getNodeLabel());

        Set<Neo4jRelationshipSchema> neo4jRelationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();
        assertEquals(2, neo4jRelationshipSchemas.size());

        Set<String> relationshipTypes = new HashSet<>();
        for (Neo4jRelationshipSchema schema : neo4jRelationshipSchemas) {
            relationshipTypes.add(schema.getRelationshipType());
        }

        assertTrue(relationshipTypes.contains("ACTED_IN"));
        assertTrue(relationshipTypes.contains("DIRECTED"));
    }

    @Test
    public void testRelationshipNormalization() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, ActorNode.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jRelationshipSchema> neo4jRelationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();

        for (Neo4jRelationshipSchema schema : neo4jRelationshipSchemas) {
            if (schema.getRelationshipType().equals("ACTED_IN")) {
                // OUTGOING from ActorNode -> ActorNode is source
                assertEquals("ActorNode", schema.getSourceNodeLabel());
                assertEquals("MovieNode", schema.getTargetNodeLabel());
            } else if (schema.getRelationshipType().equals("DIRECTED")) {
                // INCOMING to ActorNode -> ActorNode is target, so normalized: MovieNode->ActorNode
                assertEquals("MovieNode", schema.getSourceNodeLabel());
                assertEquals("ActorNode", schema.getTargetNodeLabel());
            }
        }
    }

    @Test
    public void testInheritedRelationshipTracking() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, ChildNode.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jRelationshipSchema> neo4jRelationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();

        assertEquals(1, neo4jRelationshipSchemas.size());

        Neo4jRelationshipSchema schema = neo4jRelationshipSchemas.iterator().next();
        assertEquals("CREATED_BY", schema.getRelationshipType());
        // OUTGOING from ChildNode, so ChildNode is source
        assertEquals("ChildNode", schema.getSourceNodeLabel());
        assertEquals("PersonNode", schema.getTargetNodeLabel());
    }

    @Test
    public void testSingleReferenceRelationship() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        // NodeWithSingleRef has a single reference, not a collection
        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, NodeWithSingleRef.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jRelationshipSchema> neo4jRelationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();

        assertEquals(1, neo4jRelationshipSchemas.size());

        Neo4jRelationshipSchema schema = neo4jRelationshipSchemas.iterator().next();
        assertEquals("BELONGS_TO", schema.getRelationshipType());
        assertEquals("NodeWithSingleRef", schema.getSourceNodeLabel());
        assertEquals("PersonNode", schema.getTargetNodeLabel());
    }

    @Test
    public void testRelationshipWithProperties() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        // NodeWithRelProps uses @RelationshipProperties
        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, NodeWithRelProps.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jRelationshipSchema> neo4jRelationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();

        assertEquals(1, neo4jRelationshipSchemas.size());

        Neo4jRelationshipSchema schema = neo4jRelationshipSchemas.iterator().next();
        assertEquals("RATED", schema.getRelationshipType());
        assertEquals("NodeWithRelProps", schema.getSourceNodeLabel());
        assertEquals("MovieNode", schema.getTargetNodeLabel()); // Resolved via @TargetNode
        assertNotNull(schema.getPropertiesSchema()); // Should have schema for RatingRelationship
    }

    @Test
    public void testArrayRelationship() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        // NodeWithArrayRelationship uses MovieNode[] instead of List<MovieNode>
        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, NodeWithArrayRelationship.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jRelationshipSchema> neo4jRelationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();

        assertEquals(1, neo4jRelationshipSchemas.size());

        Neo4jRelationshipSchema schema = neo4jRelationshipSchemas.iterator().next();
        assertEquals("FEATURED_IN", schema.getRelationshipType());
        assertEquals("NodeWithArrayRelationship", schema.getSourceNodeLabel());
        assertEquals("MovieNode", schema.getTargetNodeLabel());
    }

    @Test
    public void testCustomNodeLabel() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        // CustomLabelNode has @Node("Film") instead of using class name
        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, CustomLabelNode.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jNodeSchema> neo4jNodeSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();

        assertEquals(1, neo4jNodeSchemas.size());

        Neo4jNodeSchema nodeSchema = neo4jNodeSchemas.iterator().next();
        assertEquals("Film", nodeSchema.getNodeLabel()); // Custom label, not "CustomLabelNode"
    }

    @Test
    public void testDefaultRelationshipType() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        // NodeWithDefaultRelType has @Relationship without explicit type - should use field name uppercase
        Neo4jTemplateClassReplacement.findAll(neo4jTemplate, NodeWithDefaultRelType.class);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jRelationshipSchema> neo4jRelationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();

        assertEquals(1, neo4jRelationshipSchemas.size());

        Neo4jRelationshipSchema schema = neo4jRelationshipSchemas.iterator().next();
        assertEquals("FAVORITEMOVIES", schema.getRelationshipType()); // field name "favoriteMovies" -> "FAVORITEMOVIES"
        assertEquals("NodeWithDefaultRelType", schema.getSourceNodeLabel());
        assertEquals("MovieNode", schema.getTargetNodeLabel());
    }

    @Test
    public void testActualRelationshipCreation() {
        ExecutionTracer.setExecutingInitNeo4J(false);

        // Create movies first
        MovieNode matrix = new MovieNode();
        matrix.setTitle("The Matrix");
        matrix.setYear(1999);
        MovieNode savedMatrix = Neo4jTemplateClassReplacement.save(neo4jTemplate, matrix);

        MovieNode johnWick = new MovieNode();
        johnWick.setTitle("John Wick");
        johnWick.setYear(2014);
        MovieNode savedJohnWick = Neo4jTemplateClassReplacement.save(neo4jTemplate, johnWick);

        // Create actor with relationships to movies
        ActorNode keanu = new ActorNode();
        keanu.setName("Keanu Reeves");
        keanu.setActedIn(Arrays.asList(savedMatrix, savedJohnWick));
        ActorNode savedKeanu = Neo4jTemplateClassReplacement.save(neo4jTemplate, keanu);

        // Verify actor was saved with ID
        assertNotNull(savedKeanu.getId());
        assertEquals("Keanu Reeves", savedKeanu.getName());

        // Verify relationships exist in Neo4j by querying
        try (Session session = driver.session()) {
            org.neo4j.driver.Result result = session.run(
                "MATCH (a:ActorNode)-[:ACTED_IN]->(m:MovieNode) WHERE a.name = 'Keanu Reeves' RETURN m.title as title"
            );
            Set<String> movieTitles = new HashSet<>();
            while (result.hasNext()) {
                movieTitles.add(result.next().get("title").asString());
            }
            assertEquals(2, movieTitles.size());
            assertTrue(movieTitles.contains("The Matrix"));
            assertTrue(movieTitles.contains("John Wick"));
        }

        // Verify schema tracking captured all node types and relationships
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<Neo4jNodeSchema> nodeSchemas = additionalInfoList.get(0).getNeo4jNodeSchemaData();
        Set<String> nodeLabels = new HashSet<>();
        for (Neo4jNodeSchema schema : nodeSchemas) {
            nodeLabels.add(schema.getNodeLabel());
        }
        assertTrue(nodeLabels.contains("MovieNode"));
        assertTrue(nodeLabels.contains("ActorNode"));

        Set<Neo4jRelationshipSchema> relationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();
        boolean foundActedIn = false;
        for (Neo4jRelationshipSchema schema : relationshipSchemas) {
            if (schema.getRelationshipType().equals("ACTED_IN")) {
                assertEquals("ActorNode", schema.getSourceNodeLabel());
                assertEquals("MovieNode", schema.getTargetNodeLabel());
                foundActedIn = true;
            }
        }
        assertTrue(foundActedIn, "ACTED_IN relationship schema should be tracked");
    }

    @Test
    public void testActualRelationshipRetrieval() {
        // First create data without tracking
        ExecutionTracer.setExecutingInitNeo4J(true);

        MovieNode movie = new MovieNode();
        movie.setTitle("Speed");
        movie.setYear(1994);
        MovieNode savedMovie = neo4jTemplate.save(movie);

        ActorNode actor = new ActorNode();
        actor.setName("Sandra Bullock");
        actor.setActedIn(Collections.singletonList(savedMovie));
        neo4jTemplate.save(actor);

        ExecutionTracer.reset();
        ExecutionTracer.setExecutingInitNeo4J(false);

        // Now retrieve and verify tracking works on retrieval
        List<ActorNode> actors = Neo4jTemplateClassReplacement.findAll(neo4jTemplate, ActorNode.class);

        assertEquals(1, actors.size());
        assertEquals("Sandra Bullock", actors.get(0).getName());

        // Verify the retrieved actor has the relationship loaded
        assertNotNull(actors.get(0).getActedIn());
        assertEquals(1, actors.get(0).getActedIn().size());
        assertEquals("Speed", actors.get(0).getActedIn().get(0).getTitle());

        // Verify schema tracking
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        Set<Neo4jRelationshipSchema> relationshipSchemas = additionalInfoList.get(0).getNeo4jRelationshipSchemaData();

        boolean foundActedIn = false;
        for (Neo4jRelationshipSchema schema : relationshipSchemas) {
            if (schema.getRelationshipType().equals("ACTED_IN")) {
                foundActedIn = true;
            }
        }
        assertTrue(foundActedIn, "ACTED_IN relationship schema should be tracked on retrieval");
    }

    @Node
    public static class PersonNode {
        @Id
        @GeneratedValue
        private Long id;
        private String name;
        private int age;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Node
    public static class MovieNode {
        @Id
        @GeneratedValue
        private Long id;
        private String title;
        private int year;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }
    }

    @Node
    public static class ActorNode {
        @Id
        @GeneratedValue
        private Long id;
        private String name;

        @Relationship(type = "ACTED_IN", direction = Relationship.Direction.OUTGOING)
        private List<MovieNode> actedIn;

        @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
        private List<MovieNode> directed;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<MovieNode> getActedIn() {
            return actedIn;
        }

        public void setActedIn(List<MovieNode> actedIn) {
            this.actedIn = actedIn;
        }

        public List<MovieNode> getDirected() {
            return directed;
        }

        public void setDirected(List<MovieNode> directed) {
            this.directed = directed;
        }
    }

    // Base class with relationship (for inheritance testing)
    @Node
    public static class BaseNode {
        @Id
        @GeneratedValue
        private Long id;

        @Relationship(type = "CREATED_BY", direction = Relationship.Direction.OUTGOING)
        private PersonNode creator;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public PersonNode getCreator() {
            return creator;
        }

        public void setCreator(PersonNode creator) {
            this.creator = creator;
        }
    }

    // Child class that inherits relationship from BaseNode
    @Node
    public static class ChildNode extends BaseNode {
        private String childProperty;

        public String getChildProperty() {
            return childProperty;
        }

        public void setChildProperty(String childProperty) {
            this.childProperty = childProperty;
        }
    }

    // Node with single reference (not collection)
    @Node
    public static class NodeWithSingleRef {
        @Id
        @GeneratedValue
        private Long id;

        @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
        private PersonNode owner;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public PersonNode getOwner() {
            return owner;
        }

        public void setOwner(PersonNode owner) {
            this.owner = owner;
        }
    }

    // Relationship with properties
    @RelationshipProperties
    public static class RatingRelationship {
        @Id
        @GeneratedValue
        private Long id;

        private int stars;
        private String comment;

        @TargetNode
        private MovieNode movie;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public int getStars() {
            return stars;
        }

        public void setStars(int stars) {
            this.stars = stars;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public MovieNode getMovie() {
            return movie;
        }

        public void setMovie(MovieNode movie) {
            this.movie = movie;
        }
    }

    // Node that uses @RelationshipProperties
    @Node
    public static class NodeWithRelProps {
        @Id
        @GeneratedValue
        private Long id;

        @Relationship(type = "RATED", direction = Relationship.Direction.OUTGOING)
        private List<RatingRelationship> ratings;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<RatingRelationship> getRatings() {
            return ratings;
        }

        public void setRatings(List<RatingRelationship> ratings) {
            this.ratings = ratings;
        }
    }

    // Node with array relationship (MovieNode[] instead of List<MovieNode>)
    @Node
    public static class NodeWithArrayRelationship {
        @Id
        @GeneratedValue
        private Long id;

        @Relationship(type = "FEATURED_IN", direction = Relationship.Direction.OUTGOING)
        private MovieNode[] movies;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public MovieNode[] getMovies() {
            return movies;
        }

        public void setMovies(MovieNode[] movies) {
            this.movies = movies;
        }
    }

    // Node with custom label (not using class name)
    @Node("Film")
    public static class CustomLabelNode {
        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    // Node with @Relationship that has no explicit type (should use field name uppercase)
    @Node
    public static class NodeWithDefaultRelType {
        @Id
        @GeneratedValue
        private Long id;

        @Relationship(direction = Relationship.Direction.OUTGOING)
        private List<MovieNode> favoriteMovies;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<MovieNode> getFavoriteMovies() {
            return favoriteMovies;
        }

        public void setFavoriteMovies(List<MovieNode> favoriteMovies) {
            this.favoriteMovies = favoriteMovies;
        }
    }
}
