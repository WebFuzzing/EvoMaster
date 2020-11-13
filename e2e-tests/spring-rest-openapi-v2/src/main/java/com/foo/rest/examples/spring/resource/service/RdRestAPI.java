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
@RequestMapping(path = "/api/rd")
public class RdRestAPI {
  @Autowired private RdRepository rdRepository;

  @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
  public ResponseEntity createRdEntity(@RequestBody Rd rd) {
    if (rdRepository.findById(rd.id).isPresent()) return ResponseEntity.status(400).build();
    RdEntity node = new RdEntity();
    node.setId(rd.id);
    node.setName(rd.name);
    node.setValue(rd.value);
    rdRepository.save(node);
    return ResponseEntity.status(201).build();
  }

  @RequestMapping(
      value = "/{rdId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Rd> getRdEntity(@PathVariable(name = "rdId") Long rdId) {
    if (!rdRepository.findById(rdId).isPresent()) return ResponseEntity.status(400).build();
    Rd dto = rdRepository.findById(rdId).get().getDto();
    return ResponseEntity.ok(dto);
  }
}

