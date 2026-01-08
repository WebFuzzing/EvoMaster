package com.example.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.MySpringBootApplication;
import com.example.demo.vo.BindCardReq;
import com.example.demo.vo.CommonReq;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO: commented out, due to need update for database

//@SpringBootTest(classes = MySpringBootApplication.class)
//@AutoConfigureMockMvc
public class EmControllerTest {
//
//    @Autowired
//    private WebApplicationContext webApplicationContext;
//
//    private MockMvc mockMvc;
//
//    @InjectMocks
//    private DemoController demoController;
//
//    @BeforeEach
//    public void setup() {
//        MockitoAnnotations.openMocks(this);
//        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
//    }
//
//    @Test
//    public void testController() throws Exception {
//
//        CommonReq<BindCardReq> req = CommonReq.<BindCardReq>builder()
//                .requestId("4aJ7oE3NT")
//                .sign("_EM_1_XYZ_")
//                .bizData(null)
//                .build();
//
//        //String jsonObject = "{\"requestId\":\"4aJ7oE3NT\", \"sign\":\"_EM_1_XYZ_\"}";
//        String jsonObject = JSONObject.toJSONString(req);
//
//        EmController controller = new EmController();
//        String key = controller.deriveObjectParameterData("key",jsonObject,null);
//        req.setKey(key);
//        String data = controller.deriveObjectParameterData("data",jsonObject,null);
//        req.setData(data);
//
//        //SIGN depends on KEY and DATA
//        jsonObject = JSONObject.toJSONString(req);
//
//        String sign = controller.deriveObjectParameterData("sign",jsonObject,null);
//        req.setSign(sign);
//
//        String requestJson = JSONObject.toJSONString(req);
//
//        mockMvc.perform(post("/api/bind_card_apply")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestJson))
//                .andExpect(status().isOk());
//    }
}
