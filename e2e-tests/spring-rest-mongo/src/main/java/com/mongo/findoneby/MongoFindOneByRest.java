package com.mongo.findoneby;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(path = "/findoneby")
public class MongoFindOneByRest {

    @Autowired
    private MongoFindOneByRepository repository;

    @RequestMapping(value = "/sourcetypeid/{source}/{type}/{id}", method = RequestMethod.GET)
    public ResponseEntity<Void> getSessionBySourceTypeAndId(@PathVariable String source,
                              @PathVariable SessionType type,
                              @PathVariable String id) {
        Session session = repository.findOneBySourceAndTypeAndId(source, type, id);
        if(session!=null){
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }

    @RequestMapping(value = "id/{id}", method = RequestMethod.GET)
    public ResponseEntity<Void> getSessionById(@PathVariable String id) {
        Session session = repository.findOneById(id);
        if(session!=null){
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }

    @RequestMapping(value = "type/{type}", method = RequestMethod.GET)
    public ResponseEntity<Void> getSessionByType(@PathVariable SessionType type) {
        Session session = repository.findOneByType(type);
        if(session!=null){
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }
    @RequestMapping(value = "source/{source}", method = RequestMethod.GET)
    public ResponseEntity<Void> getSessionBySource(@PathVariable String source) {
        Session session = repository.findOneBySource(source);
        if(session!=null){
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }

    @RequestMapping(value = "idtype/{id}/{type}", method = RequestMethod.GET)
    public ResponseEntity<Void> getSessionByIdAndType(@PathVariable String id,
                                                      @PathVariable SessionType type) {
        Session session = repository.findOneByIdAndType(id, type);
        if(session!=null){
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(400).build();
        }
    }

}
