# Download and Install EvoMaster


_EvoMaster_ is composed of two main components:

* *Core*: the main program executable, packaged in a `evomaster.jar` file.
* *Driver*: library used to control and instrument the application under test.
            There is going to be one library per supported language/environment,
            like the JVM.
            Note: the driver is __NOT__ needed for [Black-Box Testing](./blackbox.md). 


The latest release of the `evomaster.jar` executable  can be downloaded from GitHub
 on the [releases page](https://github.com/EMResearch/EvoMaster/releases).
Alternatively, it can be built from [source code](./build.md).

Note: it does not matter where you download the jar file (e.g., your home folder, or the folder
of your project), as long as you can easily access it from a command-line terminal (e.g.,
to be able to execute `java -jar` on it).

Besides an uber-fat jar, since version 1.2.0 we also provide installers for Windows/OSX/Linux.
Note: the installers are built with `jpackage`, that currently does not support 
updating the [PATH environment variable](https://stackoverflow.com/questions/67784565/jpackage-update-path-environment-variable).
This means that, unless you want to type the full absolute path of where _EvoMaster_
gets installed each time you want to use it, you will need to update the PATH environmental variable by hand.
By default, _EvoMaster_ will get installed at:
* On Windows: `C:\Program Files\evomaster\evomaster.exe`
* On OSX: `/Applications/evomaster.app/Contents/MacOS/evomaster`
* On Linux: ` /opt/evomaster/bin/evomaster`


Regarding the _driver_ library, it depends on the language/environment.
For example, the JVM support is available from [Maven Central](https://mvnrepository.com/artifact/org.evomaster). 
If your are building such library from [source code](./build.md), then make sure to
use the Maven `install` option to copy it over your local `~/.m2` repository.
 
