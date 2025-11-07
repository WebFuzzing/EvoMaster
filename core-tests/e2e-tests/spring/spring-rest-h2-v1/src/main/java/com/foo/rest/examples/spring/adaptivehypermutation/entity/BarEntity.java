package com.foo.rest.examples.spring.adaptivehypermutation.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import com.foo.rest.examples.spring.adaptivehypermutation.dto.*;
/** automatically created on 2020-10-22 */
@Entity
@Table(name = "Bar")
public class BarEntity {
  public BarEntity() {}

  public BarEntity(Integer a, String b, Integer c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  @Id @NotNull private Integer a;
  @NotNull private String b;
  @NotNull private Integer c;

  public void setA(Integer id) {
    this.a = id;
  }

  public Integer getA() {
    return this.a;
  }

  public void setB(String name) {
    this.b = name;
  }

  public String getB() {
    return this.b;
  }

  public void setC(Integer value) {
    this.c = value;
  }

  public Integer getC() {
    return this.c;
  }

  public Bar getDto() {
    Bar dto = new Bar();
    dto.a = this.getA();
    dto.b = this.getB();
    dto.c = this.getC();
    return dto;
  }
}
