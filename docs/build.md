# Build EvoMaster


To compile the project, you need Maven and the JDK (either __8__ or __11__).

Use the Maven command:

`mvn  clean install -DskipTests`

This should create an `evomaster.jar` executable under the `core/target` folder,
and install the driver library in your local `~/.m2` repository.

Note: if you get an error from the *shade-plugin*, then make sure to use
`clean` in your Maven commands.
Furthermore, if you decide to do not skip the tests, then you will need to have
_Docker_ installed and running on your machine.