package com.cassandra.findbytag;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * There is no endpoint writing into the table on purpose: a 200 here is only reachable if EvoMaster
 * inserts a row of its own into the database first, with the queried tag inside the generated set.
 */
@RestController
@RequestMapping(path = "/cassandrafindbytag")
public class CassandraFindByTagRest {

    @Autowired
    private CqlSession session;

    /**
     * The tag is inlined, as the heuristics are computed on the text of the query, so the quotes it
     * may contain are escaped the way CQL expects, ie doubled. Without that, any tag holding a quote
     * would make the statement fail to parse.
     */
    @GetMapping("/session/tagged/{tag}")
    public ResponseEntity<Void> findByTag(@PathVariable String tag) {

        String escaped = tag.replace("'", "''");

        boolean found = session.execute("SELECT * FROM " + CassandraFindByTagApp.KEYSPACE + "."
                + CassandraFindByTagApp.TABLE + " WHERE tags CONTAINS '" + escaped + "'")
                .iterator().hasNext();

        if (found) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }
}