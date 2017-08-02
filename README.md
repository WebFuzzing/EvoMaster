[![Build Status](https://travis-ci.org/EMResearch/EvoMaster.svg?branch=master)](https://travis-ci.org/EMResearch/EvoMaster)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.evomaster/evomaster-client-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.evomaster/evomaster-client-java)

# EvoMaster: A Tool For Automatically Generating System-Level Test Cases

EvoMaster ([www.evomaster.org](http://evomaster.org)) is a tool prototype that automatically generates system-level test cases.
At the moment, EvoMaster targets RESTful APIs compiled to JVM bytecode (e.g., Java and Kotlin).

This project is in early stage of development. Documentation is still under construction. 

To compile the project, use the Maven command:

`mvn  install -DskipTests`

This should create an `evomaster.jar` executable under the `core/target` folder.

Available options can be queried by using:

`java -jar evomaster.jar --help`

Note: to generate tests, you need an EvoMaster Driver up and running before executing `evomaster.jar`.
These drivers have to be built manually for each system under test (SUT).
See [EMB](https://github.com/EMResearch/EMB) for a set of existing SUTs with drivers.

### License
EvoMaster's source code is released under the LGPL (v3) license.



### ![](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of 
<a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>
and 
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.


