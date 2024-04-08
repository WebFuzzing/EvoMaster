# EvoMaster Driver

To generate tests for [white-box testing](whitebox.md), you need an _EvoMaster Driver_ up and running before
executing `evomaster.jar`.
These drivers have to be built manually for each system under test (SUT).
See the [EMB repository](https://github.com/EMResearch/EMB) for a set of existing SUTs with drivers.

To build a client driver in Java (or any JVM language), you need to import the
_EvoMaster_ Java client library. For example, in Maven:

```
<dependency>
   <groupId>org.evomaster</groupId>
   <artifactId>evomaster-client-java-controller</artifactId>
   <scope>test</scope>
   <version>LATEST</version>
</dependency>
```

In Gradle, it would be:

`testCompile('org.evomaster:evomaster-client-java-controller:LATEST')`.

The placeholder `LATEST` needs to be replaced with an actual version number (e.g.,
`1.0.0` or `1.0.0-SNAPSHOT`).
For the latest version, check [Maven Central Repository](https://mvnrepository.com/artifact/org.evomaster/evomaster-client-java-controller).
The latest version number should also appear at the top of the main readme page.
If you are compiling directly from the _EvoMaster_ source code, make sure to use `mvn install` to
install the snapshot version `x.y.z-SNAPSHOT` of the Java client into your local Maven repository
(e.g., under *~/.m2*).
For the actual `x.y.z-SNAPSHOT` version number, you need to look at the root `pom.xml` file in the project.
If you are using Gradle, you can for example check on this [SO question](https://stackoverflow.com/questions/6122252/gradle-alternate-to-mvn-install)
to see how to do something equivalent to `mvn install`.

Note: the core application `evomaster.jar` is independent of the driver library, and it contains none of
the driver's classes.

Note: you might also need to import some [other libraries](library_dependencies.md)
(e.g., *RestAssured* when generating tests for REST APIs runninng on the JVM).

Once the client library is imported, you need to create a class that extends either
`org.evomaster.client.java.controller.EmbeddedSutController`
or
`org.evomaster.client.java.controller.ExternalSutController`.
Both these classes extend `SutController`.
The difference is on whether the SUT is started in the same JVM of the _EvoMaster_
driver (*embedded*), or in a separated JVM (*external*).

The easiest approach (which we recommend) is to use the *embedded* version, especially when dealing with
frameworks like Spring and DropWizard.
However, when the presence of the _EvoMaster_ client library gives side-effects (although
its third-party libraries are shaded, side-effects might still happen),
or when it is not possible (or too complicated) to start the SUT directly (e.g., JEE),
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

Note: when implementing a new class, most IDEs (e.g., IntelliJ) have the function
to automatically generate empty
stubs for all the abstract methods in its super-classes.
Also, all the concrete (i.e., non-abstract) methods in  `EmbeddedSutController`
and `ExternalSutController` are marked as `final`, to prevent overriding them by mistake
and so breaking the driver's internal functionalities.

Each of the abstract methods you need to implement does provide Javadocs.
How to read those Javadocs depend on your IDE settings (e.g., hovering the mouse over a method declaration).
You can also browse them online [here](https://javadoc.io/doc/org.evomaster/evomaster-client-java-controller).



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
Note that it is also possible to run the driver from command-line, like any other Java program with a `main` function.
However, in such case, you will need to package an uber jar file (e.g., using plugins like `maven-shade-plugin` and `maven-assembly-plugin`).


__WARNING__: Java 9 broke backward compatibility.
And each new JDK version seems breaking more stuff :-(.
To deal with recent versions of the JDK, see [here for details](./jdks.md).





## TCP Ports

When writing an _EvoMaster_ driver, there are 2 TCP ports that you need to consider:

* the port of the driver itself, whose default value is 40100. This can be changed when instantiating
  a `SutController`. However, the _EvoMaster_ core process would need to be informed of this different
  port value (e.g., by using the `--sutControllerPort` option).

* the port of the SUT. In general, you will want to set up an ephemeral port (i.e., a free, un-used one)
  for this (e.g., by using the value 0, and then read back in the driver which port was actually
  assigned to the server).

## Starting The Application

How to start/reset/stop the SUT depends on the chosen framework used to implement the SUT.
To implement an _EvoMaster Driver_ class, you need check the JavaDocs of the extended super class,
e.g., `EmbeddedSutController`, and the existing examples in
[EMB](https://github.com/EMResearch/EMB).


As _SpringBoot_ is nowadays the most common way to implement enterprise systems on the JVM, here we provide
some discussions / walk-through on how to write a driver for it that extends `EmbeddedSutController`,
using as reference the [driver for the *features-service* SUT in EMB](https://github.com/EMResearch/EMB/blob/master/jdk_8_maven/em/embedded/rest/features-service/src/main/java/em/embedded/org/javiermf/features/EmbeddedEvoMasterController.java).


To programmatically start a _SpringBoot_ application (needed to implement `startSut()`), you can use `SpringApplication.run`,
and save the resulting `ConfigurableApplicationContext` in variable (e.g., `ctx`).
This will be useful when needing to override the methods `isSutRunning()` and `stopSut()`,
as you can just implement them with  `ctx.isRunning()` and `ctx.stop()`.


When starting the SUT, there is one important configuration that you want to change: the binding port, as you want to use 0 for ephemeral ports (to avoid port conflicts).

Note: since version _1.3.0_, there is no longer the need to configure `P6Spy`.

For a SUT like `features-service`, this can be done with:

```
ctx = SpringApplication.run(Application.class, new String[]{
                "--server.port=0"                
      });
```      

The actual chosen port can then be extracted with:

```
protected int getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
}
``` 

Finally, the `startSut()` method must return the URL of where the SUT is listening on.
When running tests locally, this is as simple as returning `"http://localhost:" + getSutPort()`.


Note that you need to make sure you can run your application programmatically, regardless of EvoMaster. A simple way is to check if the following works:

```
public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
}
```

The issue could arise when using `spring-boot-maven-plugin` to start the application, and there are some classpath problems in your application.

## SQL Databases

If the application is using a SQL database, you must configure
`getDbSpecifications()`, instead of leaving its returned value as `null`.
For example:

```
@Override
public List<DbSpecification> getDbSpecifications() {
   return Arrays.asList(new DbSpecification(DatabaseType.H2, sqlConnection));
}
```
Here, you can specify how to connect to 1 or more SQL databases.
You need to specify the type of database, and a `Connection` object for it.

You can extract a connection object in the `startSut()` method (and save it in a variable),
by simply using:

```
connection =  java.sql.DriverManager.getConnection(url,user,password);
```

Note that, since version `1.5.0`, the methods `getConnection()` and  `getDatabaseDriverName()` have been removed.


## Independent Tests

Test cases must be __independent__ from each other.
Otherwise, you could get different results based on their execution order.
To enforce such independence, you must clean the state of the SUT in the `resetStateOfSUT()` method.
In theory, RESTful APIs should be _stateless_.
If indeed stateless, resetting the state would be just a matter of cleaning the database (if any).
For this purpose, we provide the `DbCleaner` utility class
(used to delete data without recreating the database schema).
There might be some tables that you might not want to clean, like for example if you are using
_FlyWay_ to handle schema migrations.
These tables can be skipped, for example:

```
public void resetStateOfSUT() {
   DbCleaner.clearDatabase_H2(connection, Arrays.asList("schema_version"));
}
```

where the content of the table `schema_version` is left untouched.

If your application uses some caches, those might be reset at each test execution.
However, an easier approach could be to just start the SUT without the caches, for example using
the option `--spring.cache.type=NONE`.

__IMPORTANT__: since version `1.5.0` we automatically infer which SQL tables and databases to clean after each test execution.
Setting up `resetStateOfSUT()` for SQL databases is no longer necessary.
However, if for any reason this behavior needs to be changed (e.g., due to issues), it can be deactivated with `DbSpecification.withDisabledSmartClean()`.


Whenever possible, it would be best to use an embedded database such as _H2_.
However, if you need to rely on a specific database such as _Postgres_, we recommend starting
it with _Docker_.  
In Java, this can be done with libraries such as [TestContainers](https://github.com/testcontainers/testcontainers-java/) (which you will need to import in Maven/Gradle).
In your driver, you can then have code like:

```
private static final GenericContainer postgres = new GenericContainer("postgres:9")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_HOST_AUTH_METHOD","trust")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
```

Then, the database can be started in `startSut()` with `postgres.start()`,
and stopped in `stopSut()` with `postgres.stop()`.
Then, the URL to connect to the database can be something like:

```
String host = postgres.getContainerIpAddress();
int port = postgres.getMappedPort(5432);
String url = "jdbc:postgresql://"+host+":"+port+"/postgres"
```

You can then tell Spring to use such URL with the parameter `--spring.datasource.url`.

Note: the `withTmpFs` configuration is very important, and it is database dependent.
A database running in _Docker_ will still write on your hard-drive, which is an unnecessary,
time-consuming overhead.
The idea then is to mount the folder, in which the database writes, directly in RAM.

For an example, you can look at the E2E tests in EvoMaster, like the class [com.foo.spring.rest.postgres.SpringRestPostgresController](https://github.com/EMResearch/EvoMaster/blob/master/e2e-tests/spring-rest-postgres/src/test/kotlin/com/foo/spring/rest/postgres/SpringRestPostgresController.kt).


## MongoDB Database

Although at the current moment _EvoMaster_ does not analyze how MongoDB databases are accessed at runtime, it is important still to reset their state (to avoid dependencies among generated tests).
Compared to SQL databases, MongoDB is easier to reset.
Can use directly the APIs of MongoDB.
For example, given a reference to `com.mongodb.MongoClient`, you can have:
```
public void resetStateOfSUT() {
    mongoClient.getDatabase("foo").drop();
}        
```

As for any external service, we recommend to start them with Docker.
For example:

```
private static final GenericContainer mongodb =  
         new GenericContainer("mongo:6.0")
                .withTmpFs(Collections.singletonMap("/data/db", "rw"))
                .withExposedPorts(27017);
```

In Spring, the connection URL can then be set with:
```
--spring.data.mongodb.uri=mongodb://"+mongodb.getContainerIpAddress()+":"+mongodb.getMappedPort(27017)+"/foo"
```




## Code Coverage

When _EvoMaster_ evolves test cases, it tries to maximize code coverage in the SUT.
But there is no much point in trying to maximize code coverage of the third-party libraries,
like Spring, Hibernate, Tomcat, etc.
Therefore, in the `getPackagePrefixesToCover()` you need to specify the common package prefix for your
business logic.
In the case of the `features-service` SUT, this was `org.javiermf.features`.


## REST OpenApi/Swagger Schema

To test a RESTful API, in the the `getProblemInfo()`, you need to return an instance of the
`RestProblem` class.
Here, you need to specify where the _OpenApi_ schema is found, and whether any endpoint should be skipped,
i.e., not generating test cases for.
This latter option is useful for example to skip the _SpringBoot Actuator_ endpoints (if any is present).  
If your RESTful API does not have an _OpenApi/Swagger_ schema, this can be automatically added by using
libraries such as [SpringDoc](https://github.com/springdoc/springdoc-openapi).


## GRAPHQL Schema

To test a GraphQL API, in the the `getProblemInfo()`, you need to return an instance of the
`GraphQlProblem` class.
Here, you need to specify the endpoint of where the GraphQL API can be accessed. Default value is `/graphql`.
Note: must be able to do an _introspective query_ on such API to fetch its schema. If this is disabled for security reasons, _EvoMaster_ will fail.

## RPC APIs

To test RPC APIs, in the the `getProblemInfo()`, you need to return an instance of the
`RPCProblem` class.
Fuzzing RPC APIs requires a driver, and importing a JVM version of the API client library used to communicate with it.
Note that such API client library can be generated based on the schema definition file, e.g., `.proto` and `.thrift`, but this is tool and framework dependent.
Preparing such client library for the JVM needs to be setup by the user.
Currently, there is no direct support in _EvoMaster_ for file formats such as `.proto` and `.thrift`.
Schema definitions are derived from the Java/Kotlin interfaces of the API client library.
On the one hand, this means it is possible to fuzz any type of RPC API (and not just gRPC and Thrift), as long as there is a JVM client library.
On the other hand, _black-box_ testing would require to write a driver.

Here, in `RPCProblem` there are 3 main things you need to specify:
- the interface for the client library, defining the operations available in the API.
- implementation for the interface, initializing a client stub to make calls to the API for each specified interface.
- specify the RPC type (e.g., gRPC or Thrift).

## Security

The SUT might require authenticated requests (e.g., when _Spring Security_ is used).
How to do it must be specified in the `getInfoForAuthentication()`.
This is clarified in details in the [authentication documentation](auth.md).
