package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
Vulnerable application in which only authorized users can create or delete resources but
any user can modify resources.

This application was developed to satisfy the request:

An example application with 2 endpoints: PUT and DELETE on the same resource

/api/{x}

 */

@RestController
@RequestMapping(path = "/api")
public class ACDeletePutRest {

    // owners of each resource
    private static final Map<String, String> resourceOwners= new ConcurrentHashMap<>();

    // set of resources
    private static final Map<String, ACDeletePutDto> resources = new ConcurrentHashMap<>();


    public static void resetState(){
        resourceOwners.clear();
        resources.clear();
    }


    @GetMapping(value = "/{x}")
    public ResponseEntity getResource(@PathVariable("x") String x) {

        if (resources.containsKey(x)) {
            return ResponseEntity.status(HttpStatus.OK).body(resources.get(x));
        }
        else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Could not find the resource " +
                    "with the key: " + x);
        }
    }

    // put is to update an existing resource, this endpoint is vulnerable in the sense that
    // it does not check the owner of the resource before updating it
    @PutMapping(value = "/{x}")
    public ResponseEntity modifyResource(
            @PathVariable("x") String x,
            @RequestBody ACDeletePutDto newResource,
            Authentication authentication) {

        // Authorization is not checked here, but if the resource does not exist,
        // the caller is assigned as the owner of the resource

        if (!resources.containsKey(x)) {

            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CREATOR"))) {
                resourceOwners.put(x, authentication.getName());
                resources.put(x, newResource);

                return new ResponseEntity<>("Created the resource successfully ", HttpStatus.CREATED);

            } else {
                return new ResponseEntity<>("Only users with the role CREATOR can create resources ",
                        HttpStatus.FORBIDDEN);
            }
        }
        else {

            // no authorization check for an existing resource, which is a security issue.
            resources.put(x, newResource);

            return new ResponseEntity<>("Modified the existing resource successfully ",
                    HttpStatus.NO_CONTENT);
        }
    }

    // delete endpoint checks the owner of the resource to make sure the resource cannot be deleted
    @DeleteMapping(value = "/{x}")
    public ResponseEntity deleteResource(@PathVariable("x") String x,Authentication authentication) {

        if (!resources.containsKey(x)) {
            return new ResponseEntity<>("The resource does not exist ", HttpStatus.NOT_FOUND);
        } else {

            // only the owner of the resource can delete the resource
            if (resourceOwners.get(x).equals(authentication.getName())) {
                resources.remove(x);
                resourceOwners.remove(x);

                return new ResponseEntity<>("Resource has been successfully deleted ",
                            HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Only the resource owner is authorized to delete the resource ",
                        HttpStatus.FORBIDDEN);
                }
        }

    }
}
