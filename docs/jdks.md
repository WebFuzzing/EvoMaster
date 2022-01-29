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
To use JDK __17__, besides passing `-Djdk.attach.allowAttachSelf=true`, you will also need:

`--add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED`

isn't it lovely? 


## evomaster.jar

The recommended way to use _EvoMaster_ is via its installer, e.g.,
_.msi_ for Windows.
However, you can still use its _jar_ file directly, if you really want.
Unfortunately, though, if you use JDK _17_ or above, you have to deal
with `--add-opens` shenanigans. 

To see how to set it up, look at the usage of `--add-opens` in  [makeExecutable.sh](../makeExecutable.sh) script. 


