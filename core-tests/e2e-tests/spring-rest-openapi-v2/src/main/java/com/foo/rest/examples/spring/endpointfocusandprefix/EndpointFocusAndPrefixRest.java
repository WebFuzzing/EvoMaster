package com.foo.rest.examples.spring.endpointfocusandprefix;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

/**
 * Application for testing focus and prefix endpoints, inspired from petStore API:
 * <a href="https://petstore.swagger.io/">PetStore API</a>
 */
@RestController
@SuppressWarnings("unused")
public class EndpointFocusAndPrefixRest {

    @ApiOperation("Get a pet according to the given pet id ")
    @RequestMapping(
            value = "/api/pet/{petId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String getPetById(    @ApiParam("Value to retrieve")
                              @PathVariable("petId")
                              Integer value) {
        return value + " retrieved";
    }

    @ApiOperation("Update a pet according to the given pet id")
    @RequestMapping(
            value = "/api/pet/{petId}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public String updatePetById(    @ApiParam("Value to store")
                              @PathVariable("petId")
                              Integer value) {
        return value + " updated";
    }

    @ApiOperation("Delete a pet according to the given pet id")
    @RequestMapping(
            value = "/api/pet/{petId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON
    )
    public String deletePetById(    @ApiParam("Value to delete")
                                   @PathVariable("petId")
                                   Integer value) {
        return value + " deleted";
    }

    @ApiOperation("Upload image of a pet according to the given pet id")
    @RequestMapping(
            value = "/api/pet/{petId}/uploadImage",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public String uploadImageForPet(    @ApiParam("Pet ID to upload")
                                    @PathVariable("petId")
                                    Integer value) {
        return "Image uploaded for the pet: " + value;
    }

    @ApiOperation("Add a pet to the store")
    @RequestMapping(
            value = "/api/pet",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public String addNewPet() {
        return "A new pet has been added";
    }

    @ApiOperation("Update an existing pet")
    @RequestMapping(
            value = "/api/pet",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON
    )
    public String updatePet() {
        return "An existing pet has been updated";
    }

    @ApiOperation("Finds pets by the given status")
    @RequestMapping(
            value = "/api/pet/findByStatus",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public List<String> findByStatus() {
        return Arrays.asList("pet1", "pet2", "pet3", "pet4");
    }

    @ApiOperation("Finds items in the store inventory")
    @RequestMapping(
            value = "/api/store/inventory",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public List<String> getStoreInventory() {
        return Arrays.asList("item", "item2", "item3");
    }


    @ApiOperation("Find a given order according to orderID ")
    @RequestMapping(
            value = "/api/store/order/{orderID}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String findStoreOrderById(    @ApiParam("order ID to check")
                                  @PathVariable("orderID")
                                  Integer value) {
        return "Information for the order: " + value;
    }

    @ApiOperation("Delete a given order according to orderID ")
    @RequestMapping(
            value = "/api/store/order/{orderID}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON
    )
    public String deleteStoreOrderById(    @ApiParam("order ID to check")
                                         @PathVariable("orderID")
                                         Integer value) {
        return "Deleted the order: " + value;
    }

    @ApiOperation("Place an order")
    @RequestMapping(
            value = "/api/store/order",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public String placeOrder() {
        return "Order Placed";
    }

    @ApiOperation("Get information about a user")
    @RequestMapping(
            value = "/api/user/{username}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String getUserByName(    @ApiParam("username to retrieve")
                                    @PathVariable("username")
                                    String name) {
        return "Retrieved information for the user " + name;
    }

    @ApiOperation("Update information about a user")
    @RequestMapping(
            value = "/api/user/{username}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON
    )
    public String updateUserInformation(
            @ApiParam("username to update")
            @PathVariable("username")
            String name) {
        return "Updated information for the user " + name;
    }

    @ApiOperation("Delete information about a user")
    @RequestMapping(
            value = "/api/user/{username}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON
    )
    public String deleteUserInformation(
            @ApiParam("username to delete")
            @PathVariable("username")
            String name) {
        return "Deleted information for the user " + name;
    }

    @ApiOperation("Create a new user")
    @RequestMapping(
            value = "/api/user",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public String createUser() {
        return "Created a new user";
    }

    @ApiOperation("Create users given with a given list")
    @RequestMapping(
            value = "/api/user/createWithList",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public String createUserWithList(
            @RequestBody List<EndpointFocusAndPrefixRestDTO>
                    userlist) {

        StringBuilder reportBuilder = new StringBuilder("Creating users\n");
        for(EndpointFocusAndPrefixRestDTO dto : userlist) {

            reportBuilder.append("-------\n");
            reportBuilder.append("ID: ").append(dto.id).append("\n");
            reportBuilder.append("Username: ").append(dto.userName).append("\n");
            reportBuilder.append("Firstname: ").append(dto.firstName).append("\n");
            reportBuilder.append("Lastname: ").append(dto.lastName).append("\n");
            reportBuilder.append("-------\n");
        }
        String report = reportBuilder.toString();

        report = report + "Created all users\n";

        return report;
    }

    @ApiOperation("Logs in a user")
    @RequestMapping(
            value = "/api/user/login",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String userLogin() {

        return "Logged in a user";
    }

    @ApiOperation("Logs out a user")
    @RequestMapping(
            value = "/api/user/logout",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String userLogout() {

        return "Logged out a user";
    }
}
