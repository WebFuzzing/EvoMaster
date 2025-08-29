package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.example.demo.MySpringBootApplication;
import com.example.demo.util.CryptoUtil;
import com.example.demo.vo.BindCardReq;
import com.example.demo.vo.CommonReq;
import org.apache.commons.lang3.RandomStringUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.problem.param.DerivedParamContext;
import org.evomaster.client.java.controller.problem.param.RestDerivedParam;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EmController extends EmbeddedSutController {

    protected ConfigurableApplicationContext ctx;
    protected final Class<?> applicationClass;

    private static final String aesKey = RandomStringUtils.randomAlphanumeric(16);

    public EmController() {
        super.setControllerPort(0);
        this.applicationClass = MySpringBootApplication.class;
    }


    @Override
    public String deriveObjectParameterData(String paramName, String jsonObject, String endpointPath) throws Exception {

        if(paramName.equals("sign")){
            CommonReq req = JSONObject.parseObject(jsonObject, CommonReq.class);
            String signText = req.signText();
            return CryptoUtil.sign(signText, DemoController.OTHER_PARTY_PRIVATE_KEY);
        }

        if(paramName.equals("key")){
            return CryptoUtil.encryptByPublicKey(aesKey, DemoController.YOUR_PUBLIC_KEY);
        }

        if(paramName.equals("data")){
            CommonReq<BindCardReq> req = JSON.parseObject(jsonObject, new TypeReference<CommonReq<BindCardReq>>() {});
            return CryptoUtil.encrypt(JSONObject.toJSONString(req.getBizData()), aesKey);
        }

        throw new IllegalArgumentException("Unrecognized parameter: " + paramName);
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v3/api-docs",
                null
        ).withDerivedParams(Arrays.asList(
                new RestDerivedParam("key", DerivedParamContext.BODY_PAYLOAD, null, 0),
                new RestDerivedParam("data", DerivedParamContext.BODY_PAYLOAD, null, 0),
                new RestDerivedParam("sign", DerivedParamContext.BODY_PAYLOAD, null, 1)
        ));
    }

    @Override
    public String startSut() {

        ctx = SpringApplication.run(applicationClass, "--server.port=0");


        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }




    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.";
    }

    @Override
    public void resetStateOfSUT() {
        //nothing to do
    }



    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }



    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
