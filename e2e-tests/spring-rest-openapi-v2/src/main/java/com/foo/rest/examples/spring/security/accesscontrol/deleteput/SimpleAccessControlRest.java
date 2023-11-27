package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;

/*
Vulnerable application in which only authorized users can create or delete resources but
any user can modify resources.

This application was developed to satisfy the request:

delete this file once added example

each E2E will have its own subpackage here

example, simple with only 2 endpoints: PUT and DELETE on

/api/{x}

 */

@RestController
@RequestMapping(path = "/api")
public class SimpleAccessControlRest {

    // owners of each resource
    private HashMap<String, String> resourceOwners= new HashMap<String, String>();

    // set of resources
    private HashMap<String, ResourceDto> resources = new HashMap<String, ResourceDto>();


    //


    @GetMapping(value = "/{x}")
    public ResourceDto getResource(@PathVariable("x") String x) {

        return resources.get(x);
    }

    @PostMapping(value = "/{x}")
    public ResponseEntity createResource(@PathVariable("x") String x, @RequestParam String stringValue, @RequestParam int integerValue, @RequestParam boolean booleanValue ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // anonymous users cannot create resources
        if (authentication.getPrincipal().equals("anonymousUser")) {

            return new ResponseEntity<>("Only authenticated users can create resources ", HttpStatus.UNAUTHORIZED);
        }
        // the user is not anonymous
        else {

            String creatorUser = authentication.getName();

            ResourceDto newResource = new ResourceDto();
            newResource.stringValue = stringValue;
            newResource.integerValue = integerValue;
            newResource.booleanValue = booleanValue;

            // if the resource exists and the user sending this request is the resource owner, overwrite the resource
            if (resources.containsKey(x)) {

                if (resourceOwners.get(x).equals(creatorUser)) {

                    resources.put(x, newResource);
                    return new ResponseEntity<>("Resource has been successfully recreated by its owner ",
                            HttpStatus.OK);
                }
                else {
                    return new ResponseEntity<>("Only the owner can recreate the resource ", HttpStatus.UNAUTHORIZED);
                }
            }
            else {

                // if the user has role CREATOR, create the resource
                if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CREATOR"))) {
                    resources.put(x, newResource);
                    resourceOwners.put(x, creatorUser);

                    return new ResponseEntity<>("New resource has been successfully created ",
                            HttpStatus.OK);
                }
                else {
                    return new ResponseEntity<>("Only users with creator role can create resources ",
                            HttpStatus.UNAUTHORIZED);
                }

            }
        }
    }

    // put is to update an existing resource, this endpoint is vulnerable in the sense that
    // it does not check the owner of the resource before updating it
    @PutMapping(value = "/{x}")
    public ResponseEntity modifyResource(@PathVariable("x") String x, @RequestParam String stringValue, @RequestParam int integerValue, @RequestParam boolean booleanValue ) {

        ResourceDto newResource = new ResourceDto();
        newResource.stringValue = stringValue;
        newResource.integerValue = integerValue;
        newResource.booleanValue = booleanValue;

        resources.put(x, newResource);

        return new ResponseEntity<>("Modified the resource successfully ",
                HttpStatus.OK);

    }

    // delete endpoint checks the owner of the resource to make sure the resource cannot be deleted
    @DeleteMapping(value = "/{x}")
    public ResponseEntity deleteResource(@PathVariable("x") String x) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // anonymous users cannot delete resources
        if (authentication.getPrincipal().equals("anonymousUser")) {

            return new ResponseEntity<>("Only authenticated users can delete resources ", HttpStatus.UNAUTHORIZED);
        } else {

            if (!resources.containsKey(x)) {
                return new ResponseEntity<>("The resource does not exist ", HttpStatus.INTERNAL_SERVER_ERROR);
            } else {

                // only the owner of the resource can delete the resource
                if (resourceOwners.get(x).equals(authentication.getName())) {
                    resources.remove(x);
                    resourceOwners.remove(x);

                    return new ResponseEntity<>("Resource has been successfully deleted ",
                            HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("The user is not authorized to delete the resource ",
                            HttpStatus.UNAUTHORIZED);
                }
            }
        }
    }
}
