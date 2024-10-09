package com.foo.spring.rest.h2.z3solver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by agusaldasoro on 04-Oct-24.
 */
@RestController
@RequestMapping(path = "/api/h2/z3solver/")
public class Z3SolverTypesRest {

    @Autowired
    private EntityManager em;

//    // This fails to load, as when the WHERE clause is empty, it fails to calculate the failedWhere
//    @GetMapping("/products")
//    public ResponseEntity<Void> getEmptyWhere() {
//        Query query = em.createNativeQuery(
//                "select (1) from products");
//        List<?> data = query.getResultList();
//
//        if (data.isEmpty()) {
//            return ResponseEntity.status(400).build();
//        } else {
//            return ResponseEntity.status(200).build();
//        }
//    }

    @GetMapping("/products-1")
    public ResponseEntity<Void> getId1() {
        Query query = em.createNativeQuery(
                "select (1) from products where id = 1");
        List<?> data = query.getResultList();

        if (data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }
}

