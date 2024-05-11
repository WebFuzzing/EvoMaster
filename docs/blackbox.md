# Black-Box Testing

## RESTful APIs

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
java -jar core/target/evomaster.jar  --blackBox true --bbSwaggerUrl https://api.apis.guru/v2/openapi.yaml  --outputFormat JAVA_JUNIT_4 --maxTime 30s --ratePerMinute 60
```

The command is doing the following:

* `java -jar core/target/evomaster.jar`: execute the _EvoMaster_ core process. 
  The executable `evomaster.jar` must be either [downloaded](download.md) 
  or [built from source](build.md).
* `--blackBox true`: by default, _EvoMaster_ does white-box testing. Here, we specify that
  we do black-box testing instead.
* `--bbSwaggerUrl ...`: URL of where the OpenAPI/Swagger schema is. The location of the API will be inferred from this schema (e.g., from `host` and `servers` tags). If such info is missing, then the API is assumed to be on same host as the schema. If needed, the API host location can be changed with the optional `--bbTargetUrl` (which overrides what specified in the schema).   
* `--outputFormat JAVA_JUNIT_4`: must specify how the tests will be generated, e.g., in Java
  using JUnit 4 in this case. Note: the language of the generated tests is not necessarily related
  to the language in which the tested application is implemented. 
* `--maxTime 30s`: for how long to run the search, i.e., just 30 seconds in this very simple example.
* `--ratePerMinute 60`: avoid doing a DoS attack by bombarding the remote service with too many HTTP calls in quick rapid succession. Limit to max 1 per second (i.e., 60 per minute) in this example. Note: if you are testing an API running on your machine (e.g., on `localhost`) then this parameter is not only __not required__, but also __detrimental__ for performance (i.e., do not use it).

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

### CLI Parameters

Note that there are several further parameters that can be configured ([see documentation](./options.md)).
These for example include options to specify where to store the generated files (`--outputFolder`), filter endpoints to test (e.g., `--endpointPrefix`), specify how test files will be named (e.g., `--outputFilePrefix` and `--outputFileSuffix`), etc.

Since version `3.0.0` these options can be specified in the generated `em.yaml` configuration files (so they do not need to be typed each time). 


### Issues with JDKs Above 8

The previous example run _EvoMaster_ directly from its JAR file, using the command:

```java -jar core/target/evomaster.jar```

To do this, you need to have a JDK installed on your machine, version 8 or later. 
An easier approach is to download and install _EvoMaster_ through its installer files (e.g., `.msi` for Windows), as those embed a JDK as well, with right configuration.
Then _EvoMaster_ can be run with for example `evomaster.exe` (for Windows).

If you want to run the JAR file directly with the JDK, you will encounter issues with versions from 17 on, due _integrity constraints_ on the JDK. 
You will have to manually add `--add-opens` commands like:

```java --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -jar core/target/evomaster.jar ```

If you fail to do that, _EvoMaster_ will crash, but at least it will tell you what to do (with the most update requirements, in case more `--add-opens` are required since this documentation was written).

Unfortunately, there is nothing we can do to address this major usability problem in the latest JDK versions :-( 

## GraphQL APIs

Black-box fuzzing of GraphQL APIs uses the same options as for RESTful APIs.
One difference is that `--bbTargetUrl` is used to specify the entry point of the GraphQL API.
Another difference is that we must specify the `--problemType` to be `GRAPHQL`, as the default is `REST`.
An example on GitLab's API is:

```
evomaster.exe  --problemType GRAPHQL --bbTargetUrl https://gitlab.com/api/graphql --blackBox true --outputFormat JAVA_JUNIT_4 --maxTime 30s --ratePerMinute 60
```


## AUTH

Since version `1.3.0`, it is possible to specify custom HTTP headers (e.g., to pass auth tokens), using the options from `--header0` to `--header2` (in case more than one HTTP header is needed). 
Many more options are now available since version `3.0.0`.
This is clarified in details in the [authentication documentation](auth.md).



## WARNING

Black-box testing is easy to do: first download the tool, and then just specify where the OpenAPI/GraphQL schema can be found.
However, as it does know nothing about the internal details of the tested application, it is  unlikely that black-box testing will get good results in terms of code coverage.
Albeit it can still detect many kinds faults (especially related to input validation).

The first time you try _EvoMaster_, use black-box testing to get an idea of what _EvoMaster_ could do for you.
However, after an initial trial, we recommend to switch to [white-box testing](whitebox.md),
as it can lead to much, much better results.
However, for the time being only programs running on the JVM (e.g., written in Java or Kotlin) are supported for white-box testing. 
