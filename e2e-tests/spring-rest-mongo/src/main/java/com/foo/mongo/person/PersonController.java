package com.foo.mongo.person;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class PersonController {

    @Autowired
    private PersonRepository repository;

    @Path("/api/mongoperson/add")
    @POST
    public void add(PersonDto dto) {
        Person person = Person.fromDto(dto);
        repository.save(person);
    }

    @Path("/api/mongoperson/findByLastName")
    @GET
    public List<PersonDto> findByLastName(String lastName) {
        List<Person> persons = repository.findByLastName(lastName);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }


}
