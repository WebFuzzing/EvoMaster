package com.foo.rest.examples.spring.db.crossfks;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Set;

@Entity
public class NodeBTableEntity {

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

    @JsonIgnore
    @ManyToMany(mappedBy = "activedNodeBTableEntities")
    Set<NodeCTableEntity> inNodeC;

    public static NodeBTableEntity withName(RootTableEntity rootTableEntity, String nodeBName) {
        NodeBTableEntity nodeBTableEntity = new NodeBTableEntity();
        nodeBTableEntity.name = nodeBName;
        nodeBTableEntity.rootTableEntity = rootTableEntity;
        return nodeBTableEntity;
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
        if (!(o instanceof NodeBTableEntity)) return false;

        NodeBTableEntity nodeBTableEntity = (NodeBTableEntity) o;

        if (!name.equals(nodeBTableEntity.name)) return false;
        return rootTableEntity.equals(nodeBTableEntity.rootTableEntity);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + rootTableEntity.hashCode();
        return result;
    }
}
