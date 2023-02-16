package com.foo.rest.examples.spring.db.crossfks;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class NodeCTableDto {

    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = false)
    String name;

    @Column
    Boolean valid = true;

    @JsonIgnore
    @ManyToOne
    RootTableDto rootTableDto;

    @ManyToMany(fetch = FetchType.EAGER)
    Set<NodeBTableDto> activedNodeBTableDtos = new HashSet<NodeBTableDto>();

    public Set<NodeBTableDto> getActivedNodeBs() {
        return activedNodeBTableDtos;
    }

    public Set<String> availableNodeBs() {
        return collectNodeBNames(rootTableDto.getNodeBTableDtos());
    }

    private Set<String> collectNodeBNames(Set<NodeBTableDto> nodeBsSet) {
        Set<String> nodeB = new HashSet<String>();
        for (NodeBTableDto nodeBTableDto : nodeBsSet) {
            nodeB.add(nodeBTableDto.name);
        }

        return nodeB;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RootTableDto getRootTableDto() {
        return rootTableDto;
    }

    public void setRootTableDto(RootTableDto rootTableDto) {
        this.rootTableDto = rootTableDto;
    }

    public Set<String> activedNodeBs() {
        return collectNodeBNames(activedNodeBTableDtos);
    }

    public Boolean isValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public void active(NodeBTableDto nodeBTableDto) {
        activedNodeBTableDtos.add(nodeBTableDto);

    }

    public void deactive(NodeBTableDto nodeBTableDto) {
        activedNodeBTableDtos.remove(nodeBTableDto);
    }

    public void active(String nodeBName) {
        NodeBTableDto nodeBTableDto = rootTableDto.findRootANodeBByName(nodeBName);
        active(nodeBTableDto);
    }

    public void deactive(String nodeBName) {
        NodeBTableDto nodeBTableDto = rootTableDto.findRootANodeBByName(nodeBName);
        deactive(nodeBTableDto);
    }

    public boolean hasActiveNodeBs(String nodeBName) {
        return activedNodeBs().contains(nodeBName);
    }
}
