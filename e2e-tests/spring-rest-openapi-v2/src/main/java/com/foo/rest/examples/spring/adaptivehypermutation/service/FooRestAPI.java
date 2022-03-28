package com.foo.rest.examples.spring.adaptivehypermutation.service;

import com.foo.rest.examples.spring.adaptivehypermutation.entity.*;
import com.foo.rest.examples.spring.adaptivehypermutation.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.*;
/** automatically created on 2020-10-22 */
@RestController
@RequestMapping(path = "/api")
public class FooRestAPI {
  @Autowired private FooRepository fooRepository;

  @RequestMapping(
      value = "/foos/{x}",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON)
  public ResponseEntity createFoo(
      @PathVariable(name = "x") Integer x, @RequestParam String y, @Valid @RequestBody Info z) {
    if (fooRepository.count() < 3)
      return ResponseEntity.status(400).build();
    if (x < 0 || fooRepository.findById(x).isPresent())
      return ResponseEntity.status(400).build();
    if (!y.equalsIgnoreCase("foo"))
      return ResponseEntity.status(400).build();
    String response = "B0";
    if (z.c == 100)
      response = "B1";
    else if (z.c == 200)
      response = "B2";
    else if (z.c == 300)
      response = "B3";
    LocalDate date = LocalDate.parse(z.t);
    if (date.getYear() == 2020)
      response += "B4";
    if (fooRepository.findById(42).isPresent())
      response += "B5";

    FooEntity node = new FooEntity();
    node.setX(x);
    node.setY(y);
    node.setZ(z);
    // save the entity
    fooRepository.save(node);
    return ResponseEntity.ok(response);
  }

  @RequestMapping(
      value = "/foos/{x}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Foo> getFooById(@PathVariable(name = "x") Integer x) {
    if (!fooRepository.findById(x).isPresent()) return ResponseEntity.status(404).build();
    Foo dto = fooRepository.findById(x).get().getDto();
    return ResponseEntity.ok(dto);
  }

  @RequestMapping(
      value = "/foos",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<List<Foo>> getAllFoo() {
    List<Foo> allDtos = new ArrayList<>();
    for (FooEntity e : fooRepository.findAll()) {
      allDtos.add(e.getDto());
    }
    return ResponseEntity.ok(allDtos);
  }

  @RequestMapping(
      value = "/foos/{x}",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity deleteFoo(@PathVariable(name = "x") Integer x) {
    // an entity with id x should exist
    if (!fooRepository.findById(x).isPresent()) return ResponseEntity.status(404).build();
    fooRepository.deleteById(x);
    return ResponseEntity.status(200).build();
  }
}
