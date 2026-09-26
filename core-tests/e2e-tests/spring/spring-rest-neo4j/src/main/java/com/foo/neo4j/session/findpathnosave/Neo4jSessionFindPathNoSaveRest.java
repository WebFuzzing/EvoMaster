package com.foo.neo4j.session.findpathnosave;

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
@RequestMapping(path = "/neo4jsessionfindpathnosave")
public class Neo4jSessionFindPathNoSaveRest extends AbstractNeo4jRest {

    @GetMapping("/findOwner")
    public ResponseEntity<Void> findOwner() {
        try (Session session = driver.session()) {
            boolean found = session.run("MATCH (p:Player)-[:HAS_USER]->(u:User {username: 'ana'}) RETURN p").hasNext();
            return ResponseEntity.status(found ? 200 : 404).build();
        }
    }
}
