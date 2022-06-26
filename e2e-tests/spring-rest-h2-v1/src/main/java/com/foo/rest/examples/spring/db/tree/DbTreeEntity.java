package com.foo.rest.examples.spring.db.tree;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class DbTreeEntity {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private DbTreeEntity parent;

    public DbTreeEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DbTreeEntity getParent() {
        return parent;
    }

    public void setParent(DbTreeEntity parent) {
        this.parent = parent;
    }
}
