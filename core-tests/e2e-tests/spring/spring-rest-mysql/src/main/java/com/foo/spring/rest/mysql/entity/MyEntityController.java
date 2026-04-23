package com.foo.spring.rest.mysql.entity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/myentities")
public class MyEntityController {

    private final MyEntityService myEntityService;

    public MyEntityController(MyEntityService myEntityService) {
        this.myEntityService = myEntityService;
    }


    @GetMapping("/{id}")
    public ResponseEntity<MyEntity> getEntityById(@PathVariable Long id){
        MyEntity myEntity = myEntityService.getMyEntityById(id);
        if (myEntity == null) {
            return ResponseEntity.status(400).body(null);
        } else {
            return ResponseEntity.status(200).body(myEntity);
        }
    }
}
