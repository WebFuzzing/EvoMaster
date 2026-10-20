package com.foo.neo4j.entity.findbyname;

import com.foo.neo4j.AbstractNeo4jRest;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * Looks a person up by name and returns the whole node. Nothing writes to the graph, so the lookup can
 * only succeed on data EvoMaster inserts, and the response shows which properties that data carries.
 */
@RestController
@RequestMapping(path = "/neo4jentityfindbyname")
public class Neo4jEntityFindByNameRest extends AbstractNeo4jRest {

    @GetMapping("/findPerson/{name}")
    public ResponseEntity<Person> findPerson(@PathVariable("name") String name) {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (p:Person {name: $name}) RETURN p", Collections.singletonMap("name", name));
            if (!result.hasNext()) {
                return ResponseEntity.notFound().build();
            }
            Record record = result.next();
            Node node = record.get("p").asNode();
            return ResponseEntity.ok(new Person(
                    node.get("name").asString(null),
                    node.get("age").isNull() ? null : node.get("age").asInt(),
                    node.get("active").isNull() ? null : node.get("active").asBoolean(),
                    node.get("score").isNull() ? null : node.get("score").asDouble()));
        }
    }
}
