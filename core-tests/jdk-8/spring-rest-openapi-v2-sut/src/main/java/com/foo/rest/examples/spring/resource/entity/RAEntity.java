package com.foo.rest.examples.spring.resource.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import com.foo.rest.examples.spring.resource.dto.*;
/** automatically created on 2019-08-29 */
@Entity
@Table(name = "RA")
public class RAEntity {
  public RAEntity() {}

  public RAEntity(Long id_var, String name_var, int value_var) {
    this.id = id_var;
    this.name = name_var;
    this.valueInt = value_var;
  }

  @Id @NotNull private Long id;
  @NotNull private String name;
  @NotNull private int valueInt;

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

  public void setValue(int valueInt) {
    this.valueInt = valueInt;
  }

  public int getValue() {
    return this.valueInt;
  }

  public RA getDto() {
    RA dto = new RA();
    dto.id = this.getId();
    dto.name = this.getName();
    dto.valueInt = this.getValue();
    return dto;
  }
}

