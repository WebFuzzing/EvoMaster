package com.cassandra.findbyage;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/cassandrafindbyage")
public class CassandraFindByAgeRest {

    @Autowired
    private CqlSession session;

    @PostMapping("/person/{name}/{age}")
    public ResponseEntity<Void> savePerson(@PathVariable String name, @PathVariable int age) {

        session.execute(SimpleStatement.newInstance("INSERT INTO " + CassandraFindByAgeApp.KEYSPACE + "."
                + CassandraFindByAgeApp.TABLE + " (age, name) VALUES (" + age + ", ?)", name));

        return ResponseEntity.status(200).build();
    }
    
    @GetMapping("/person/{age}")
    public ResponseEntity<Void> findByAge(@PathVariable int age) {

        boolean found = session.execute("SELECT * FROM " + CassandraFindByAgeApp.KEYSPACE + "."
                + CassandraFindByAgeApp.TABLE + " WHERE age = " + age).iterator().hasNext();

        if (found) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }
}