package com.foo.customer;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CustomerMongoRepository extends MongoRepository<CustomerEntity, String> {

    CustomerEntity findByFirstName(String firstName);

    List<CustomerEntity> findByLastName(String lastName);

}