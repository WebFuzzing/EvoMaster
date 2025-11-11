package com.example.demo.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BindCardResp extends BaseResult implements Serializable {

    @JSONField(name = "sessionId")
    private String sessionId;
}