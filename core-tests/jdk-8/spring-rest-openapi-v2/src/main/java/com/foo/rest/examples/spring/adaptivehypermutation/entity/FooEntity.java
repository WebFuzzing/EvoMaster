package com.foo.rest.examples.spring.adaptivehypermutation.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import com.foo.rest.examples.spring.adaptivehypermutation.dto.*;

import java.sql.Date;
import java.time.LocalDate;

/** automatically created on 2020-10-22 */
@Entity
@Table(name = "Foo")
public class FooEntity {
  public FooEntity() {}

  public FooEntity(Integer x, String y, Integer zc, Date zt, String zd) {
    this.x = x;
    this.y = y;
    this.zc = zc;
    this.zt = zt;
    this.zd1 = zd;
  }

  @Id @NotNull @Min(0) private Integer x;
  @NotNull private String y;
  @NotNull private Integer zc;
  @NotNull private Date zt;
  private String zd1;
  private String zd2;
  private String zd3;

  public void setX(Integer id) {
    this.x = id;
  }

  public Integer getX() {
    return this.x;
  }

  public void setY(String name) {
    this.y = name;
  }

  public String getY() {
    return this.y;
  }

  public void setZc(Integer value) {
    this.zc = value;
  }

  public Integer getZc() {
    return this.zc;
  }

  public Date getZt() {
    return zt;
  }

  public void setZt(Date zt) {
    this.zt = zt;
  }

  public String getZd1() {
    return zd1;
  }

  public void setZd1(String zd) {
    this.zd1 = zd;
  }

  public String getZd2() {
    return zd2;
  }

  public void setZd2(String zd2) {
    this.zd2 = zd2;
  }

  public String getZd3() {
    return zd3;
  }

  public void setZd3(String zd3) {
    this.zd3 = zd3;
  }

  public void setZ(Info info){
    this.zc = info.c;
    this.zt = Date.valueOf(LocalDate.parse(info.t));
    this.zd1 = info.d1;
    this.zd2 = info.d2;
    this.zd3 = info.d3;
  }

  public Foo getDto() {
    Foo dto = new Foo();
    dto.x = this.getX();
    dto.y = this.getY();
    dto.z = new Info(this.zc, this.zt.toString(), this.zd1, this.zd2, this.zd3);
    return dto;
  }
}
