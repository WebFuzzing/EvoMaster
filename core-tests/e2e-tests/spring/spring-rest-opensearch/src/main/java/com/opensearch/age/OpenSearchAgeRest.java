package com.opensearch.age;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/age")
public class OpenSearchAgeRest {
    @Autowired
    private AgeRepository ageRepo;

    @PostMapping("age")
    public void post() throws IOException {
        Age s = new Age(18);
        ageRepo.index(s);
    }

    @GetMapping("{age}")
    public ResponseEntity<List<Age>> findByAge(@PathVariable("age") Integer age) throws IOException {
        List<Age> results = ageRepo.findByAge(age);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("gte/{gte}")
    public ResponseEntity<List<Age>> findGteAge(@PathVariable("gte") Integer gte) throws IOException {
        List<Age> results = ageRepo.findGteAge(gte);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("lte/{lte}")
    public ResponseEntity<List<Age>> findLteAge(@PathVariable("lte") Integer lte) throws IOException {
        List<Age> results = ageRepo.findLteAge(lte);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("gt/{gt}")
    public ResponseEntity<List<Age>> findGtAge(@PathVariable("gt") Integer gt) throws IOException {
        List<Age> results = ageRepo.findGtAge(gt);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("lt/{lt}")
    public ResponseEntity<List<Age>> findLtAge(@PathVariable("lt") Integer lt) throws IOException {
        List<Age> results = ageRepo.findLtAge(lt);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("gte-lte/{gte}/{lte}")
    public ResponseEntity<List<Age>> findGteLteAge(@PathVariable("gte") Integer gte, @PathVariable("lte") Integer lte) throws IOException {
        List<Age> results = ageRepo.findGteLteAge(gte, lte);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("gte-lt/{gte}/{lt}")
    public ResponseEntity<List<Age>> findGteLtAge(@PathVariable("gte") Integer gte, @PathVariable("lt") Integer lt) throws IOException {
        List<Age> results = ageRepo.findGteLtAge(gte, lt);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("gt-lte/{gt}/{lte}")
    public ResponseEntity<List<Age>> findGtLteAge(@PathVariable("gt") Integer gt, @PathVariable("lte") Integer lte) throws IOException {
        List<Age> results = ageRepo.findGtLteAge(gt, lte);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("gt-lt/{gt}/{lt}")
    public ResponseEntity<List<Age>> findGtLtAge(@PathVariable("gt") Integer gt, @PathVariable("lt") Integer lt) throws IOException {
        List<Age> results = ageRepo.findGtLtAge(gt, lt);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

}
