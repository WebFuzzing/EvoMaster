# Version: SNAPSHOT

Under development in `master` branch.

### New Features
- Generated test suite files have now a license disclaimer stating these generated files are not subject to LGPL.
- Support for object in example/examples in OpenAPI schemas.
- In REST APIs, if OpenAPI schema has $ref entries pointing to external schema files, those will be automatically downloaded and processed.
- Now generated tests have meaningful names, instead of being just numbers  like _test01()_.

### Addressed GitHub Issues
- #1171: IllegalStateException: only support Map with String key in EvoMaster
- #1159: EvoMaster crashed when reading em.yaml file 


# Version: 3.4.0

### New Features
- Now EvoMaster is released also on Docker Hub, with id `webfuzzing/evomaster`. 
  However, this is only for black-box mode. For white-box, it is still recommended to use an OS installer or the uber-jar file from release page.
- For improving readability, generated tests now have summary comments (e.g., as JavaDoc for Java/Kotlin outputs).

### Bug Fixes
- Fixed missing java.util.Arrays in generated files, leading to compilation errors in some cases involving SQL database resets. 

### Addressed GitHub Issues
- #1150: java.util.NoSuchElementException: Key org.evomaster.core.search.gene.optional.OptionalGene@5dc8227c is missing in the map
- #301: convert to Docker

# Version: 3.3.0

### New Features
- MongoDB support. For white-box heuristics, can analyze all queries done toward MongoDB databases, as well as being able to insert data directly as part of the generated test cases.
- improved fault detection for OpenAPI schema faults, in particular regarding the structure of the received responses, which are now validated. 
- improved coverage criteria for black-box testing for REST APIs.
- support for exploiting "links" declarations in OpenAPI schemas. 
- improved re-used of data between endpoints (e.g., data returned from GET requests can be used as input for following requests using fields with similar names). 

### Bug Fixes
- bbSwaggerUrl now works when using a local path instead of URL (as was stated in documentation).
- fixed wrong handling of "date-time" format in OpenAPI schemas. 


# Version: 3.2.0

### New Features
- when running JAR file with JDK 17+, no longer need to manually specify --add-opens commands
- schema validation for OpenAPI schemas, with summary of issues printed in the console logs
- generated tests that detect faults have now comments highlighting those faults. 

### Bug Fixes
- fixed release of JAR requiring JDK 21 by mistake
- fixed issue in Python and JavaScript output for when JSON responses are wrongly free text (e.g., unquoted strings)

### Addressed GitHub Issues

- #765: Details how to use evomaster + info on found faults
- #822: additionalProperties: [true/false] causes crash
- #986: Should this test be in EvoMaster_fault_representatives_Test.java and is resolveLocation() working as expected?
- #989: TestCaseWriter login cookie variable name
- #1055: Unable to start EVoMaster in public petstore API
- #1069: Error in EvoMaster 3.1.0: Black-Box Testing Initialization Failure with InvocationTargetException and NoSuchMethodError

---
# Version: 3.1.0

### New Features
- new fully-supported output formats: Python and JavaScript 
- support for JDK 21
- body payloads for GET, HEAD and DELETE are not valid in OpenAPI. However, they can be valid in some special cases in HTTP (as of RFC 9110). Now, EvoMaster can test those cases as well, but only for DELETE (for GET it is not possible, as current version of HTTP library in EvoMaster is faulty, i.e., it cannot handle such case).
- update of internal parameter settings to improve performance, based on large tuning experiments. 
- --prematureStop option can now be used to stop search prematurely if no improvement has been obtained in the last specified amount of time 

### Changes
- output format for black-box testing now defaults to Python instead of throwing an error
- --outputFolder now defaults to a more meaningful value, i.e., "generated_tests" 

### Bug Fixes
- better dealing with reset of SQL tables where names in queries used quotes, and/or table is not accessible in database.
- fixed issue related to SQL insertion failures preventing all SQL insertions from being part of the generated tests, leading to flaky tests.
- fixed handling of JsonProperty

### Addressed GitHub Issues
- #834: Authentication required when accessing schema
- #957: Generated test cases fail due to 'checkInstrumentation' 
- #962: Fail to parse endpoint due to body.content must not be null when requestBody has $ref
- #988: Is it normal that EvoMaster combined "sucess" and "other" tests into "fault_representatives"?

---
# Version: 3.0.0

### Breaking Changes
- Authentication credential definitions have been overhauled. This breaks current white-box drivers. For example, `AuthenticationDto` is now moved into a `auth` sub-package, i.e., `org.evomaster.client.java.controller.api.dto.auth`. Cookie and token based authentication definitions are now replaced with `LoginEndpointDto`. See new [auth documentation](docs/auth.md). 
- SQL related configuration DTOs have been moved into new `org.evomaster.client.java.sql` package from old naming `org.evomaster.client.java.controller.db`. 

### New Features
- Handling configuration files to specify _options parameters_ and _authentication credentials_ (especially needed for black-box testing). An empty `em.yaml` file is now created automatically if missing, with documentation as comments. Location can be changed with `--configPath` option. 
- Possibility to filter endpoints to fuzz based on their OpenAPI tags (using `--endpointTagFilter`).
- Bypassing HTTPS checks. Now it is possible to fuzz APIs using HTTPS where their certificates are expired or invalid. 

### White-Box Improvements
- Handling of `Base64.getDecoder()`
- Handling of `@Email` javax constraints

### Bug Fixes

- fixed wrong number of covered endpoints in console logs
- fixed issues in reading OpenAPI schemas from file-system on Windows
- fixed issues related to endpoint definitions ending with a `/`, e.g., `/foo` and `/foo/` are technically 2 different endpoints.

### Addressed GitHub Issues
- #701: EvoMaster process terminated abruptly when using BlackBox and a local file for schema
- #759: Cannot find EvoMaster_Test.java file in the output folder
- #820: OpenApi spec default value is ignored
- #862: Errors while saving tests to src/em
- #873: Bug in GraphQL Blackbox mode
- #906: WTSAlgorithm Crossover Bug
- #914: duplicate of #701

---
# Version 2.0.0 and Earlier

Unfortunately, we only started in 2024 to write release notes.
There is no release notes for older versions of _EvoMaster_.

