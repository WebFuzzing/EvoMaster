# Download EvoMaster


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


Regarding the _driver_ library, it depends on the language/environment.
For example, the JVM support is available from [Maven Central](https://mvnrepository.com/artifact/org.evomaster). 
If your are building such library from [source code](./build.md), then make sure to
use the Maven `install` option to copy it over your local `~/.m2` repository.
 
