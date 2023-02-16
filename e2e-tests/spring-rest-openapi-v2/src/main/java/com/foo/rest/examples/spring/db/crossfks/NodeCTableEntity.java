package com.foo.rest.examples.spring.db.crossfks;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class NodeCTableEntity {

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
    Set<NodeBTableEntity> activedNodeBTableEntities = new HashSet<NodeBTableEntity>();

    public Set<NodeBTableEntity> getActivedNodeBs() {
        return activedNodeBTableEntities;
    }

    public Set<String> availableNodeBs() {
        return collectNodeBNames(rootTableEntity.getNodeBTableDtos());
    }

    private Set<String> collectNodeBNames(Set<NodeBTableEntity> nodeBsSet) {
        Set<String> nodeB = new HashSet<String>();
        for (NodeBTableEntity nodeBTableEntity : nodeBsSet) {
            nodeB.add(nodeBTableEntity.name);
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

    public Set<String> activedNodeBs() {
        return collectNodeBNames(activedNodeBTableEntities);
    }

    public Boolean isValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public void active(NodeBTableEntity nodeBTableEntity) {
        activedNodeBTableEntities.add(nodeBTableEntity);

    }

    public void deactive(NodeBTableEntity nodeBTableEntity) {
        activedNodeBTableEntities.remove(nodeBTableEntity);
    }

    public void active(String nodeBName) {
        NodeBTableEntity nodeBTableEntity = rootTableEntity.findRootANodeBByName(nodeBName);
        active(nodeBTableEntity);
    }

    public void deactive(String nodeBName) {
        NodeBTableEntity nodeBTableEntity = rootTableEntity.findRootANodeBByName(nodeBName);
        deactive(nodeBTableEntity);
    }

    public boolean hasActiveNodeBs(String nodeBName) {
        return activedNodeBs().contains(nodeBName);
    }
}
