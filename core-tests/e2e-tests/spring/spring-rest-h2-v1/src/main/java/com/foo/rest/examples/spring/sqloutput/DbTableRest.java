package com.foo.rest.examples.spring.sqloutput;

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
@RequestMapping(path = "/api/db/sql/output")
public class DbTableRest {

    @Autowired
    private DbTableRepository repository;


    @RequestMapping(
            path = "",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<Void> create(@NotNull @RequestBody DbTableDto dto) {

        DbTableEntity entity = new DbTableEntity();
        entity.setName(dto.name);

        repository.save(entity);
        long id = entity.getId();

        return ResponseEntity.created(URI.create("/api/db/sql/output/" + id)).build();
    }

    @RequestMapping(
            path = "",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public List<DbTableDto> getAll() {

        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(e -> new DbTableDto(e.getId(), e.getName()))
                .collect(Collectors.toList());
    }

    @RequestMapping(
            path = "/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<DbTableDto> get(@PathVariable("id") Long id) {

        DbTableEntity entity = repository.findById(id).orElse(null);

        if(entity == null){
            return ResponseEntity.status(404).build();
        }

        DbTableDto dto = new DbTableDto();
        dto.id = entity.getId();
        dto.name = entity.getName();

        return ResponseEntity.ok(dto);
    }


    @RequestMapping(
            path = "/{name}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<List<DbTableDto>> getByName(@PathVariable String name) {

        List<DbTableEntity> entities = repository.findByName(name);
        if (entities.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        List<DbTableDto> list = entities.stream()
                .map(e -> new DbTableDto(e.getId(), e.getName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }
}
