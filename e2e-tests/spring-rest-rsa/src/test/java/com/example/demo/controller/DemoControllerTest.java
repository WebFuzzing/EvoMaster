package com.example.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.MySpringBootApplication;
import com.example.demo.util.CryptoUtil;
import com.example.demo.vo.BindCardReq;
import com.example.demo.vo.BindCardResp;
import com.example.demo.vo.CommonReq;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MySpringBootApplication.class)
@AutoConfigureMockMvc
public class DemoControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @InjectMocks
    private DemoController demoController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    public void testBindCardApply() throws Exception {
        BindCardReq bindCardReq = BindCardReq.builder()
                .idCardNo("110101199405059421")
                .bankCardNo("6216603505281538160")
                .loanPersonName("张三")
                .bankCardPhoneNumber("13818612679")
                .build();
        CommonReq<BindCardReq> req = CommonReq.<BindCardReq>builder()
                .appId("demo")
                .requestId("l1SY8ZTZtIZT4vaBCXhG5so7rUM2aIMp")
                .timestamp("1735110480398")
                .bizData(bindCardReq)
                .build();

        // Generate a sample AES key and encrypt the biz data using it
        String aesKey = RandomStringUtils.randomAlphanumeric(16); // In reality, you should generate this securely
        String encryptedBizData = CryptoUtil.encrypt(JSONObject.toJSONString(bindCardReq), aesKey);
        req.setData(encryptedBizData);

        // Encrypt the AES key using the other party's public key
        String encryptedAesKey = CryptoUtil.encryptByPublicKey(aesKey, DemoController.YOUR_PUBLIC_KEY);
        req.setKey(encryptedAesKey);

        // Sign the request using your private key
        String signText = req.signText(); // Assuming signText() is a method that constructs the text to be signed
        String sign = CryptoUtil.sign(signText, DemoController.OTHER_PARTY_PRIVATE_KEY);
        req.setSign(sign);

        // Convert the request to JSON
        String requestJson = JSONObject.toJSONString(req);
        BindCardResp resp = BindCardResp.builder()
                .sessionId("l1SY8ZTZtIZT4vaBCXhG5so7rUM2aIMp")
                .build();

        // Perform the POST request and verify the response
        mockMvc.perform(post("/api/bind_card_apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());

    }
}