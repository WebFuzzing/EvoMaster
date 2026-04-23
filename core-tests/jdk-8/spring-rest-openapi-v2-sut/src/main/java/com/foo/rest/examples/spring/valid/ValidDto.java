package com.foo.rest.examples.spring.valid;

import javax.validation.constraints.*;

public class ValidDto {

    @NotNull @AssertTrue public Boolean btrue0;
    @NotNull @AssertTrue public Boolean btrue1;
    @NotNull @AssertTrue public Boolean btrue2;
    @NotNull @AssertTrue public Boolean btrue3;
    @NotNull @AssertTrue public Boolean btrue4;

    @NotNull @AssertFalse public Boolean bfalse0;
    @NotNull @AssertFalse public Boolean bfalse1;
    @NotNull @AssertFalse public Boolean bfalse2;
    @NotNull @AssertFalse public Boolean bfalse3;
    @NotNull @AssertFalse public Boolean bfalse4;

    @NotNull @Positive public Integer ipos0;
    @NotNull @Positive public Integer ipos1;
    @NotNull @Positive public Integer ipos2;
    @NotNull @Positive public Integer ipos3;
    @NotNull @Positive public Integer ipos4;

    @NotNull @Negative public Integer ineg0;
    @NotNull @Negative public Integer ineg1;
    @NotNull @Negative public Integer ineg2;
    @NotNull @Negative public Integer ineg3;
    @NotNull @Negative public Integer ineg4;
}
