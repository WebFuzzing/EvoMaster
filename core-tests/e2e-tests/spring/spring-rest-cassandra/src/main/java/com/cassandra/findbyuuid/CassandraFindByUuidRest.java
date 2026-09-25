package com.cassandra.findbyuuid;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * There is no endpoint writing into the table on purpose: a 200 here is only reachable if EvoMaster
 * inserts a row of its own into the database first.
 */
@RestController
@RequestMapping(path = "/cassandrafindbyuuid")
public class CassandraFindByUuidRest {

    @Autowired
    private CqlSession session;

    /**
     * The id is taken as a {@code UUID}, so that Spring answers 400 to anything that is not one, and
     * what is written into the CQL text is always a valid uuid literal. The value is inlined rather
     * than bound, as the heuristics are computed on the text of the query.
     */
    @GetMapping("/record/{id}")
    public ResponseEntity<Void> findById(@PathVariable UUID id) {

        boolean found = session.execute("SELECT * FROM " + CassandraFindByUuidApp.KEYSPACE + "."
                + CassandraFindByUuidApp.TABLE + " WHERE id = " + id).iterator().hasNext();

        if (found) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }
}