package com.foo.customer;

import org.springframework.data.annotation.Id;

public class CustomerEntity {

    @Id
    public String id;
    public String firstName;
    public String lastName;

    public CustomerEntity() {
    }

    public CustomerEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format(
                "Customer[firstName='%s', lastName='%s']",
                firstName, lastName);
    }

}
