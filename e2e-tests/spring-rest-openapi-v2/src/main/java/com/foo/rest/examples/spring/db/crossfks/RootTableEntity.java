package com.foo.rest.examples.spring.db.crossfks;


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
    Set<NodeBTableEntity> rootANodeBTableEntities = new HashSet<NodeBTableEntity>();

    @JsonProperty("nodeBTableDto")
    public Set<NodeBTableEntity> getNodeBTableDtos() {
        return rootANodeBTableEntities;
    }

    public Long getId() {
        return id;
    }

    public void addNodeB(NodeBTableEntity nodeBTableEntity) {
        rootANodeBTableEntities.add(nodeBTableEntity);
    }

    public void removeNodeB(NodeBTableEntity nodeBTableEntity) {
        rootANodeBTableEntities.remove(nodeBTableEntity);
    }

    public static RootTableEntity buildWithNodeBs(String... nodeBNames) {
        RootTableEntity rootTableEntity = new RootTableEntity();
        for (String nodeBName : nodeBNames) {
            rootTableEntity.addNodeB(NodeBTableEntity.withName(rootTableEntity, nodeBName));
        }

        return rootTableEntity;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public NodeBTableEntity findRootANodeBByName(String nodeBName) {
        for (NodeBTableEntity nodeBTableEntity : getNodeBTableDtos()) {
            if (nodeBTableEntity.name.equalsIgnoreCase(nodeBName)) {
                return nodeBTableEntity;
            }
        }

        return null;
    }

    public boolean hasNodeBNamed(String nodeBName) {
        for (NodeBTableEntity nodeBTableEntity : getNodeBTableDtos()) {
            if (nodeBTableEntity.name.equalsIgnoreCase(nodeBName)) {
                return true;
            }
        }

        return false;
    }
}
