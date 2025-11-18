package com.opensearch.queries;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Product {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("rating")
    private Integer rating;
    
    @JsonProperty("inStock")
    private Boolean inStock;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("brand")
    private String brand;
    
    @JsonProperty("email")
    private String email;

    public Product() {}

    public Product(String id, String name, String description, String category, Double price, 
                   Integer rating, Boolean inStock, List<String> tags, String brand, String email) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.rating = rating;
        this.inStock = inStock;
        this.tags = tags;
        this.brand = brand;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
