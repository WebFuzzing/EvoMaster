package com.foo.rest.examples.spring.db.disablesql;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Entity
public class DisableSqlEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private Integer intValue = Integer.MAX_VALUE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntValue(Integer newValue) {
        this.intValue = newValue;
    }

    public Integer getIntValue() {
        return this.intValue;
    }

}
