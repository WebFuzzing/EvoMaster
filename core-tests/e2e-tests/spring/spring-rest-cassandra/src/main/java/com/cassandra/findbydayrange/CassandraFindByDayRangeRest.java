package com.cassandra.findbydayrange;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * There is no endpoint writing into the table on purpose: a 200 here is only reachable if EvoMaster
 * inserts a row of its own into the database first.
 */
@RestController
@RequestMapping(path = "/cassandrafindbydayrange")
public class CassandraFindByDayRangeRest {

    @Autowired
    private CqlSession session;

    /**
     * Two conditions to satisfy at once: an equality on the partition key, which is a date, and a
     * range on the clustering column, which is a timestamp given as epoch milliseconds. Both values
     * are inlined, as the heuristics are computed on the text of the query, and both are safe to
     * inline: the day is re-serialized from a parsed {@code LocalDate}, and the instant is a long.
     *
     * @param day    the day to look into, as an ISO date, ie 2020-01-31
     * @param millis the instant, in epoch milliseconds, the measurements must be later than
     */
    @GetMapping("/measurement/{day}/after/{millis}")
    public ResponseEntity<Void> findAfter(@PathVariable String day, @PathVariable long millis) {

        LocalDate parsedDay;
        try {
            parsedDay = LocalDate.parse(day);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(400).build();
        }

        boolean found = session.execute("SELECT * FROM " + CassandraFindByDayRangeApp.KEYSPACE + "."
                + CassandraFindByDayRangeApp.TABLE + " WHERE day = '" + parsedDay + "'"
                + " AND at > " + millis).iterator().hasNext();

        if (found) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }
}