package com.foo.rest.examples.spring.namedresource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by arcand on 01.03.17.
 */
@RestController
@RequestMapping(path = "/api/nr")
public class NamedResourceRest {

    //recall, this is just an example for testing
    public static final Map<String, String> data = new ConcurrentHashMap<>();


    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity create(@PathVariable("id") String id,
                                 @RequestBody NamedResourceDto dto) {

        data.put(id, dto.value);

        return ResponseEntity.status(201).build();
    }


    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<NamedResourceDto> get(@PathVariable("id") String id) {

        if (!data.containsKey(id)) {
            return ResponseEntity.notFound().build();
        } else {
            NamedResourceDto dto = new NamedResourceDto();
            dto.name = id;
            dto.value = data.get(id);

            return ResponseEntity.ok().body(dto);
        }
    }

    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.DELETE
    )
    public ResponseEntity delete(@PathVariable("id") String id) {

        if (!data.containsKey(id)) {
            return ResponseEntity.notFound().build();
        } else {
            data.remove(id);
            return ResponseEntity.noContent().build();
        }
    }

    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity replace(@PathVariable("id") String id,
                                  @RequestBody NamedResourceDto dto) {

        if (!data.containsKey(id)) {
            //in this case, the PUT will create it
            data.put(id, dto.value);
            return ResponseEntity.status(201).build();
        } else {
            data.put(id, dto.value);
            return ResponseEntity.noContent().build();
        }
    }

    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity update(@PathVariable("id") String id,
                                 @RequestBody NamedResourceDto dto) {

        if (!data.containsKey(id)) {
            return ResponseEntity.status(404).build();
        } else {
            data.put(id, dto.value);
            return ResponseEntity.noContent().build();
        }
    }

}
