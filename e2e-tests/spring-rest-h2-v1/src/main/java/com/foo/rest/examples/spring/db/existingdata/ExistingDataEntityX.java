package com.foo.rest.examples.spring.db.existingdata;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

/**
 * Created by arcuri82 on 19-Jun-19.
 */
@Entity
public class ExistingDataEntityX {

    @Id
    private Long id;

    @NotNull
    private String name;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
