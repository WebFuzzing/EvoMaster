package com.foo.neo4j.session.findnodenosave;

import com.foo.neo4j.AbstractNeo4jRest;
import org.neo4j.driver.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Has no endpoint that writes, so the query can only be satisfied by data that EvoMaster inserts.
 */
@RestController
@RequestMapping(path = "/neo4jsessionfindnodenosave")
public class Neo4jSessionFindNodeNoSaveRest extends AbstractNeo4jRest {

    @GetMapping("/findPerson")
    public ResponseEntity<Void> findPerson() {
        try (Session session = driver.session()) {
            boolean found = session.run("MATCH (p:Person {name: 'Ana'}) RETURN p").hasNext();
            return ResponseEntity.status(found ? 200 : 404).build();
        }
    }
}
