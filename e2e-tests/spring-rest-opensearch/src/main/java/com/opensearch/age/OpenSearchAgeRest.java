package com.opensearch.age;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<Age> findByAge(@PathVariable("age") Integer age) throws IOException {
        return ageRepo.findByAge(age);
    }

    @GetMapping("gte/{gte}")
    public List<Age> findGteAge(@PathVariable("gte") Integer gte) throws IOException {
        return ageRepo.findGteAge(gte);
    }

    @GetMapping("lte/{lte}")
    public List<Age> findLteAge(@PathVariable("lte") Integer lte) throws IOException {
        return ageRepo.findLteAge(lte);
    }

    @GetMapping("gt/{gt}")
    public List<Age> findGtAge(@PathVariable("gt") Integer gt) throws IOException {
        return ageRepo.findGtAge(gt);
    }

    @GetMapping("lt/{lt}")
    public List<Age> findLtAge(@PathVariable("lt") Integer lt) throws IOException {
        return ageRepo.findLtAge(lt);
    }

    @GetMapping("gte-lte/{gte}/{lte}")
    public List<Age> findGteLteAge(@PathVariable("gte") Integer gte, @PathVariable("lte") Integer lte) throws IOException {
        return ageRepo.findGteLteAge(gte, lte);
    }

    @GetMapping("gte-lt/{gte}/{lt}")
    public List<Age> findGteLtAge(@PathVariable("gte") Integer gte, @PathVariable("lt") Integer lt) throws IOException {
        return ageRepo.findGteLtAge(gte, lt);
    }

    @GetMapping("gt-lte/{gt}/{lte}")
    public List<Age> findGtLteAge(@PathVariable("gt") Integer gt, @PathVariable("lte") Integer lte) throws IOException {
        return ageRepo.findGtLteAge(gt, lte);
    }

    @GetMapping("gt-lt/{gt}/{lt}")
    public List<Age> findGtLtAge(@PathVariable("gt") Integer gt, @PathVariable("lt") Integer lt) throws IOException {
        return ageRepo.findGtLtAge(gt, lt);
    }


}
