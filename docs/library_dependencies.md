# Library Dependencies

When *EvoMaster* generates test cases, those test cases can require 
to import some libraries. 
This depends on the target programming language, and on the kind of tests
that are generated. 

When doing white-box testing, you are  required to import the
*EvoMaster-Client* library (as discussed in the documentation for writing 
[white-box drivers](write_driver.md)).
However, such library does **NOT** import the other needed libraries transitively.
This is done on purpose, as to avoid nasty version mismatching if those
libraries are already used in the application you want to test.
You need to add them manually in your build scripts. 

One possible negative side-effect of this approach is that the generated
tests might not be compatible with the version of the library you
are using.
This should be rare, but it might happen if we were using a deprecated API, and a new version
of the library did remove it. 
As a rule of thumb, we try to update *EvoMaster* to the most recent
versions of those libraries.
If you see such problems, please report them on the issue page.


## JVM (e.g., Java and Kotlin)    

For black-box testing,
to simplify the use of *EvoMaster*, since version *1.6.1-SNAPSHOT* we have added a new module including all the needed third-party dependencies. 
Those can be imported with:

```
<dependency>
     <groupId>org.evomaster</groupId>
     <artifactId>evomaster-client-java-dependencies</artifactId>
     <version>LATEST</version>
     <type>pom</type>
     <scope>test</scope>
</dependency>
```
The placeholder `LATEST` needs to be replaced with an actual version number.
The alternative is to import each needed library manually, in case of issues with version mismatches.


When generating test cases for REST/GraphQL APIs written in Java/Kotlin, for example 
*EvoMaster* relies on the [RestAssured](https://github.com/rest-assured/rest-assured)
library.

In Maven, this can be imported with:
```
<dependency>
     <groupId>io.rest-assured</groupId>
     <artifactId>rest-assured</artifactId>
     <version>4.3.0</version>
     <scope>test</scope>
</dependency>
```
Recall to check out for recent versions of this library.

A word of advice if you are using Spring: Spring imports transitively
several libraries, including old versions of JUnit and RestAssured
as well. This can mess up the use of RestAssured if you are using 
a different major version compared to the one imported by Spring.
However, it is not too hard to [fix](https://stackoverflow.com/questions/44993615/java-lang-noclassdeffounderror-io-restassured-mapper-factory-gsonobjectmapperfa). 


For white-box testing, you need as well to import the client controller library:

```
<dependency>
   <groupId>org.evomaster</groupId>
   <artifactId>evomaster-client-java-controller</artifactId>
   <scope>test</scope>
   <version>LATEST</version>
</dependency>
```

as explained in the documentation for writing [white-box drivers](write_driver.md). 


## NodeJS (e.g., JavaScript)

Generated tests for block-box testing requires different libraries, such as Jest and SuperAgent.
In your `package.json` file, you can import them with something like:

```
  "devDependencies": {
    "jest": "29.7.0",
    "superagent": "9.0.2",
    "supertest": "7.0.0",
    "urijs": "1.19.6"
  }
```

Note, as this documentation might not be updated as often as the code, make sure to use latest versions, and check if required any further library (if so, please open an issue on GitHub, so we can fix the documentation if we forget to update it properly). 

Note that we do not have any library release on NPM.
Utility functions are generated and saved directly with the tests (e.g., the `EMTestUtils.js` file).


## Python

Different libraries are used, such as, at the time of writing:
```
rfc3986==2.0.0
urllib3==1.26.5
requests==2.25.1
timeout-decorator==0.5.0
```
These can then be saved in a `requirements.txt` file, and installed with:

```
python -m pip install --upgrade pip --user
pip install -r ./requirements.txt --user
```
