# EvoMaster Driver



TODO update


    
Note, to generate tests for white-box testing, you need an _EvoMaster Driver_ up and running before 
executing `evomaster.jar`.
These drivers have to be built manually for each system under test (SUT).
See [EMB](https://github.com/EMResearch/EMB) for a set of existing SUTs with drivers.

To build a client driver in Java (or any JVM language), you need to import the
_EvoMaster_ Java client library. For example, in Maven:

```
<dependency>
   <groupId>org.evomaster</groupId>
   <artifactId>evomaster-client-java-controller</artifactId>
   <version>LATEST</version>
</dependency>
```

For the latest version, check [Maven Central Repository](https://mvnrepository.com/artifact/org.evomaster/evomaster-client-java-controller).
The latest version number should also appear at the top of this page.
If you are compiling directly from the _EvoMaster_ source code, make sure to use `mvn install` to 
install the snapshot version of the Java client into your local Maven repository 
(e.g., under *~/.m2*). 

Once the client library is imported, you need to create a class that extends either
`org.evomaster.client.java.controller.EmbeddedSutController`
 or
 `org.evomaster.client.java.controller.ExternalSutController`.
Both these classes extend `SutController`.
The difference is on whether the SUT is started in the same JVM of the _EvoMaster_
driver (*embedded*), or in a separated JVM (*external*).
 
The easiest approach is to use the *embedded* version, especially when dealing with
frameworks like Spring and DropWizard. 
However, when the presence of the _EvoMaster_ client library gives side-effects (although 
its third-party libraries are shaded, side-effects might still happen),
or when it is not possible (or too complicate) to start the SUT directly (e.g., JEE),
it is better to use the *external* version.
The requirement is that there should be a single, self-executable uber/fat jar for the SUT 
(e.g., Wildfly Swarm).
It can be possible to handle WAR files (e.g., by using Payara), 
but currently we have not tried it out yet.

Once a class is written that extends either `EmbeddedSutController` or
`ExternalSutController`, there are a few abstract methods that need to
be implemented.
For example, those methods specify how to start the SUT, how it should be stopped,
and how to reset its state.
The _EvoMaster_ Java client library also provides further utility classes to help
writing those controllers/drivers.
For example, `org.evomaster.client.java.controller.db.DbCleaner` helps in resetting
the state of a database (if any is used by the SUT).

Until better documentation and tutorials are provided,
to implement a `SutController` class check the JavaDocs of the extended super class,
and the existing examples in 
[EMB](https://github.com/EMResearch/EMB).


Once a class `X` that is a descendant of `SutController` is written, you need
to be able to start the _EvoMaster_ driver, by using the 
`org.evomaster.client.java.controller.InstrumentedSutStarter`
class. 
For example, in the source code of the class `X`, you could add:
 
```
public static void main(String[] args){

   SutController controller = new X();
   InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

   starter.start();
}
```

At this point, once this driver is started (e.g., by right-clicking on it in
an IDE to run it as a Java process),
then you can use `evomaster.jar` to finally generate test cases.

