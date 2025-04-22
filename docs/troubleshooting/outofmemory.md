# java.lang.OutOfMemoryError

_EvoMaster_ core process runs on the JVM. 
Depending on the tested application, and for how long you run it, it might end up that _EvoMaster_ runs out of memory, throwing for example a `java.lang.OutOfMemoryError` exception.

This can happen even if you have enough memory left!
By default, a Java application can use up to 1/4 of your total memory. 
For example, on a 16GB RAM laptop, it would not use more than 4GB of RAM. 
This is fine in most cases. 
But, if you get a `java.lang.OutOfMemoryError`, you might need to increase such limit. 
This can be achieved with the `-Xmx` JVM option. 
For example:

`java -Xmx8g -jar core/target/evomaster.jar ...`

If you have installed _EvoMaster_ via one of its OS installers, you might need to check its configuration files where the executable file is located. 
A possibility could be to modify the `evomaster.cfg` file, in particular its `java-options` entry (note: we have not tested out if this works on all OS). 