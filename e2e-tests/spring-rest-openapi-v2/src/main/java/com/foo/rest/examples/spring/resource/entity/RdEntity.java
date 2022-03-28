package com.foo.rest.examples.spring.resource.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import com.foo.rest.examples.spring.resource.dto.*;
/** automatically created on 2019-08-29 */
@Entity
@Table(name = "Rd")
public class RdEntity {
  public RdEntity() {}

  public RdEntity(Long id_var, String name_var, int value_var) {
    this.id = id_var;
    this.name = name_var;
    this.value = value_var;
  }

  @Id @NotNull private Long id;
  @NotNull private String name;
  @NotNull private int value;

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return this.id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public int getValue() {
    return this.value;
  }

  public Rd getDto() {
    Rd dto = new Rd();
    dto.id = this.getId();
    dto.name = this.getName();
    dto.value = this.getValue();
    return dto;
  }
}

