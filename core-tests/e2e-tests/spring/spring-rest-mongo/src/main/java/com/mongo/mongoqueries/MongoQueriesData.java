package com.mongo.mongoqueries;

import org.bson.Document;

import java.util.List;

public class MongoQueriesData {

    private String id;
    private String name;
    private Integer age;
    private Long flags;
    private List<String> tags;
    private String description;
    private Document location;

    public MongoQueriesData() {
    }

    public MongoQueriesData(String id, String name, Integer age, Long flags, List<String> tags, String description, Document location) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.flags = flags;
        this.tags = tags;
        this.description = description;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Long getFlags() {
        return flags;
    }

    public void setFlags(Long flags) {
        this.flags = flags;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Document getLocation() {
        return location;
    }

    public void setLocation(Document location) {
        this.location = location;
    }
}
