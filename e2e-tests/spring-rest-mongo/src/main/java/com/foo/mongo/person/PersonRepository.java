package com.foo.mongo.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

    List<Person> findByLastName(String lastName);

    Page<Person> findByFirstName(String firstName, Pageable pageable);

    Person findByAddress(Address address);

}
