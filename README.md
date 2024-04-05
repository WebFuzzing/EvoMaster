# EvoMaster: A Tool For Automatically Generating System-Level Test Cases


![](docs/img/carl-cerstrand-136810_compressed.jpg  "Photo by Carl Cerstrand on Unsplash")

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.evomaster/evomaster-client-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.evomaster/evomaster-client-java)
[![javadoc](https://javadoc.io/badge2/org.evomaster/evomaster-client-java-controller/javadoc.svg)](https://javadoc.io/doc/org.evomaster/evomaster-client-java-controller)
![CI](https://github.com/EMResearch/EvoMaster/workflows/CI/badge.svg)
[![codecov](https://codecov.io/gh/EMResearch/EvoMaster/branch/master/graph/badge.svg)](https://codecov.io/gh/EMResearch/EvoMaster)
[![DOI](https://zenodo.org/badge/92385933.svg)](https://zenodo.org/badge/latestdoi/92385933)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL_v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)
[![Github All Releases](https://img.shields.io/github/downloads/emresearch/evomaster/total.svg)](https://github.com/EMResearch/EvoMaster/releases)


### Summary

_EvoMaster_ ([www.evomaster.org](http://evomaster.org)) is the first (2016) open-source AI-driven tool
that automatically *generates* system-level test cases
for web/enterprise applications.
This is related to [Fuzzing](https://en.wikipedia.org/wiki/Fuzzing).
Not only _EvoMaster_ can generate inputs that find program crashes, but also it generates small effective test suites (e.g., in JUnit format) that can be used for _regression testing_.

_EvoMaster_ is an AI driven tool.
In particular, internally it uses an [Evolutionary Algorithm](https://en.wikipedia.org/wiki/Evolutionary_algorithm)
and [Dynamic Program Analysis](https://en.wikipedia.org/wiki/Dynamic_program_analysis)  to be
able to generate effective test cases.
The approach is to *evolve* test cases from an initial population of
random ones, trying to maximize measures like code coverage and fault detection.
_EvoMaster_ uses several kinds of AI heuristics to improve performance even further,
building on decades of research in the field of [Search-Based Software Testing](https://en.wikipedia.org/wiki/Search-based_software_engineering).


__Key features__:

* _Web APIs_: At the moment, _EvoMaster_ can generate test cases for __REST__, __GraphQL__ and __RPC__ (e.g., __gRPC__ and __Thrift__) APIs.

* _Blackbox_ testing mode: can run on any API (regardless of its programming language, e.g., Python and Go).
  However, results for blackbox testing will be worse than whitebox testing (e.g., due to lack of code analysis).

* _Whitebox_ testing mode: can be used for APIs compiled to
  JVM (e.g., Java and Kotlin). _EvoMaster_ analyses the bytecode of the tested applications, and uses
  several heuristics such as _testability transformations_ and _taint analysis_ to be able to generate
  more effective test cases. We support JDK __8__ and the major LTS versions after that (currently JDK __17__, where JDK __21__ has not been properly tested yet). Might work on other JVM versions, but we provide __NO__ support for it.
  Note: there was initial support for other languages as well, like for example JavaScript/TypeScript and C#, but they are not in a stable, feature-complete state. The support for those languages has been dropped, at least for the time being. 

* _Installation_: we provide installers for the main operating systems: Windows (`.msi`),
  OSX (`.dmg`) and Linux (`.deb`). We also provide an uber-fat JAR file.
  To download them, see the [Release page](https://github.com/EMResearch/EvoMaster/releases).
  Release notes are present in the file [release_notes.md](https://github.com/EMResearch/EvoMaster/blob/master/release_notes.md).

* _State-of-the-art_: an [independent study (2022)](https://arxiv.org/abs/2204.08348), comparing 10 fuzzers on 20 RESTful APIs, shows that _EvoMaster_ gives the best results.

* _Schema_: REST APIs must provide a schema in [OpenAPI/Swagger](https://swagger.io)
  format (either _v2_ or _v3_).

* _Output_: the tool generates _JUnit_ (version 4 or 5) tests, written in either Java or Kotlin. There is initial support for other formats. For complete list, see the documentation for the CLI parameter [--outputFormat](docs/options.md).

* _Fault detection_: _EvoMaster_ can generate tests cases that reveal faults/bugs in the tested applications.
  Different heuristics are employed, like checking for 500 status codes and mismatches from the API schemas.

* _Self-contained tests_: the generated tests do start/stop the application, binding to an ephemeral port.
  This means that the generated tests can be used for _regression testing_ (e.g., added to the Git repository
  of the application, and run with any build tool such as Maven and Gradle).


* _SQL handling_: _EvoMaster_ can intercept and analyse all communications done with SQL databases, and use
  such information to generate higher code coverage test cases. Furthermore, it can generate data directly
  into the databases, and have such initialization automatically added in the generated tests.
  At the moment, _EvoMaster_ supports _Postgres_, _MySQL_ and _H2_  databases.


* _Authentication_: we support auth based on authentication headers and cookies.

__Known limitations__:

* _Driver_: to be used for _whitebox_ testing, users need to write a [driver manually](docs/write_driver.md).
  We recommend to try _blackbox_ mode first (should just need a few minutes to get it up and running) to get
  an idea of what _EvoMaster_ can do for you.

* _JDK 9+_: whitebox testing requires bytecode manipulation. 
            Each new release of the JDK makes doing this harder and harder. 
            Dealing with JDKs above __8__ is doable, but it requires some settings.
            [See documentation](docs/jdks.md).

* _Execution time_: to get good results, you might need to run the search for several hours.
  We recommend to first try the search for 10 minutes, just to get an idea of what type of tests can be generated.
  But, then, you should run _EvoMaster_ for something like between 1 and 24 hours (the longer the better, but
  it is unlikely to get better results after 24 hours).

* _RPC APIs_: for the moment, we do not directly support RPC schema definitions. Fuzzing RPC APIs requires to write a driver, using the client library of the API to make the calls.

* _External services_: (e.g., other RESTful APIs) currently there is no support for them (e.g., to automatically mock them).
  It is work in progress.

* _NoSQL databases_: (e.g., MongoDB) currently no support. It is work in progress.

* _Failing tests_: the tests generated by _EvoMaster_ should all pass, and not fail, even when they detect a fault.
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

* [Short presentation](https://youtu.be/iQSAlrr-PZo) (5 minutes) about version 2.0.0. 

### Alternatives

In the last few years, several few tools have been proposed in the academic literature and in the open-source community.
You can read more details in this [2023 survey](docs/publications/2023_tosem_survey.pdf) on REST API testing.

Existing open-source tools for REST API fuzzing are for example (in alphabetic order):
[Dredd](https://github.com/apiaryio/dredd),
[Fuzz-lightyear](https://github.com/Yelp/fuzz-lightyear),
[ResTest](https://github.com/isa-group/RESTest),
[RestCT](https://github.com/GIST-NJU/RestCT),
[Restler](https://github.com/microsoft/restler-fuzzer),
[RestTestGen](https://github.com/SeUniVr/RestTestGen),
and
[Schemathesis](https://github.com/schemathesis/schemathesis).

All these tools are _black-box_, i.e., they do not analyze the source-code of the tested APIs to generate more effective test data.
As we are the authors of EvoMaster, we are too biased to compare it properly with those other black-box tools.
However, an [independent study (2022)](https://arxiv.org/abs/2204.08348) shows that EvoMaster is among the best performant.
Furthermore, if your APIs are running on the JVM (e.g., written in Java or Kotlin), then EvoMaster has clearly an advantage, as it supports _white-box_ testing. 


### Hiring

Depending on the year, we might have funding for _postdoc_ and _PhD student_ positions to work on this project (in Oslo, Norway).

Current open positions: none.
<!---
* 2023: PhD student positions. No new calls scheduled for the moment.
* 2023: Postdoc positions. No new calls scheduled for the moment.
--->

For questions on these positions, please contact Prof. Andrea Arcuri.

<!---
For more details on current vacancies, see our group page at [AISE Lab](https://emresearch.github.io/).
--->



### Documentation

If you are trying to use _EvoMaster_, but the instructions in this documentation are not enough to get you started, or they are too unclear, then it means it is a _bug_ in the documentation, which then would need to be clarified and updated. In such cases, please create a new [issue](https://github.com/EMResearch/EvoMaster/issues).

* [Example of generated tests](docs/example.md)
* [Download and Install EvoMaster](docs/download.md)
* [Build EvoMaster from source](docs/build.md)
* [Command-Line Interface (CLI) options](docs/options.md)
* [OpenApi/Swagger Schema](docs/openapi.md)
* [Using EvoMaster for Black-Box Testing (easier to setup, but worse results)](docs/blackbox.md)
* [Using EvoMaster for White-Box Testing (harder to setup, but better results)](docs/whitebox.md)
  * [Write an EvoMaster Driver for White-Box Testing](docs/write_driver.md)
  * [Dealing with JDKs above version 8](docs/jdks.md)
* [Console output](docs/console_output.md)
* [Library dependencies for the generated tests](docs/library_dependencies.md)
* [How to contribute](docs/contribute.md)
  * [Technical notes for developers contributing to EvoMaster](docs/for_developers.md)
* Troubleshooting
  * [Windows and networking](docs/troubleshooting/windows.md)
* More Info
  * [Academic papers related to EvoMaster](docs/publications.md)
  * [Slides of presentations/seminars](docs/presentations.md)
  * [Replicating studies](docs/replicating_studies.md)






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


