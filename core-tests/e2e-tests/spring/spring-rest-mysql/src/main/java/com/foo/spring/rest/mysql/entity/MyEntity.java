package com.foo.spring.rest.mysql.entity;



import javax.persistence.*;
import javax.validation.constraints.Email;

@Entity
@Table(name ="myentities")
public class MyEntity {

    public MyEntity() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Email
    private String email;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public @Email String getEmail() {
        return email;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(@Email String email) {
        this.email = email;
    }
}
