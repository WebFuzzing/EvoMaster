package com.opensearch.students;

import com.opensearch.config.OpenSearchRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/students")
public class OpenSearchStudentsRest {
    @Autowired
    private StudentsRepository students;

    @PostMapping("jorge")
    public void post() throws IOException {
        Student s = new Student("Jorge", "Ramirez");
        students.index(s);
    }

    @GetMapping("{lastName}")
    public List<Student> findByLastName(@PathVariable("lastName") String lastName) throws IOException {
        return students.findByLastName(lastName);
    }

    @PostMapping("addAndGetJorge")
    public List<Student> postAndGet() throws IOException {
        Student s = new Student("Jorge", "Ramirez");
        students.index(s);
        return students.findByFirstName("Jorge");
    }
}
