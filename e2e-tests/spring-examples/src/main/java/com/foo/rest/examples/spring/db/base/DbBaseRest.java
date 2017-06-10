package com.foo.rest.examples.spring.db.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping(path = "/api/db/base")
public class DbBaseRest {

    @Autowired
    private DbBaseRepository repository;


    @RequestMapping(
            path = "/entities",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity create(@NotNull @RequestBody DbBaseDto dto) {

        DbBaseEntity entity = new DbBaseEntity();
        entity.setName(dto.name);

        repository.save(entity);
        long id = entity.getId();

        return ResponseEntity.created(URI.create("/api/db/base/entities/" + id)).build();
    }

    @RequestMapping(
            path = "/entities",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public List<DbBaseDto> getAll() {

        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(e -> new DbBaseDto(e.getId(), e.getName()))
                .collect(Collectors.toList());
    }

    @RequestMapping(
            path = "/entities/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<DbBaseDto> get(@PathVariable("id") Long id) {

        DbBaseEntity entity = repository.findOne(id);

        if(entity == null){
            return ResponseEntity.status(404).build();
        }

        DbBaseDto dto = new DbBaseDto();
        dto.id = entity.getId();
        dto.name = entity.getName();

        return ResponseEntity.ok(dto);
    }


    @RequestMapping(
            path = "/entitiesByName/{name}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<List<DbBaseDto>> getByName(@PathVariable String name) {

        List<DbBaseEntity> entities = repository.findByName(name);
        if (entities.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        List<DbBaseDto> list = entities.stream()
                .map(e -> new DbBaseDto(e.getId(), e.getName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }
}
