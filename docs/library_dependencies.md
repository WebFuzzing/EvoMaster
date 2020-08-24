# Library Dependencies

When *EvoMaster* generates test cases, those test cases can require 
to import some libraries. 
This depends on the target programming language, and on the kind of tests
that are generated. 

When doing white-box testing, you are also required to import the
*EvoMaster-Client* library (as discussed in the documentation for writing 
[white-box drivers](write_driver.md)).
However, such library does **NOT** import the others transitively.
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


## REST APIs

### JVM (e.g., Java and Kotlin)    

When generating test cases for REST APIs written in Java/Kotlin,
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


