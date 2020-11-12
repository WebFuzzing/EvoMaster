package com.foo.rest.examples.spring.resource.service;

import com.foo.rest.examples.spring.resource.entity.*;
import com.foo.rest.examples.spring.resource.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
/** automatically created on 2019-08-29 */
@RestController
@RequestMapping(path = "/api/rA")
public class RARestAPI {
  @Autowired private RARepository rARepository;

  @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
  public ResponseEntity createRAEntity(@RequestBody RA rA) {
    if (rARepository.findById(rA.id).isPresent()) return ResponseEntity.status(400).build();
    RAEntity node = new RAEntity();
    node.setId(rA.id);
    node.setName(rA.name);
    node.setValue(rA.value);
    rARepository.save(node);
    return ResponseEntity.status(201).build();
  }

  @RequestMapping(
      value = "/{rAId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<RA> getRAEntity(@PathVariable(name = "rAId") Long rAId) {
    if (!rARepository.findById(rAId).isPresent()) return ResponseEntity.status(400).build();
    RA dto = rARepository.findById(rAId).get().getDto();
    return ResponseEntity.ok(dto);
  }
}

