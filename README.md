# EvoMaster: A Tool For Automatically Generating System-Level Test Cases


![](docs/img/carl-cerstrand-136810_compressed.jpg  "Photo by Carl Cerstrand on Unsplash")

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.evomaster/evomaster-client-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.evomaster/evomaster-client-java)
[![javadoc](https://javadoc.io/badge2/org.evomaster/evomaster-client-java-controller/javadoc.svg)](https://javadoc.io/doc/org.evomaster/evomaster-client-java-controller)
![CI](https://github.com/EMResearch/EvoMaster/workflows/CI/badge.svg)
[![CircleCI](https://circleci.com/gh/EMResearch/EvoMaster.svg?style=svg)](https://circleci.com/gh/EMResearch/EvoMaster)
[![codecov](https://codecov.io/gh/EMResearch/EvoMaster/branch/master/graph/badge.svg)](https://codecov.io/gh/EMResearch/EvoMaster)
<!---
Needs auth :(
[[JaCoCo]](https://circleci.com/api/v1.1/project/github/arcuri82/evomaster/latest/artifacts/0/home/circleci/evomaster-build/report/target/site/jacoco-aggregate/index.html)
-->



### Summary 

_EvoMaster_ ([www.evomaster.org](http://evomaster.org)) is the first (2016) open-source tool 
that automatically *generates* system-level test cases
for web/enterprise applications.
This is related to [Fuzzing](https://en.wikipedia.org/wiki/Fuzzing).
Not only _EvoMaster_ can generate inputs that find program crashes, but also it generates small effective test suites that can be used for _regression testing_.

_EvoMaster_ is an AI driven tool.
In particular, internally it uses an [Evolutionary Algorithm](https://en.wikipedia.org/wiki/Evolutionary_algorithm) 
and [Dynamic Program Analysis](https://en.wikipedia.org/wiki/Dynamic_program_analysis)  to be 
able to generate effective test cases.
The approach is to *evolve* test cases from an initial population of 
random ones, trying to maximize measures like code coverage and fault detection.
_EvoMaster_ uses several kinds of AI heuristics to improve performance even further, 
building on decades of research in the field of [Search-Based Software Testing](https://en.wikipedia.org/wiki/Search-based_software_engineering).


__Key features__:

* At the moment, _EvoMaster_ targets RESTful APIs compiled to 
  JVM __8__ and __11__ bytecode. Might work on other JVM versions, but we provide __NO__ support for it.

* We provide installers for the main operating systems: Windows (`.msi`), 
  OSX (`.dmg`) and Linux (`.deb`). We also provide an uber-fat JAR file.

* The REST APIs must provide a schema in [OpenAPI/Swagger](https://swagger.io) 
  format (either _v2_ or _v3_).

* The tool generates _JUnit_ (version 4 or 5) tests, written in either Java or Kotlin.

* _Fault detection_: _EvoMaster_ can generate tests cases that reveal faults/bugs in the tested applications.
  Different heuristics are employed, like checking for 500 status codes and mismatches from the API schemas. 

* Self-contained tests: the generated tests do start/stop the application, binding to an ephemeral port.
  This means that the generated tests can be used for _regression testing_ (e.g., added to the Git repository
  of the application, and run with any build tool such as Maven and Gradle). 

* Advanced _whitebox_ heuristics: _EvoMaster_ analyses the bytecode of the tested applications, and uses
  several heuristics such as _testability transformations_ and _taint analysis_ to be able to generate 
  more effective test cases. 

* SQL handling: _EvoMaster_ can intercept and analyse all communications done with SQL databases, and use
  such information to generate higher code coverage test cases. Furthermore, it can generate data directly
  into the databases, and have such initialization automatically added in the generated tests. 
  At the moment, _EvoMaster_ supports _H2_ and _Postgres_ databases.  

* _Blackbox_ testing mode: can run on any API (regardless of its programming language), 
  as long as an OpenAPI schema is provided. However, results will be worse than whitebox testing (e.g., due
  to lack of bytecode analysis).

* _Authentication_: for white-box testing we support auth based on authentication headers and cookies. 
   However, at the moment we do not support OAuth, nor authentication in black-box testing.

__Known limitations__:

* To be used for _whitebox_ testing, users need to write a [driver manually](docs/write_driver.md).
  We recommend to try _blackbox_ mode first (should just need a few minutes to get it up and running) to get
  an idea of what _EvoMaster_ can do for you.  

* Execution time: to get good results, you might need to run the search for several hours. 
  We recommend to first try the search for 10 minutes, just to get an idea of what type of tests can be generated.
  But, then, you should run _EvoMaster_ for something like between 1 and 24 hours (the longer the better, but
  it is unlikely to get better results after 24 hours).
  
* External services (e.g., other RESTful APIs): currently there is no support for them (e.g., to automatically mock them).
  It is work in progress.
  
* NoSQL databases (e.g., MongoDB): currently no support. It is work in progress. 

* Failing tests: the tests generated by _EvoMaster_ should all pass, and not fail, even when they detect a fault.
  In those cases, comments/test-names would point out that a test is revealing a possible fault, while still passing.
  However, in some cases the generated tests might fail. This is due to the so called _flaky_ tests, e.g., when
  a test has assertions based on the time clock (e.g., dates and timestamps). 
  There is ongoing effort to address this problem, but it is still not fully solved.   

<!--### Videos---> 
<!-- 
<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div> 
-->


### Videos

![](docs/img/video-player-flaticon.png)

* A [short video](https://youtu.be/3mYxjgnhLEo) (5 minutes)
shows the use of _EvoMaster_ on one of the 
case studies in [EMB](https://github.com/EMResearch/EMB). 

* This [13-minute video](https://youtu.be/ORxZoYw7LnM)
  shows how to write a white-box driver for EvoMaster, for the
  [rest-api-example](https://github.com/EMResearch/rest-api-example). 

* How to [Download and Install EvoMaster on Windows 10](https://youtu.be/uh_XzGxws9o), using its _.msi_ installer. 
 
<!---
### Hiring

Each year we usually have funding for _postdoc_ and _PhD student_ positions to work on this project (in Oslo, Norway).
For more details on current vacancies, see our group page at [AISE Lab](https://emresearch.github.io/).
--->



### Documentation

* [Example of generated tests](docs/example.md)
* [Download and Install EvoMaster](docs/download.md)
* [Build EvoMaster from source](docs/build.md)
* [Console options](docs/options.md)
* [OpenApi/Swagger Schema](docs/openapi.md)
* [Using EvoMaster for Black-Box Testing (easier to setup, but worse results)](docs/blackbox.md)
* [Using EvoMaster for White-Box Testing (harder to setup, but better results)](docs/whitebox.md)
    * [Write an EvoMaster Driver for White-Box Testing](docs/write_driver.md)
* [Console output](docs/console_output.md)  
* [Library dependencies for the generated tests](docs/library_dependencies.md)
* [How to contribute](docs/contribute.md)
    * [Technical notes for developers contributing to EvoMaster](docs/for_developers.md)
* Troubleshooting
    * [Windows and networking](docs/troubleshooting/windows.md)
* More Info
    * [Academic papers related to EvoMaster](docs/publications.md)
    * [Slides of presentations/seminars](docs/presentations.md)






### Funding

_EvoMaster_ has been funded by: 
* 2020-2025: a 2 million Euro grant by the European Research Council (ERC),
as part of the *ERC Consolidator* project 
<i>Using Evolutionary Algorithms to Understand and Secure Web/Enterprise Systems</i>.
*  2018-2021: a 7.8 million Norwegian Kroner grant  by the Research Council of Norway (RCN), 
as part of the Frinatek project <i>Evolutionary Enterprise Testing</i>.  


<img src="https://github.com/EMResearch/EvoMaster/blob/master/docs/img/LOGO_ERC-FLAG_EU_.jpg?raw=true" width="200" >


This project has received funding from the European Research Council (ERC) under the European Union’s Horizon 2020 research and innovation programme (grant agreement No 864972).


### License
_EvoMaster_'s source code is released under the LGPL (v3) license.
For a list of the used third-party libraries, you can directly see the root [pom.xml](./pom.xml) file.
For a list of code directly imported (and then possibly modified/updated) from 
other open-source projects, see [here](./docs/reused_code.md).


### ![](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of 
<a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>
and 
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.


