package com.foo.rest.examples.spring.db.jpa;


import javax.persistence.*;
import javax.validation.constraints.*;

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



    @NotBlank
    @Column(name="notblank")
    private String notblank;

    public String getNotBlank() {
        return notblank;
    }

    public void setNotBlank(String notBlank) {
        this.notblank = notBlank;
    }

    @Email
    @NotNull
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
