package com.mongo.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exercises exists/count/delete through Spring Data repositories (derived and annotated queries),
 * so the Mongo driver is only called from within Spring Data and never from SUT code.
 */
@RestController
@RequestMapping(path = "/mongorepository")
public class MongoRepositoryController {

    @Autowired
    private ExistsDerivedRepository existsDerivedRepository;

    @Autowired
    private ExistsAnnotatedRepository existsAnnotatedRepository;

    @Autowired
    private CountDerivedRepository countDerivedRepository;

    @Autowired
    private CountAnnotatedRepository countAnnotatedRepository;

    @Autowired
    private DeleteDerivedRepository deleteDerivedRepository;

    @Autowired
    private DeleteAnnotatedRepository deleteAnnotatedRepository;

    private static ResponseEntity<Void> response(boolean found) {
        return ResponseEntity.status(found ? 200 : 404).build();
    }

    /**
     * Derived exists query on a Spring Data repository
     */
    @GetMapping("existsDerived")
    public ResponseEntity<Void> existsDerived(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(existsDerivedRepository.existsByCityAndAgeGreaterThanEqual(city, minAge));
    }

    /**
     * Annotated exists query on a Spring Data repository
     */
    @GetMapping("existsAnnotated")
    public ResponseEntity<Void> existsAnnotated(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(existsAnnotatedRepository.existsAnnotated(city, minAge));
    }

    /**
     * Derived count query on a Spring Data repository
     */
    @GetMapping("countDerived")
    public ResponseEntity<Void> countDerived(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(countDerivedRepository.countByCityAndAgeGreaterThanEqual(city, minAge) > 0);
    }

    /**
     * Annotated count query on a Spring Data repository
     */
    @GetMapping("countAnnotated")
    public ResponseEntity<Void> countAnnotated(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(countAnnotatedRepository.countAnnotated(city, minAge) > 0);
    }

    /**
     * Derived delete query on a Spring Data repository
     */
    @DeleteMapping("deleteDerived")
    public ResponseEntity<Void> deleteDerived(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(deleteDerivedRepository.deleteByCityAndAgeGreaterThanEqual(city, minAge) > 0);
    }

    /**
     * Annotated delete query on a Spring Data repository
     */
    @DeleteMapping("deleteAnnotated")
    public ResponseEntity<Void> deleteAnnotated(@RequestParam(name = "city") String city,
            @RequestParam(name = "minAge") int minAge) {
        return response(deleteAnnotatedRepository.deleteAnnotated(city, minAge) > 0);
    }
}
