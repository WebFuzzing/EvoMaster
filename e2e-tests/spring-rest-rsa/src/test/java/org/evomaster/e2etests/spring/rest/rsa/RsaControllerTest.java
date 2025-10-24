package org.evomaster.e2etests.spring.rest.rsa;

import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.TypeReference;
import com.example.demo.vo.BindCardReq;
import com.example.demo.vo.CommonReq;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RsaControllerTest {

    @Test
    public void testIssueInController() throws Exception {

        String jsonObject = "{\"data\":\"9HYNiE6jOTJ\", \"requestId\":\"c\", \"key\":\"\", \"bizData\":{\"idCardNo\":\"60592834345805788x\", \"loanPersonName\":\"Rv4aqwzQlyT0R\", \"bankCardNo\":\"969354799171580191\", \"bankCardPhoneNumber\":\"19446156283\"}}";

        ObjectMapper mapper = new ObjectMapper();

        //CommonReq<BindCardReq> req = JSON.parseObject(jsonObject, new TypeReference<CommonReq<BindCardReq>>() {});
        CommonReq<BindCardReq> req = mapper.readValue(jsonObject, new TypeReference<CommonReq<BindCardReq>>(){} );

        assertNotNull(req.getData());
        assertNotNull(req.getBizData().getIdCardNo());
    }
}
