package com.foo.rest.examples.spring.db.existingdata;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

/**
 * Created by arcuri82 on 19-Jun-19.
 */
@Entity
public class ExistingDataEntityY {

    @Id @GeneratedValue
    private Long id;

    @NotNull @OneToOne
    private ExistingDataEntityX x;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExistingDataEntityX getX() {
        return x;
    }

    public void setX(ExistingDataEntityX x) {
        this.x = x;
    }
}
