package com.foo.neo4j.transaction.findnode;

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
 * Runs its queries inside managed transactions, so they go through {@code Transaction.run} and
 * never through {@code Session.run}.
 */
@RestController
@RequestMapping(path = "/neo4jtransactionfindnode")
public class Neo4jTransactionFindNodeRest extends AbstractNeo4jRest {

    @PostMapping("/x/foo/{y}/bar")
    public ResponseEntity<Void> savePerson(@PathVariable String y) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx ->
                    tx.run("CREATE (:Person {name: $name})", Collections.singletonMap("name", y)).consume());
        }
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/findPerson/{name}")
    public ResponseEntity<Void> findPerson(@PathVariable String name) {
        try (Session session = driver.session()) {
            boolean found = session.readTransaction(tx ->
                    tx.run("MATCH (p:Person {name: $name}) RETURN p", Collections.singletonMap("name", name))
                            .hasNext());
            return ResponseEntity.status(found ? 200 : 404).build();
        }
    }
}
