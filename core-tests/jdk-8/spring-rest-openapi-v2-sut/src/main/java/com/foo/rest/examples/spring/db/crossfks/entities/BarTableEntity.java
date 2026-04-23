package com.foo.rest.examples.spring.db.crossfks.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Set;

@Entity
public class BarTableEntity {

    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = false)
    String name;

    @Column
    String description;

    @JsonIgnore
    @ManyToOne
    RootTableEntity rootTableEntity;


    public static BarTableEntity withName(RootTableEntity rootTableEntity, String barName) {
        BarTableEntity barTableEntity = new BarTableEntity();
        barTableEntity.name = barName;
        barTableEntity.rootTableEntity = rootTableEntity;
        return barTableEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RootTableEntity getRootTableDto() {
        return rootTableEntity;
    }

    public void setRootTableDto(RootTableEntity rootTableEntity) {
        this.rootTableEntity = rootTableEntity;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BarTableEntity)) return false;

        BarTableEntity barTableEntity = (BarTableEntity) o;

        if (!name.equals(barTableEntity.name)) return false;
        return rootTableEntity.equals(barTableEntity.rootTableEntity);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + rootTableEntity.hashCode();
        return result;
    }
}
