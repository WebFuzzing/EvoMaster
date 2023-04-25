package com.foo.rest.examples.spring.db.jpa;


import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Entity
@Table(name = "ExistingTable")
public class EntityJPAData {


    private @Id int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id= id;
    }


    @Min(value=42)
    @Max(value=45)
    private int x1;

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }
}
