# NOTES FOR DEVELOPERS


These notes are meant for developers working on _EvoMaster_, and for people making a pull request.
There are several rules of thumb regarding how to write "good code",
but often rules are either too generic and not tailored for a given particular
piece of software (e.g., different kinds of architectures).

The rules of thumb described here in this document are not meant to be either exhaustive nor absolute.
Rigid rules are not substitute for common sense, as they are rather guidelines that can
be ignored in some special cases.
Furthermore, the guidelines need to be _realistic_ and easy to use: there would be no point
to ask for detailed comments on each single method/field and 100% coverage test suites...

These notes also include some explanations and motivations for some of the architectural choices
made in the development of _EvoMaster_.


### Kotlin vs. Java
The core process of _EvoMaster_ is built in _Kotlin_, as we strongly prefer it over _Java_.
However, the client libraries for JDK SUTs (e.g., not just _Java_, but also all other languages that do 
compile to JDK bytecode) are written in _Java_ instead of _Kotlin_.
The main reason is that, being libraries, we do not want to also have to ship the _Kotlin_ runtime 
libraries with them. 


### BRACES { AND SPACES vs. TABS 

Not going to start an holy war here... once made a choice, we just keep it consistent throughout the whole project. 
Regarding opening braces {, the ancient texts state that those shall be on the same line of the code, and not in their own line.
Therefore, the following is right(eous):
```
foo{
}
```
whereas the following blasphemy must be expunged:
```
foo
{
}
```

Note that, at the current moment, we do not use an automated-formatter as part of the build (for several reasons...).

Regarding spaces vs. tabs, I have no idea what is in use. This should be automatically handled by your IDE (and if it doesn't, switch to a better IDE).



### AVOID `System.out` AND `System.err`
_EvoMaster_ uses a logging framework.
For debugging  and logging errors in a class `Foo`, create a logger in the following way.

* for Java: `private static Logger log = LoggerFactory.getLogger(Foo.class);`
* for Kotlin: `companion object { private val log: Logger = LoggerFactory.getLogger(Foo::class.java)}`

It is important to keep the same name `log` to make things consistent among different classes.
If the logging should be part the actual output for the console user, then rather use: 

`LoggingUtil.getInfoLogger()`


### AVOID `String` CONCATENATION IN LOGGERS

Writing something like:

`log.debug("this is not "+ foo + " very " + bar +" efficient")`

is not efficient, as most of the time debug logs are deactivated, and concatenating strings is
expensive. Recall `String` is immutable, and each `+` does create a new `String` object.
The above logging can be rewritten into:

`log.debug("this is not {} very {} efficient", foo, bar)`

Note: not a big deal for _warn_/_error_, as those are/should be rare... but it can become
quite an overhead for _trace_/_debug_/_info_.




### DO NOT USE `System.exit`

Better to throw an exception, as the entry point of _EvoMaster_ does some logging when ends.
Furthermore, `System.exit` becomes problematic when unit testing _EvoMaster_.



### STATIC VARIABLES ARE YOUR ENEMY

Static variables should be either constant or representing transient data (e.g., cache information 
whose presence/missing has only effect on performance, not on functionality).
Having "classes with static state" is usually a poor OO design (an exception to this rule 
is `ExecutionTracer`).
If those are really needed, then you should rather use an _injectable_ singleton service (see next point). 
This is not just to be pedantic, but, really, non-constant static variables make unit testing 
far much harder and lead to code that is more difficult to understand and maintain. 


### `Guice` and `Governator`

To avoid issues with mutable static variables, we use a dependency injection framework.
In particular, we use `Guice`, extended with `Governator` to handle post-construct events.
All injectable services should be singletons, and declared under a package called `*.service` (this
is to make it easy to find out which services are available).

There is no auto-discovery of beans. This is done manually.
The reason is that, depending on configurations, we can have many different context initializations.
For example, the beans used for testing REST APIs would not be needed when testing GraphQL ones.  


### HOW TO WRITE UNIT TEST CASES

Unit tests should be put in the `src/test/java` and `src/test/kotlin` folders, 
following the same package structure as _EvoMaster_ code.
A unit test suite for SUT `org.evomaster.somepackage.Foo` __MUST__ be called `org.evomaster.somepackage.FooTest`.
This is important for several reasons:
- Need to know what class the test case is supposed to unit test by just looking at its name
- Should be easy to identify if a class has a test suite for it
- If in same package, then the test suite can access package/protected fields/methods
- Having `Test` as postfix (instead of a prefix) is useful for when searching for classes by name
- A `Test` postfix is a requirement for _Maven_ to execute the test suite during the build 


### HOW TO WRITE END-TO-END (E2E) TEST CASES

Besides unit tests, it is essential to have E2E ones as well.
Those should be added under the `e2e-tests` module. 
Being _non-deterministic_, we cannot guarantee that _EvoMaster_ can always find a valid solution (e.g., 
create test cases with certain properties).
Furthermore, we cannot run the E2E tests for long time (otherwise the CI builds will take forever).
The idea is to create artificial SUTs that should be _trivial_ to solve when some settings (which we want
to test) are on, and very difficult (if not straight-out infeasible) otherwise.

Note: current version of JUnit 5 is worse than JUnit 4 when dealing with E2E tests.
E.g., there is no handling of _flaky_ tests (in JUnit 4, this was handled by the _Surefire_/_Failsafe_ plugins).
This is the reason why such test executions should be wrapped inside a `handleFlaky` call.   

Also notice that, for JavaScript and C#, E2E tests are different, as run through bash scripts.
This is due to the fact that we have to run 2 separate processes using different technologies (e.g., JVM vs. .Net and NodeJS). 

### AVOID TOO LONG METHODS/CLASSES

Too long methods (e.g., more than 100 lines) should be split, as difficult to understand.
For this task, in _IntelliJ_, you can right-click on a code snippet and choose 
"_Refactor -> Extract -> Function_".
Likewise, should avoid classes with more than 1000 lines.




### WRITE COMMENTS

In the ideal world, each class/method/field would have nice, detailed, appropriate code comments.
But even in such a beautiful world, everything would go to hell at the first code change, as that might
require manually changing most of the code comments.

Cannot really quantify how many comments one should write, but at least it would be good to have:
* brief (1-2 sentences) description of what the class is useful for (just before the class declaration) 
* for fields that are data structures (e.g., collections and arrays) some comments would be useful, as long and detailed 
  variable names are not practical
* for `Map`s, should add a comment stating what is the _key_, and what is the _value_.   

When writing a comment for a class/method/field, use JavaDoc style:
`/**
*/`
In this way, your IDE can show the comments when you hover with the mouse over them.
For C#, besides `/** */`, for single line documentation you can use a triple slash `///`.   
  
  


### IF CANNOT AVOID EXTERNAL SIDE-EFFECTS, DO DOCUMENT IT!!!

If a call on a object has side effects outside the class itself (e.g., writing to disk, add a system hook thread),
then this needs to be documented (see point on how to write comments),
unless it is obvious from the function/class name.  



### PRE AND POST CONDITIONS

* _Pre-conditions_ of `public` methods should throw exceptions explicitly 
  (e.g., `IllegalArgumentException` and `IllegalStateException`).
  Whenever possible, it is worth to write pre-conditions to `public` methods.
* _Pre-conditions_ of `private` methods and _post-conditions_ (both `public` and `private` methods) 
  should use the keyword `assert` in _Java_, and the function `assert()` in _Kotlin_.
  (An exception is when the validation of inputs of a public method is delegated/moved to 
  a `private` method: in this case you could add `throw`.)
  _Post-conditions_ are good, but often are difficult to write.
  Note: a _post-condition_ does not to be complete to be useful (i.e., find bugs). 
  For example, if we have _A && B_, but the writing
  of _B_ is too difficult (or time-consuming), still having just _A_ as _post-condition_ can help  

Note: currently _Kotlin_ does not have lazily evaluated assertions. 
If you are writing a computational expensive check, rather user `Lazy.assert(predicate)`.  
  
  

### FIELDS/CONSTRUCTORS/METHODS ORDER IN A CLASS 

When writing a new class (or re-factoring a current one), fields should come first, 
followed by class constructors and then the other methods.


### NON-DETERMINISM

_EvoMaster_ uses randomized algorithms. Running it twice on the same application can give 
different results. 
This is a problem for testing and debugging _EvoMaster_ itself, as for example the test cases 
will be _flaky_.
To avoid such issues, we must control the source of non-determinism.
All randomness sources __MUST__ come from the `Randomness` class.
Some data-structures could lead to non-deterministic behavior (e.g., iteration over a `Set` does not 
guarantee the order).
This does not seem the case for the default data-structures in Kotlin, but it is definitively 
a problem in Java, e.g., `HashSet` vs. `LinkedHashSet`.

In _EvoMaster_ we do have checks for its determinism. This is achieved by running some E2E tests twice
with verbose logging, and then compare the logs for an _exact_ match.
If some logs are not deterministic (e.g., printing out for how many seconds the search ran), those should
be inside a check for `EMConfig.avoidNonDeterministicLogs`.
  

When running _EvoMaster_ on an application, the _seed_ for the random generator is taken from
the CPU clock.
To make a run deterministic, you will need to use the `--seed` option to specify a constant seed. 


### NAMING CONVENTION
We follow the typical naming convention used in `Java`: class names start in capital letter
(e.g., `class Foo`), whereas we use camel-case for variables and 
methods (e.g., `void fooBar()` and `String helloWorld;`).
Constants in `Java` (but usually not in `Kotlin`, unless they are global public variables in a companion object) 
would be typically in upper-case using snake-case
(e.g., `final String HELLO_WORLD`).
Kebab-case should be avoided for names of classes/methods/variables 
(e.g., no `String hello-world`, which anyway would not compile).

Regarding packages and modules, it is a bit more tricky. In this project, the current
rules are the following (but might change if given arguments for a better approach):
no dashes `-` and no upper-case in the package names, but `-` are fine (and preferable) in module names.
For example, `org.EvoMaster.foo-bar` would be wrong for 2 reasons, which could be fixed
with `org.evomaster.foobar` or `org.evomaster.foo.bar`. 
On the other hand, a Maven module called `foo-bar` would be fine, 
but not `Foo-bar`.
The motivation here is that modules are mapped to folders on the operating system,
and we need to avoid issues with OSs like Windows that are case insensitive, and with `.`
treated as beginning of a file extension. 

All code written for `EvoMaster` must be inside the package `org.evomaster.*`.
Each module must define a subpackage, with a name somehow related to the module itself.
Dashes `-` in the module name would be either stripped or replaced with dots `.`. 
For example, a module called `controller-api` under the module `client-java` could
define a package called `org.evomaster.client.java.controller.api`.
Note that it is imperative that no module defines the same subpackage, as to avoid 
class name conflicts. 

All names should use ASCII letters. Non-ASCII ones like ø or Å must be avoided.



### MAVEN MODULE HIERARCHY

`EvoMaster` is built with `Maven`, with a hierarchy of submodules. 
Given a module `X` declaring a submodule `Y` with `<module>` in its `pom.xml` file,
then `Y` **must** declare `X` as parent with `<parent>`.
Do no break the hierarchy by pointing to a parent outside `EvoMaster` (e.g., 
something like `spring-boot-starter-parent`).
If you need to use such external poms, you can import them as dependency, i.e., specifying
the `<scope>import</scope>` tag. 

DEPRECATED: When creating a new module, it is also important to add it as a dependency to `report`,
so that aggregated, transitive code coverage can be calculated.


### MAVEN DEPENDENCY VERSION FOR LIBRARIES

All dependency `<version>` tags must be declared in the *root* `pom.xml` file, 
in the `<dependencyManagement>` section.
Submodules *must* not declare a version for a library, and rather refer to the ones in
the root using just `<groupId>` and `<artifactId>` (but possibly overriding some configurations,
like `<scope>`).

Motivation: must have only a single version of a library in `EvoMaster`. Specifying versions
in submodules can lead to duplicated `<version>` declarations with different version numbers.
All version numbers should be easily audited, and so should be in a single file (i.e., the
*root* `pom.xml`).

There are cases in which we might need different versions of the same library in different modules (e.g., recall the difference between `core`, `client` and `e2e` modules).
And the are cases as well in which adding a dependency management definition can have side effects on transitively imported libraries.
In those cases, a dependency management declaration in the root pom file would be problematic.
In such a case, should still have the version number declared as a property (see `<properties>` entry) in the root pom file.


### THIRD-PARTY LIBRARIES

In general, adding a new dependency is fine, but few things to consider:

* __NEVER__ ever add a GPL licensed library, unless it is under the so called _classpath exception_.
  Note that LGPL libraries are fine.
 
* When adding a new library, check who is maintaining it, and when was its last update.
  No longer maintained libraries should be avoided. 

* Libraries might need to be *shaded* if added to the client controller module.

* Best to always ask the team lead before adding any new library (especially if you do not know what shading is). 
      
### THIRD-PARTY CODE
  
As a rule of thumb, to avoid possible issues with copyrights and license compliance, 
we should not include code directly from third-party sources.
However, when that happens, it __MUST__ be made clear in the files themselves (e.g.,
with comments in their top, with URLs of the original sources). 
Furthermore, this information should also be added to the [reused_code.md](./reused_code.md) file.  
  
  
### Trello
If you are among the core developers of `EvoMaster`, you should get an invitation to join
[Trello](https://trello.com).
We use it to track activities and assign tasks. Anyone can create new tasks/cards.
Current usage:
- `On going`: tasks that are currently under development. Those must be assigned to at least
   1 person.
- `Done`: tasks that are fully done. We do not delete them, e.g., just in case if need to look at
   them again in the future.
   Even when a task is completed, the moving from `On going` to `Done` should be carried out 
   __only__ during a developer meeting (so it can be demoed or at least discussed).
   Furthermore, a done task should be added on top of the `Done` list.
   In this way, by looking at the top of the list, one can see what were the most recent changes.    
- `Important, to do soon`: high priority tasks which have not been started yet.
- `Issues/bugs`: reported bugs which are not trivial to fix. For developers, better to report them
  here than GitHub issue page.
- `Backlog-*`: different backlogs, divided by topic.


### MAKING A NEW RELEASE
Only the project manager should make a new release, as it requires a password.
Instructions can be found [here](./release.md).


### JDK VERSIONS
At this point, we only support JDK __8__ and the following major LTS versions.
_EvoMaster_ must be built with JDK 8, but still must be able to run it with the most recent LTS JDK.
Can be useful to setup your machine to easily switch between different JDK versions.
For example, if you are using a Mac, in your `~/.profile` configuration, you could have something 
like:
```
export JAVA_HOME_8=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/ 
export JAVA_HOME_11=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home/ 

export JAVA_HOME=$JAVA_HOME_11 
export PATH=$JAVA_HOME/bin:$PATH

alias java8='$JAVA_HOME_8/bin/java'
alias java11='$JAVA_HOME_11/bin/java'
alias mvn8='JAVA_HOME=$JAVA_HOME_8 && mvn'
alias mvn11='JAVA_HOME=$JAVA_HOME_11 && mvn'
```

If you are using Windows, it does not seem there is a simple way to define aliases.
Besides setting up the `JAVA_HOME` environment variable, it can be useful to set up an environment variable for each LTS JDK version, e.g., `JAVA_HOME_8`, `JAVA_HOME_11` and `JAVA_HOME_17` (of course, you will need to install all those JDKs...).
Then, from a bash shell (e.g., Git Bash), you can build with Maven using:

`JAVA_HOME=$JAVA_HOME_11 mvn <your_inputs>`

For example, try it with:

`JAVA_HOME=$JAVA_HOME_11 mvn --version`

You can also call `java` directly with:

`$JAVA_HOME_17/bin/java -version`





 
