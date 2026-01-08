# Build EvoMaster


To compile the project, you need Maven and the JDK (version __8__ or later).

Use the Maven command:

`mvn  clean install -DskipTests`

This should create an `evomaster.jar` executable under the `core/target` folder,
and install the driver library in your local `~/.m2` repository.

You need to build the project at least once with Maven before trying to use it from an IDE.
The reason is that some source code files are generated with some Maven plugins (e.g., from grammar files).
If those plugins are not run at least once, the project will not compile. 


Note: if you get an error from the *shade-plugin*, then make sure to use
`clean` in your Maven commands.
Furthermore, if you decide to do not skip the tests, then you will need to have
_Docker_ installed and running on your machine.


Note: we have also started initial support for other languages, e.g., JavaScript and C#.
To build everything, you can use the script `buildAll.sh`.
However, besides the JDK, you will need other tools as well installed on your machine (e.g., NodeJS and DotNet Core).
