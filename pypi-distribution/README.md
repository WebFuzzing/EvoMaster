# EvoMaster

[EvoMaster](https://github.com/WebFuzzing/EvoMaster) is one of the oldest (2016) Web API fuzzer that is still actively maintained (see its [GitHub repository](https://github.com/WebFuzzing/EvoMaster)).

This is not a re-write of _EvoMaster_ from Kotlin into Python, but rather a wrapper distributed via PyPi. 
As a CLI application, PyPi gives to _EvoMaster_ a better user experience compared to the cumbersome ways of distributing JDK programs. 

Via PyPi, _EvoMaster_ can be installed using:

`pip install evomaster`

or using `pip3` if on a Mac.
Once the wrapper is installed, it can be executed on the command line with:

`evomaster <args>`

For a list of available args options, see documentation on its [GitHub repository](https://github.com/WebFuzzing/EvoMaster).

One the first run, `evomaster` will do:
1) download the latest `evomaster.jar` release from the [GitHub EvoMaster' release page](https://github.com/WebFuzzing/EvoMaster/releases).
2) download a suitable JDK for the host OS and architecture (e.g., Amazon Corretto).

These files are downloaded under `~/.evomaster` folder.
Those files take a few hundreds MBs in space. 

WARNING: executing `pip uninstall evomaster` unfortunately does not delete the content of `~/.evomaster`. You will need to delete it manually. 

When `evomaster` Python script is executed, it simply calls something like:

`~/.evomaster/jdk/bin/java -jar ~/.evomaster/evomaster/<version>/evomaster.jar <args>`




