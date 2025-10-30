package com.mongo.students;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/students")
public class StudentController {

    @Autowired
    private StudentsRepository students;

    @PostMapping("jorge")
    public void post() {
        Student s = new Student("Jorge", "Ramirez");
        students.save(s);
    }

    @GetMapping("{lastName}")
    public List<Student> findByLastName(@PathVariable("lastName") String lastName) {
        return students.findByLastName(lastName);
    }

    @PostMapping("addAndGetJorge")
    public List<Student> postAndGet() {
        Student s = new Student("Jorge", "Ramirez");
        students.save(s);
        return students.findByFirstName("Jorge");
    }
}


