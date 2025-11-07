package com.foo.rest.examples.spring.adaptivehypermutation.service;

import com.foo.rest.examples.spring.adaptivehypermutation.entity.*;
import com.foo.rest.examples.spring.adaptivehypermutation.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
/** automatically created on 2020-10-22 */
@RestController
@RequestMapping(path = "/api")
public class BarRestAPI {
  @Autowired private BarRepository barRepository;

  @RequestMapping(
      value = "/bars/{a}",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON)
  public ResponseEntity createBar(@PathVariable(name = "a") Integer a, @RequestParam(name = "b", required = true) String b, @RequestParam(name = "c") Integer c) {
    // an entity with id bar.id should not exist
    if (barRepository.findById(a).isPresent()) return ResponseEntity.status(400).build();
    if (!b.toLowerCase().equals("bar")) return ResponseEntity.status(400).build();
    BarEntity node = new BarEntity();
    node.setA(a);
    node.setB(b);
    node.setC(c);
    // save the entity
    barRepository.save(node);
    return ResponseEntity.status(201).build();
  }

  @RequestMapping(
      value = "/bars/{a}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Bar> getBarById(@PathVariable(name = "a") Integer a) {
    if (!barRepository.findById(a).isPresent()) return ResponseEntity.status(404).build();
    Bar dto = barRepository.findById(a).get().getDto();
    return ResponseEntity.ok(dto);
  }

  @RequestMapping(value = "/bars", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<List<Bar>> getAllBar() {
    List<Bar> allDtos = new ArrayList<>();
    for (BarEntity e : barRepository.findAll()) {
      allDtos.add(e.getDto());
    }
    return ResponseEntity.ok(allDtos);
  }



  @RequestMapping(
      value = "/bars/{a}",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity deleteBar(@PathVariable(name = "a") Integer a) {
    // an entity with id a should exist
    if (!barRepository.findById(a).isPresent()) return ResponseEntity.status(404).build();
    barRepository.deleteById(a);
    return ResponseEntity.status(200).build();
  }
}
