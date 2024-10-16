# Dealing With JDKs Above Version 8

## Driver/Controller Class

Java __9__ broke backward compatibility in an awful way.
And each new JDK version seems breaking even more stuff :-(.
One painful change was that self-attachment of Java-Agents (needed for bytecode instrumentation)
is now forbidden by default.
When for example starting the driver with a JDK __9+__ (e.g., JDK __11__), you need to add the VM option
`-Djdk.attach.allowAttachSelf=true`, otherwise the process will crash.   
For example, in IntelliJ IDEA:

![](img/intellij_jdk11_jvm_options.png)

Note: there is a hacky workaround for this "_feature_"
(i.e., as done in [ByteBuddy](https://github.com/raphw/byte-buddy/issues/295)),
but it is not implemented. 
There is no much point in implementing such feature, as JDK __17__ broke more stuff as well, and we would still need to set some JVM parameters. 
To use JDK __17__, besides passing `-Djdk.attach.allowAttachSelf=true`, you will also need to setup a few `--add-opens` commands.
In particular, you will need:

`-Djdk.attach.allowAttachSelf=true  --add-opens java.base/java.util=ALL-UNNAMED  --add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED`

isn't it lovely? 

An option to solve this issue is to set those options in an _environment variable_ called `JDK_JAVA_OPTIONS` (which was introduced in JDK __9__).
However, this would apply to _all_ JVM programs running on your machine.
So, use it with care.
A further alternative is to set them up globally in your IDE per project, but of course that is IDE dependent.
For example, for IntelliJ there are plugins such as [JVM Default Options](https://plugins.jetbrains.com/plugin/21136-jvm-default-options) that might help. 
But we have not tried them.


Regarding JDK __21__, it looks like thanks to JEP 451 the attachment of agents gives a warning, as it will be deactivated by default in future releases.
When that will happen, there will be yet again another JVM option that will need to be set. 


## evomaster.jar

The recommended way to use _EvoMaster_ is via its installer, e.g.,
_.msi_ for Windows.
However, you can still use its _jar_ file directly, if you need.
You will need a JDK on your machine to run it with `java -jar evomaster.jar`. 
Since version 3.2.0, there is no longer need to setup any `--add-opens` when using a JDK 17+, as those are now directly set in the jar's manifest.



