package com.foo.rest.examples.spring.db.crossfks;


import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class RootTableDto {

    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = false)
    String name;

    @OneToMany(mappedBy = "rootTableDto", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<NodeBTableDto> rootANodeBTableDtos = new HashSet<NodeBTableDto>();

    @JsonProperty("nodeBTableDto")
    public Set<NodeBTableDto> getNodeBTableDtos() {
        return rootANodeBTableDtos;
    }

    public Long getId() {
        return id;
    }

    public void addNodeB(NodeBTableDto nodeBTableDto) {
        rootANodeBTableDtos.add(nodeBTableDto);
    }

    public void removeNodeB(NodeBTableDto nodeBTableDto) {
        rootANodeBTableDtos.remove(nodeBTableDto);
    }

    public static RootTableDto buildWithNodeBs(String... nodeBNames) {
        RootTableDto rootTableDto = new RootTableDto();
        for (String nodeBName : nodeBNames) {
            rootTableDto.addNodeB(NodeBTableDto.withName(rootTableDto, nodeBName));
        }

        return rootTableDto;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public NodeBTableDto findRootANodeBByName(String nodeBName) {
        for (NodeBTableDto nodeBTableDto : getNodeBTableDtos()) {
            if (nodeBTableDto.name.equalsIgnoreCase(nodeBName)) {
                return nodeBTableDto;
            }
        }

        return null;
    }

    public boolean hasNodeBNamed(String nodeBName) {
        for (NodeBTableDto nodeBTableDto : getNodeBTableDtos()) {
            if (nodeBTableDto.name.equalsIgnoreCase(nodeBName)) {
                return true;
            }
        }

        return false;
    }
}
