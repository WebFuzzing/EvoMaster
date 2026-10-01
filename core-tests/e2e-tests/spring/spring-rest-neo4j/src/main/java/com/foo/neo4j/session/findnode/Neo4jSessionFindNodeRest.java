package com.foo.neo4j.session.findnode;

import com.foo.neo4j.AbstractNeo4jRest;
import org.neo4j.driver.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * Runs its queries directly on the session, in auto-commit transactions.
 */
@RestController
@RequestMapping(path = "/neo4jsessionfindnode")
public class Neo4jSessionFindNodeRest extends AbstractNeo4jRest {

    @PostMapping("/x/foo/{y}/bar")
    public ResponseEntity<Void> savePerson(@PathVariable String y) {
        try (Session session = driver.session()) {
            session.run("CREATE (:Person {name: $name})", Collections.singletonMap("name", y)).consume();
        }
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/findPerson/{name}")
    public ResponseEntity<Void> findPerson(@PathVariable String name) {
        try (Session session = driver.session()) {
            boolean found = session
                    .run("MATCH (p:Person {name: $name}) RETURN p", Collections.singletonMap("name", name))
                    .hasNext();
            return ResponseEntity.status(found ? 200 : 404).build();
        }
    }
}
