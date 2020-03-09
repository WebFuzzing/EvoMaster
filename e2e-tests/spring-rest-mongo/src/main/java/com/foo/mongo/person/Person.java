package com.foo.mongo.person;

import org.springframework.data.annotation.Id;

public class Person {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private Address address;

    public Address getAddress() {
        return address;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getId() {
        return id;
    }

    public String getLastname() {
        return lastName;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public PersonDto toDto() {
        PersonDto dto = new PersonDto();
        dto.firstName = this.firstName;
        dto.lastName = this.lastName;
        dto.address = this.address.toDto();
        return dto;
    }

    public static Person fromDto(PersonDto dto) {
        Person p = new Person();
        p.firstName = dto.firstName;
        p.lastName= dto.lastName;
        p.address = Address.fromDto(dto.address);
        return p;
    }
}
