package com.foo.rest.examples.spring.db.existingdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by arcuri82 on 19-Jun-19.
 */
@RestController
@RequestMapping(path = "/api/db/existingdata")
public class ExistingDataRest {

    @Autowired
    private EntityManager em;


    @GetMapping
    public ResponseEntity get() {

        //id=42 is created in ExistingDataController
        TypedQuery<ExistingDataEntityY> query = em.createQuery(
                "select y from ExistingDataEntityY y where y.x.id=42", ExistingDataEntityY.class);
        List<ExistingDataEntityY> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }
}
