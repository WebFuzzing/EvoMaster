package com.mongo.notinquery;

import org.springframework.data.annotation.Id;

public class MongoNotInQueryData {

    @Id
    private String id;

    private Integer x;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }
}
