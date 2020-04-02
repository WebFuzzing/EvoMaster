# Black-Box Testing

Informally, in *Black-Box Testing* of a RESTful API, we generate test cases without
knowing the internal details of the API.
Still, we need to know the schema of the API to determine which endpoints can be called,
and how.
Otherwise, sending random bits on a TCP socket will unlikely result in any meaningful, 
well formatted HTTP message.

Even with a schema, there is still the issue of generating inputs, like HTTP query parameters
and body payloads (e.g., in JSON/XML).
This can be done at random, but still within the constraints defined in the schema (e.g., the 
structure of the body payloads). 


To do this kind of testing, there is only the need to have an API up and running, and specify
where the schema can be found.
For example, [https://apis.guru](https://apis.guru/) lists many APIs online. 
Such website provides an API itself to query info on existing APIs.
Such small API (only 2 endpoints) can be easily tested by running the following on a command-line: 

```
java -jar core/target/evomaster.jar  --blackBox true --bbSwaggerUrl https://api.apis.guru/v2/specs/apis.guru/2.0.1/swagger.json  --outputFormat JAVA_JUNIT_4 --maxTime 30s
```

The command is doing the following:

* `java -jar core/target/evomaster.jar`: execute the _EvoMaster_ core process. 
  The executable `evomaster.jar` must be either [downloaded](download.md) 
  or [built from source](build.md).
* `--blackBox true`: by default, _EvoMaster_ does white-box testing. Here, we specify that
  we do black-box testing instead.
* `--bbSwaggerUrl ...`: URL of where the Swagger schema is. If it the API is running on a different
  host, then such different host would need to be specified with `--bbTargetUrl`.   
* `--outputFormat JAVA_JUNIT_4`: must specify how the tests will be generated, e.g., in Java
  using JUnit 4 in this case. Note: the language of the generated tests is not necessarily related
  to the language in which the tested application is implemented. 
* `--maxTime 30s`: for how long to run the search, i.e., just 30 seconds in this very simple example.

This command will create the following test suite, in which 2 `GET` calls are executed:

```
public class EvoMasterTest {

    private static String baseUrlOfSut = "https://api.apis.guru";
    
    @BeforeClass
    public static void initClass() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.urlEncodingEnabled = false;
        RestAssured.config = RestAssured.config()
            .jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE))
            .redirect(redirectConfig().followRedirects(false));
    }

    
    @Test
    public void test_0() throws Exception {
        
        given().accept("application/json")
                .get(baseUrlOfSut + "/v2/list.json")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("size()", numberMatches(1605));
    }
    
    
    @Test
    public void test_1() throws Exception {
        
        given().accept("application/json")
                .get(baseUrlOfSut + "/v2/metrics.json")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("'numAPIs'", numberMatches(1605.0))
                .body("'numEndpoints'", numberMatches(46276.0))
                .body("'numSpecs'", numberMatches(2869.0));
    }
}
```


## WARNING

Black-box testing is easy to do: first download the tool, and then just specify where the Swagger schema
can be found.
However, as it does know nothing about the internal details of the tested application, it is 
unlikely that it will get good results (i.e., in terms of code coverage and detected faults).

The first time you try _EvoMaster_, use black-box testing to get an idea of what _EvoMaster_
could do for you.
However, after an initial trial, we recommend to switch to [white-box testing](whitebox.md),
as it can lead to much, much better results.
