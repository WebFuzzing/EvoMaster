package com.example.demo.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BindCardReq implements Serializable {

    @JSONField(name = "id_card_no")
    @NotBlank
    @Size(min = 18, max = 18)
    @Pattern(regexp = "^[0-9]{17}[0-9Xx]$")
    private String idCardNo;

    @JSONField(name = "loan_person_name")
    @NotBlank
    @Size(max = 20)
    private String loanPersonName;

    @JSONField(name = "bank_card_no")
    @NotBlank
    @Size(min = 16, max = 19)
    @Pattern(regexp = "^[0-9]+$")
    private String bankCardNo;

    @JSONField(name = "bank_card_phone_number")
    @NotBlank
    @Size(min = 11, max = 11)
    @Pattern(regexp = "^[1][3-9][0-9]{9}$")
    private String bankCardPhoneNumber;

}

