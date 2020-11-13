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
@RequestMapping(path = "/api/rpR")
public class RpRRestAPI {
  @Autowired private RpRRepository rpRRepository;
  @Autowired private RdRepository rdRepository;

  @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
  public ResponseEntity createRpREntity(@RequestBody RpR rpR) {
    if (rpRRepository.findById(rpR.id).isPresent()) return ResponseEntity.status(400).build();
    RpREntity node = new RpREntity();
    node.setId(rpR.id);
    node.setName(rpR.name);
    node.setValue(rpR.value);
    if (!rdRepository.findById(rpR.rdId).isPresent()) return ResponseEntity.status(400).build();
    RdEntity referVarToRdEntity = rdRepository.findById(rpR.rdId).get();
    node.setRd(referVarToRdEntity);
    rpRRepository.save(node);
    return ResponseEntity.status(201).build();
  }

  @RequestMapping(
      value = "/{rpRId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<RpR> getRpREntity(@PathVariable(name = "rpRId") Long rpRId) {
    if (!rpRRepository.findById(rpRId).isPresent()) return ResponseEntity.status(400).build();
    RpR dto = rpRRepository.findById(rpRId).get().getDto();
    return ResponseEntity.ok(dto);
  }
}

