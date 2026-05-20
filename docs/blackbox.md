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
Such small API  can be easily tested by running the following on a command-line: 

```
java -jar core/target/evomaster.jar  --blackBox true --schema https://api.apis.guru/v2/openapi.yaml  --outputFormat JAVA_JUNIT_4 --maxTime 30s --ratePerMinute 60
```

The command is doing the following:

* `java -jar core/target/evomaster.jar`: execute the _EvoMaster_ core process. 
  The executable `evomaster.jar` must be either [downloaded](download.md) 
  or [built from source](build.md) (in which case it will end up under the `core/target/` folder of the cloned repository).
* `--blackBox true`: technically, a redundant parameter, as black-box testing is the default mode since version 6.0.0. It can be omitted.
* `--schema ...`: URL of where the OpenAPI/Swagger schema is. The location of the API will be inferred from this schema (e.g., from `host` and `servers` tags). If such info is missing, then the API is assumed to be on same host as the schema. If needed, the API host location can be changed with the optional `--base` (which overrides what specified in the schema).   
* `--outputFormat JAVA_JUNIT_4`: must specify how the tests will be generated, e.g., in Java
  using JUnit 4 in this case. Note: the language of the generated tests is not necessarily related
  to the language in which the tested application is implemented. 
* `--maxTime 30s`: for how long to run the search, i.e., just 30 seconds in this very simple example.
* `--ratePerMinute 60`: avoid doing a DoS attack by bombarding the remote service with too many HTTP calls in quick rapid succession (especially useful if the tested API has no rate limiter). Limit to max 1 per second (i.e., 60 per minute) in this example. Note: if you are testing an API running on your machine (e.g., on `localhost`) then this parameter is not only __not required__, but also __detrimental__ for performance (i.e., do not use it). Also note that EvoMaster automatically handles 429 responses, by waiting the specified amount of time in the Retry-After header. 

This command will create a test suite under  the folder `generated_tests`, including an interactive web report. 

### CLI Parameters

Note that there are several further parameters that can be configured ([see documentation](./options.md)).
These for example include options to specify where to store the generated files (`--outputFolder`), filter endpoints to test (e.g., `--endpointPrefix`), specify how test files will be named (e.g., `--outputFilePrefix` and `--outputFileSuffix`), etc.

Since version `3.0.0` these options can be specified in the generated `em.yaml` configuration files (so they do not need to be typed each time). 


## GraphQL APIs

Black-box fuzzing of GraphQL APIs uses the same options as for RESTful APIs.
One difference is that `--base` is used to specify the entry point of the GraphQL API.
Another difference is that we must specify the `--problemType` to be `GRAPHQL`, as the default is `REST`.
A working example is:

```
evomaster  --problemType GRAPHQL --base https://rickandmortyapi.com/graphql/ --maxTime 30s --ratePerMinute 60
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
However, after an initial trial, if your APIs run on the JVM, we recommend to switch to [white-box testing](whitebox.md),
as it can lead to much, much better results.
However, as previously stated, for the time being only programs running on the JVM (e.g., written in Java or Kotlin) are supported for white-box testing. 
