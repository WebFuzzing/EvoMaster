package com.foo.rest.examples.spring.db.entity;


import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity(name = "BAR")
@Table(name = "HelloThere")
public class EntityData {
    public int getX0() {
        return x0;
    }

    public void setX0(int x0) {
        this.x0 = x0;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getX3() {
        return x3;
    }

    public void setX3(int x3) {
        this.x3 = x3;
    }

    public int getX4() {
        return x4;
    }

    public void setX4(int x4) {
        this.x4 = x4;
    }

    public int getX5() {
        return x5;
    }

    public void setX5(int x5) {
        this.x5 = x5;
    }

    public int getX6() {
        return x6;
    }

    public void setX6(int x6) {
        this.x6 = x6;
    }

    public int getX7() {
        return x7;
    }

    public void setX7(int x7) {
        this.x7 = x7;
    }

    public int getX8() {
        return x8;
    }

    public void setX8(int x8) {
        this.x8 = x8;
    }

    public int getX9() {
        return x9;
    }

    public void setX9(int x9) {
        this.x9 = x9;
    }

    public Long getX10() {
        return x10;
    }

    public void setX10(Long x10) {
        this.x10 = x10;
    }

    public Long getX11() {
        return x11;
    }

    public void setX11(Long x11) {
        this.x11 = x11;
    }

    public Long getX12() {
        return x12;
    }

    public void setX12(Long x12) {
        this.x12 = x12;
    }

    public Long getX13() {
        return x13;
    }

    public void setX13(Long x13) {
        this.x13 = x13;
    }

    public Long getX14() {
        return x14;
    }

    public void setX14(Long x14) {
        this.x14 = x14;
    }

    public Long getX15() {
        return x15;
    }

    public void setX15(Long x15) {
        this.x15 = x15;
    }

    public Long getX16() {
        return x16;
    }

    public void setX16(Long x16) {
        this.x16 = x16;
    }

    public Long getX17() {
        return x17;
    }

    public void setX17(Long x17) {
        this.x17 = x17;
    }

    public Long getX18() {
        return x18;
    }

    public void setX18(Long x18) {
        this.x18 = x18;
    }

    public Long getX19() {
        return x19;
    }

    public void setX19(Long x19) {
        this.x19 = x19;
    }

    public Double getY20() {
        return y20;
    }

    public void setY20(Double y20) {
        this.y20 = y20;
    }

    public Double getY21() {
        return y21;
    }

    public void setY21(Double y21) {
        this.y21 = y21;
    }

    public Double getY22() {
        return y22;
    }

    public void setY22(Double y22) {
        this.y22 = y22;
    }

    public Double getY23() {
        return y23;
    }

    public void setY23(Double y23) {
        this.y23 = y23;
    }

    public Double getY24() {
        return y24;
    }

    public void setY24(Double y24) {
        this.y24 = y24;
    }

    public Double getY25() {
        return y25;
    }

    public void setY25(Double y25) {
        this.y25 = y25;
    }

    public Double getY26() {
        return y26;
    }

    public void setY26(Double y26) {
        this.y26 = y26;
    }

    public Double getY27() {
        return y27;
    }

    public void setY27(Double y27) {
        this.y27 = y27;
    }

    public Double getY28() {
        return y28;
    }

    public void setY28(Double y28) {
        this.y28 = y28;
    }

    public Double getY29() {
        return y29;
    }

    public void setY29(Double y29) {
        this.y29 = y29;
    }

    @Id private int x0;
    private int x1;
    private int x2;
    private int x3;
    private int x4;
    private int x5;
    private int x6;
    private int x7;
    private int x8;
    private int x9;
    @NotNull private Long x10;
    @NotNull private Long x11;
    @NotNull private Long x12;
    @NotNull private Long x13;
    @NotNull private Long x14;
    @NotNull private Long x15;
    @NotNull private Long x16;
    @NotNull private Long x17;
    @NotNull private Long x18;
    @NotNull private Long x19;
    @Column(name = "x20") @NotNull private Double y20;
    @Column(name = "x21") @NotNull private Double y21;
    @Column(name = "x22") @NotNull private Double y22;
    @Column(name = "x23") @NotNull private Double y23;
    @Column(name = "x24") @NotNull private Double y24;
    @Column(name = "x25") @NotNull private Double y25;
    @Column(name = "x26") @NotNull private Double y26;
    @Column(name = "x27") @NotNull private Double y27;
    @Column(name = "x28") @NotNull private Double y28;
    @Column(name = "x29") @NotNull private Double y29;

    @NotNull
    @Column(name="z")
    @Enumerated(EnumType.STRING)
    private EntityEnum entityEnum;
}
