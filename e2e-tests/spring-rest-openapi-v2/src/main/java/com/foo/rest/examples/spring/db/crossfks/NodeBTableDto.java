package com.foo.rest.examples.spring.db.crossfks;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Set;

@Entity
public class NodeBTableDto {

    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = false)
    String name;

    @Column
    String description;

    @JsonIgnore
    @ManyToOne
    RootTableDto rootTableDto;

    @JsonIgnore
    @ManyToMany(mappedBy = "activedNodeBTableDtos")
    Set<NodeCTableDto> inNodeC;

    public static NodeBTableDto withName(RootTableDto rootTableDto, String nodeBName) {
        NodeBTableDto nodeBTableDto = new NodeBTableDto();
        nodeBTableDto.name = nodeBName;
        nodeBTableDto.rootTableDto = rootTableDto;
        return nodeBTableDto;
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

    public RootTableDto getRootTableDto() {
        return rootTableDto;
    }

    public void setRootTableDto(RootTableDto rootTableDto) {
        this.rootTableDto = rootTableDto;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeBTableDto)) return false;

        NodeBTableDto nodeBTableDto = (NodeBTableDto) o;

        if (!name.equals(nodeBTableDto.name)) return false;
        return rootTableDto.equals(nodeBTableDto.rootTableDto);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + rootTableDto.hashCode();
        return result;
    }
}
