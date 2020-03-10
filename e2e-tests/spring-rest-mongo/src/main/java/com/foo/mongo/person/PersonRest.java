package com.foo.mongo.person;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class PersonRest {

    @Autowired
    private PersonRepository repository;

    @RequestMapping(path = "/api/mongoperson/add",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = {RequestMethod.POST})
    public void add(@RequestBody PersonDto dto) {
        Person person = Person.fromDto(dto);
        repository.save(person);
    }

    @RequestMapping(path = "/api/mongoperson/findByLastName",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = {RequestMethod.GET})
    public List<PersonDto> findByLastName(@RequestBody String lastName) {
        List<Person> persons = repository.findByLastName(lastName);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByAgeGreaterThan",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = {RequestMethod.GET})
    public List<PersonDto> findByAgeGreaterThan(@RequestBody int age) {
        List<Person> persons = repository.findByAgeGreaterThan(age);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findAll",
            method = {RequestMethod.GET})
    public List<PersonDto> findAll() {
        List<Person> persons = new ArrayList<>();
        repository.findAll().forEach(persons::add);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByAgeLessThan",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = {RequestMethod.GET})
    public List<PersonDto> findByAgeLessThan(@RequestBody int age) {
        List<Person> persons = repository.findByAgeLessThan(age);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByAge/{age}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByAge(@PathVariable int age) {
        List<Person> persons = repository.findByAge(age);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }


    @RequestMapping(path = "/api/mongoperson/findByAgeBetween/{from}/{to}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByAgeBetween(@PathVariable int from, @PathVariable int to) {
        List<Person> persons = repository.findByAgeBetween(from, to);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByFirstNameNotNull",
            method = {RequestMethod.GET})
    public List<PersonDto> findByFirstNameNotNull() {
        List<Person> persons = repository.findByFirstNameNotNull();
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByFirstNameNull",
            method = {RequestMethod.GET})
    public List<PersonDto> findByFirstNameNull() {
        List<Person> persons = repository.findByFirstNameNull();
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByFirstNameLike",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = {RequestMethod.GET})
    public List<PersonDto> findByFirstNameLike(@RequestBody String name) {
        List<Person> persons = repository.findByFirstNameLike(name);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByFirstNameRegex",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = {RequestMethod.GET})
    public List<PersonDto> findByFirstNameRegex(@RequestBody String name) {
        List<Person> persons = repository.findByFirstNameRegex(name);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }
}
