package com.mongo.students;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface StudentsRepository extends MongoRepository<Student, String> {
    List<Student> findByLastName(String lastName);
    List<Student> findByFirstName(String firstName);
}