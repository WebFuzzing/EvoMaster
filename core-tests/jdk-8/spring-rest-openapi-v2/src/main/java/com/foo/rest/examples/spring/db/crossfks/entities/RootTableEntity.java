package com.foo.rest.examples.spring.db.crossfks.entities;


import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class RootTableEntity {

    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = false)
    String name;

    @OneToMany(mappedBy = "rootTableEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<BarTableEntity> rootBarTableEntities = new HashSet<BarTableEntity>();

    @JsonProperty("barTableDto")
    public Set<BarTableEntity> getBarTableDtos() {
        return rootBarTableEntities;
    }

    public Long getId() {
        return id;
    }

    public void addBar(BarTableEntity barTableEntity) {
        rootBarTableEntities.add(barTableEntity);
    }

    public void removeBar(BarTableEntity barTableEntity) {
        rootBarTableEntities.remove(barTableEntity);
    }

    public static RootTableEntity buildWithBar(String... nodeBNames) {
        RootTableEntity rootTableEntity = new RootTableEntity();
        for (String nodeBName : nodeBNames) {
            rootTableEntity.addBar(BarTableEntity.withName(rootTableEntity, nodeBName));
        }

        return rootTableEntity;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public BarTableEntity findRootBarByName(String nodeBName) {
        for (BarTableEntity barTableEntity : getBarTableDtos()) {
            if (barTableEntity.name.equalsIgnoreCase(nodeBName)) {
                return barTableEntity;
            }
        }

        return null;
    }

    public boolean hasBarNamed(String nodeBName) {
        for (BarTableEntity barTableEntity : getBarTableDtos()) {
            if (barTableEntity.name.equalsIgnoreCase(nodeBName)) {
                return true;
            }
        }

        return false;
    }
}
