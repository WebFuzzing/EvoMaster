package com.cassandra.findbyagereadonly;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * There is no endpoint writing into the table on purpose: a 200 here is only reachable if EvoMaster
 * inserts a row of its own into the database first.
 */
@RestController
@RequestMapping(path = "/cassandrafindbyagereadonly")
public class CassandraFindByAgeReadOnlyRest {

    @Autowired
    private CqlSession session;

    /**
     * The value is written into the CQL text instead of being bound: the heuristics are computed on
     * the text of the query, so a bound value would leave nothing for the search to be guided by.
     */
    @GetMapping("/person/{age}")
    public ResponseEntity<Void> findByAge(@PathVariable int age) {

        boolean found = session.execute("SELECT * FROM " + CassandraFindByAgeReadOnlyApp.KEYSPACE + "."
                + CassandraFindByAgeReadOnlyApp.TABLE + " WHERE age = " + age).iterator().hasNext();

        if (found) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }
}