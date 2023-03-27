package com.foo.rest.examples.spring.db.crossfks.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class FooTableEntity {

    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = false)
    String name;

    @Column
    Boolean valid = true;

    @JsonIgnore
    @ManyToOne
    RootTableEntity rootTableEntity;

    @ManyToMany(fetch = FetchType.EAGER)
    Set<BarTableEntity> activatedBarTableEntities = new HashSet<BarTableEntity>();

    public Set<BarTableEntity> getActivatedBars() {
        return activatedBarTableEntities;
    }

    public Set<String> availableBars() {
        return collectBarNames(rootTableEntity.getBarTableDtos());
    }

    private Set<String> collectBarNames(Set<BarTableEntity> nodeBsSet) {
        Set<String> nodeB = new HashSet<String>();
        for (BarTableEntity barTableEntity : nodeBsSet) {
            nodeB.add(barTableEntity.name);
        }

        return nodeB;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RootTableEntity getRootTableDto() {
        return rootTableEntity;
    }

    public void setRootTableDto(RootTableEntity rootTableEntity) {
        this.rootTableEntity = rootTableEntity;
    }

    public Set<String> activatedBars() {
        return collectBarNames(activatedBarTableEntities);
    }

    public Boolean isValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public void active(BarTableEntity barTableEntity) {
        activatedBarTableEntities.add(barTableEntity);

    }

    public void deactivate(BarTableEntity barTableEntity) {
        activatedBarTableEntities.remove(barTableEntity);
    }

    public void active(String barName) {
        BarTableEntity barTableEntity = rootTableEntity.findRootBarByName(barName);
        active(barTableEntity);
    }

    public void deactivate(String barName) {
        BarTableEntity barTableEntity = rootTableEntity.findRootBarByName(barName);
        deactivate(barTableEntity);
    }

    public boolean hasActiveBars(String barName) {
        return activatedBars().contains(barName);
    }
}
