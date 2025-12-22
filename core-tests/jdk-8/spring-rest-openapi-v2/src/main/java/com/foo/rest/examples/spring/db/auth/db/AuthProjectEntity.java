package com.foo.rest.examples.spring.db.auth.db;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class AuthProjectEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;


    @ManyToOne @NotNull
    private AuthUserEntity owner;


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

    public AuthUserEntity getOwner() {
        return owner;
    }

    public void setOwner(AuthUserEntity owner) {
        this.owner = owner;
    }
}
