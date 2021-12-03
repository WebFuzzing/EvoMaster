# Dealing With JDKs Above Version 8

Java 9 broke backward compatibility.
And each new JDK version seems breaking more stuff :-(.
One painful change was that self-attachment of Java-Agents (needed for bytecode instrumentation)
is now forbidden by default.
When for example starting the driver with a JDK 9+ (e.g., JDK __11__), you need to add the VM option
`-Djdk.attach.allowAttachSelf=true`, otherwise the process will crash.   
For example, in IntelliJ IDEA:

![](img/intellij_jdk11_jvm_options.png)

Note: there is a hacky workaround for this "_feature_"
(i.e., as done in [ByteBuddy](https://github.com/raphw/byte-buddy/issues/295)),
but it is not implemented yet. 
However, there is no much point in implementing such feature, as JDK __17__ broke more stuff as well. 
To use JDK __17__, besides passing `-Djdk.attach.allowAttachSelf=true`, you will also need:

`--add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED`

isn't it lovely? 