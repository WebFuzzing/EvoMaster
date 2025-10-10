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
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.problem.param.DerivedParamContext;
import org.evomaster.client.java.controller.problem.param.RestDerivedParam;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmController extends EmbeddedSutController {

    protected ConfigurableApplicationContext ctx;
    protected final Class<?> applicationClass;

    private static final String aesKey = RandomStringUtils.randomAlphanumeric(16);

    private Connection connection;

    protected static final String DB_NAME = "test";

    protected static final String MYSQL_TEST_USER_NAME = "test";

    protected static final String MYSQL_TEST_USER_PASSWORD = "test";


    protected static final String MYSQL_ROOT_USER_PASSWORD = "root";

    protected static final String MYSQL_ROOT_USER_NAME = "root";

    private static final int PORT = 3306;

    private static final String MYSQL_VERSION = "8.0.27";


    public static final GenericContainer mysql = new GenericContainer("mysql:" + MYSQL_VERSION)
            .withEnv(new HashMap<String, String>(){{
                put("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_USER_PASSWORD);
                put("MYSQL_DATABASE", DB_NAME);
                put("MYSQL_USER", MYSQL_TEST_USER_NAME);
                put("MYSQL_PASSWORD", MYSQL_TEST_USER_PASSWORD);
            }})
            .withExposedPorts(PORT);


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

        mysql.start();

        String dbUrl = getUrl();

        try {
            connection = DriverManager.getConnection(dbUrl, MYSQL_TEST_USER_NAME, MYSQL_TEST_USER_PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ctx = SpringApplication.run(applicationClass, "--server.port=0",
                "--spring.datasource.url="+dbUrl,
                "--spring.datasource.username="+MYSQL_TEST_USER_NAME,
                "--spring.datasource.password="+MYSQL_TEST_USER_PASSWORD
        );


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
        mysql.stop();
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
        return Arrays.asList(new DbSpecification(DatabaseType.MYSQL, connection));
    }


    public static String getUrl() {
        String host = mysql.getContainerIpAddress();
        int port = mysql.getMappedPort(PORT);
        String url = "jdbc:mysql://"+host+":"+port+"/"+DB_NAME;
        return url;
    }


    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
