package com.foo.rest.examples.spring.resource.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import com.foo.rest.examples.spring.resource.dto.*;
/** automatically created on 2019-08-29 */
@Entity
@Table(name = "RpR")
public class RpREntity {
  public RpREntity() {}

  public RpREntity(Long id_var, String name_var, int value_var, RdEntity rd_var) {
    this.id = id_var;
    this.name = name_var;
    this.valueInt = value_var;
    this.rd = rd_var;
  }

  @Id @NotNull private Long id;
  @NotNull private String name;
  @NotNull private int valueInt;
  @NotNull @OneToOne private RdEntity rd;

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

  public void setRd(RdEntity rd) {
    this.rd = rd;
  }

  public RdEntity getRd() {
    return this.rd;
  }

  public RpR getDto() {
    RpR dto = new RpR();
    dto.id = this.getId();
    dto.name = this.getName();
    dto.valueInt = this.getValue();
    dto.rdId = this.getRd().getId();
    return dto;
  }
}

