# Command-Line Options

_EvoMaster_ has several options that can be configured. 
Those can be set on the command-line when _EvoMaster_ is started.

There are 3 types of options:

* __Important__: those are the main options that most users will need to set
                and know about.

* __Internal__: these are low-level tuning options, which most users do not need
                to modify. These were mainly introduced when experimenting with 
                different configurations to maximize the performance of _EvoMaster_. 
                Some of these options are used to collect more info on the search, to help
                debugging issues in _EvoMaster_ itself.                                   
                
* __Experimental__: these are work-in-progress options, for features still under development
                    and testing.        
         

 The list of available options can also be displayed by using `--help`, e.g.:

 `java -jar evomaster.jar --help`
  
  Options might also have *constraints*, e.g., a numeric value within a defined range,
  or a string being an URL.
  In some cases, strings might only be chosen within a specific set of possible values (i.e., an Enum).
  If any constraint is not satisfied, _EvoMaster_ will fail with an error message.
  
  When used, all options need to be prefixed with a `--`, e.g., `--maxTime`.
  
## Important Command-Line Options

|Options|Description|
|---|---|
|`maxTime`| __String__. Maximum amount of time allowed for the search.  The time is expressed with a string where hours (`h`), minutes (`m`) and seconds (`s`) can be specified, e.g., `1h10m120s` and `72m` are both valid and equivalent. Each component (i.e., `h`, `m` and `s`) is optional, but at least one must be specified.  In other words, if you need to run the search for just `30` seconds, you can write `30s`  instead of `0h0m30s`. **The more time is allowed, the better results one can expect**. But then of course the test generation will take longer. For how long should _EvoMaster_ be left run? The default 1 _minute_ is just for demonstration. __We recommend to run it between 1 and 24 hours__, depending on the size and complexity  of the tested application. You can get better results by combining this option with `--prematureStop`. For example, something like `--maxTime 24h --prematureStop 1h` will run the search for 24 hours, but then it will stop at any point in time in which there has be no improvement in the last hour. *Constraints*: `regex (\s*)((?=(\S+))(\d+h)?(\d+m)?(\d+s)?)(\s*)`. *Default value*: `60s`.|
|`prematureStop`| __String__. Max amount of time the search is going to wait since last improvement (on metrics we optimize for, like fault finding and code/schema coverage). If there is no improvement within this allotted max time, then the search will be prematurely stopped, regardless of what specified in --maxTime option. *Constraints*: `regex ((\s*)((?=(\S+))(\d+h)?(\d+m)?(\d+s)?)(\s*))\|(^$)`. *Default value*: `""`.|
|`outputFolder`| __String__. The path directory of where the generated test classes should be saved to. *Default value*: `generated_tests`.|
|`configPath`| __String__. File path for file with configuration settings. Supported formats are YAML and TOML. When EvoMaster starts, it will read such file and import all configurations from it. *Constraints*: `regex .*\.(yml\|yaml\|toml)`. *Default value*: `em.yaml`.|
|`outputFilePrefix`| __String__. The name prefix of generated file(s) with the test cases, without file type extension. In JVM languages, if the name contains '.', folders will be created to represent the given package structure. Also, in JVM languages, should not use '-' in the file name, as not valid symbol for class identifiers. This prefix be combined with the outputFileSuffix to combined the final name. As EvoMaster can split the generated tests among different files, each will get a label, and the names will be in the form prefix+label+suffix. *Constraints*: `regex [-a-zA-Z$_][-0-9a-zA-Z$_]*(.[-a-zA-Z$_][-0-9a-zA-Z$_]*)*`. *Default value*: `EvoMaster`.|
|`outputFileSuffix`| __String__. The name suffix for the generated file(s), to be added before the file type extension. As EvoMaster can split the generated tests among different files, each will get a label, and the names will be in the form prefix+label+suffix. *Constraints*: `regex [-a-zA-Z$_][-0-9a-zA-Z$_]*(.[-a-zA-Z$_][-0-9a-zA-Z$_]*)*`. *Default value*: `Test`.|
|`outputFormat`| __Enum__. Specify in which format the tests should be outputted. If left on `DEFAULT`, for white-box testing then the value specified in the _EvoMaster Driver_ will be used. On the other hand, for black-box testing it will default to a predefined type (e.g., Python). *Valid values*: `DEFAULT, JAVA_JUNIT_5, JAVA_JUNIT_4, KOTLIN_JUNIT_4, KOTLIN_JUNIT_5, JS_JEST, PYTHON_UNITTEST`. *Default value*: `DEFAULT`.|
|`testTimeout`| __Int__. Enforce timeout (in seconds) in the generated tests. This feature might not be supported in all frameworks. If 0 or negative, the timeout is not applied. *Default value*: `60`.|
|`blackBox`| __Boolean__. Use EvoMaster in black-box mode. This does not require an EvoMaster Driver up and running. However, you will need to provide further option to specify how to connect to the SUT. *Default value*: `false`.|
|`bbSwaggerUrl`| __String__. When in black-box mode for REST APIs, specify the URL of where the OpenAPI/Swagger schema can be downloaded from. If the schema is on the local machine, you can use a URL starting with 'file://'. If the given URL is neither starting with 'file' nor 'http', then it will be treated as a local file path. *Default value*: `""`.|
|`bbTargetUrl`| __String__. When in black-box mode, specify the URL of where the SUT can be reached, e.g., http://localhost:8080 . In REST, if this is missing, the URL will be inferred from OpenAPI/Swagger schema. In GraphQL, this must point to the entry point of the API, e.g., http://localhost:8080/graphql . *Constraints*: `URL`. *Default value*: `""`.|
|`ratePerMinute`| __Int__. Rate limiter, of how many actions to do per minute. For example, when making HTTP calls towards an external service, might want to limit the number of calls to avoid bombarding such service (which could end up becoming equivalent to a DoS attack). A value of zero or negative means that no limiter is applied. This is needed only for black-box testing of remote services. *Default value*: `0`.|
|`header0`| __String__. In black-box testing, we still need to deal with authentication of the HTTP requests. With this parameter it is possible to specify a HTTP header that is going to be added to most requests. This should be provided in the form _name:value_. If more than 1 header is needed, use as well the other options _header1_ and _header2_. *Constraints*: `regex (.+:.+)\|(^$)`. *Default value*: `""`.|
|`header1`| __String__. See documentation of _header0_. *Constraints*: `regex (.+:.+)\|(^$)`. *Default value*: `""`.|
|`header2`| __String__. See documentation of _header0_. *Constraints*: `regex (.+:.+)\|(^$)`. *Default value*: `""`.|
|`endpointFocus`| __String__. Concentrate search on only one single REST endpoint. *Default value*: `null`.|
|`endpointPrefix`| __String__. Concentrate search on a set of REST endpoints defined by a common prefix. *Default value*: `null`.|
|`endpointTagFilter`| __String__. Comma-separated list of OpenAPI/Swagger 'tags' definitions. Only the REST endpoints having at least one of such tags will be fuzzed. If no tag is specified here, then such filter is not applied. *Default value*: `null`.|
|`sutControllerHost`| __String__. Host name or IP address of where the SUT EvoMaster Controller Driver is listening on. This option is only needed for white-box testing. *Default value*: `localhost`.|
|`sutControllerPort`| __Int__. TCP port of where the SUT EvoMaster Controller Driver is listening on. This option is only needed for white-box testing. *Constraints*: `min=0.0, max=65535.0`. *Default value*: `40100`.|
|`overrideOpenAPIUrl`| __String__. If specified, override the OpenAPI URL location given by the EvoMaster Driver. This option is only needed for white-box testing. *Constraints*: `URL`. *Default value*: `""`.|

## Internal Command-Line Options

|Options|Description|
|---|---|
|`S1dR`| __Double__. Specify a probability to apply S1dR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|`S1iR`| __Double__. Specify a probability to apply S1iR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|`S2dR`| __Double__. Specify a probability to apply S2dR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|`SMdR`| __Double__. Specify a probability to apply SMdR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|`adaptiveGeneSelectionMethod`| __Enum__. Specify a strategy to select genes for mutation adaptively. *Valid values*: `NONE, AWAY_NOIMPACT, APPROACH_IMPACT, APPROACH_LATEST_IMPACT, APPROACH_LATEST_IMPROVEMENT, BALANCE_IMPACT_NOIMPACT, BALANCE_IMPACT_NOIMPACT_WITH_E, ALL_FIXED_RAND`. *Default value*: `APPROACH_IMPACT`.|
|`addPreDefinedTests`| __Boolean__. Add predefined tests at the end of the search. An example is a test to fetch the schema of RESTful APIs. *Default value*: `true`.|
|`addTestComments`| __Boolean__. Add summary comments on each test. *Default value*: `true`.|
|`advancedBlackBoxCoverage`| __Boolean__. Apply more advanced coverage criteria for black-box testing. This can result in larger generated test suites. *Default value*: `true`.|
|`algorithm`| __Enum__. The algorithm used to generate test cases. The default depends on whether black-box or white-box testing is done. *Valid values*: `DEFAULT, SMARTS, MIO, RANDOM, WTS, MOSA, RW, StandardGA, MonotonicGA, SteadyStateGA`. *Default value*: `DEFAULT`.|
|`allowInvalidData`| __Boolean__. When generating data, allow in some cases to use invalid values on purpose. *Default value*: `true`.|
|`appendToStatisticsFile`| __Boolean__. Whether should add to an existing statistics file, instead of replacing it. *Default value*: `false`.|
|`archiveAfterMutationFile`| __String__. Specify a path to save archive after each mutation during search, only useful for debugging. *DEBUG option*. *Default value*: `archive.csv`.|
|`archiveGeneMutation`| __Enum__. Whether to enable archive-based gene mutation. *Valid values*: `NONE, SPECIFIED, SPECIFIED_WITH_TARGETS, SPECIFIED_WITH_SPECIFIC_TARGETS, SPECIFIED_WITH_TARGETS_DIRECTION, SPECIFIED_WITH_SPECIFIC_TARGETS_DIRECTION, ADAPTIVE`. *Default value*: `SPECIFIED_WITH_SPECIFIC_TARGETS`.|
|`archiveTargetLimit`| __Int__. Limit of number of individuals per target to keep in the archive. *Constraints*: `min=1.0`. *Default value*: `10`.|
|`avoidNonDeterministicLogs`| __Boolean__. At times, we need to run EvoMaster with printed logs that are deterministic. For example, this means avoiding printing out time-stamps. *Default value*: `false`.|
|`baseTaintAnalysisProbability`| __Double__. Probability to use input tracking (i.e., a simple base form of taint-analysis) to determine how inputs are used in the SUT. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`bbExperiments`| __Boolean__. Only used when running experiments for black-box mode, where an EvoMaster Driver would be present, and can reset state after each experiment. *Default value*: `false`.|
|`blackBoxCleanUp`| __Boolean__. In black-box testing, aim at adding calls to reset the state of the SUT after it has been modified by the test. For example, in REST APIs, DELETE operations are added (if any exist) after each successful POST/PUT. However, this is done heuristically. There is no guarantee the state will be properly cleaned-up, this is just a best effort attempt. *Default value*: `true`.|
|`bloatControlForSecondaryObjective`| __Boolean__. Whether secondary objectives are less important than test bloat control. *Default value*: `false`.|
|`coveredTargetFile`| __String__. Specify a file which saves covered targets info regarding generated test suite. *Default value*: `coveredTargets.txt`.|
|`coveredTargetSortedBy`| __Enum__. Specify a format to organize the covered targets by the search. *Valid values*: `NAME, TEST`. *Default value*: `NAME`.|
|`createConfigPathIfMissing`| __Boolean__. If there is no configuration file, create a default template at given configPath location. However this is done only on the 'default' location. If you change 'configPath', no new file will be created. *Default value*: `true`.|
|`createTests`| __Boolean__. Specify if test classes should be created as output of the tool. Usually, you would put it to 'false' only when debugging EvoMaster itself. *Default value*: `true`.|
|`customNaming`| __Boolean__. Enable custom naming and sorting criteria. *Default value*: `true`.|
|`d`| __Double__. When weight-based mutation rate is enabled, specify a percentage of calculating mutation rate based on a number of candidate genes to mutate. For instance, d = 1.0 means that the mutation rate fully depends on a number of candidate genes to mutate, and d = 0.0 means that the mutation rate fully depends on weights of candidates genes to mutate. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.8`.|
|`dependencyFile`| __String__. Specify a file that saves derived dependencies. *DEBUG option*. *Default value*: `dependencies.csv`.|
|`doCollectImpact`| __Boolean__. Specify whether to collect impact info that provides an option to enable of collecting impact info when archive-based gene selection is disable. *DEBUG option*. *Default value*: `false`.|
|`doesApplyNameMatching`| __Boolean__. Whether to apply text/name analysis to derive relationships between name entities, e.g., a resource identifier with a name of table. *Default value*: `true`.|
|`e_u1f984`| __Boolean__. QWN0aXZhdGUgdGhlIFVuaWNvcm4gTW9kZQ==. *Default value*: `false`.|
|`elitesCount`| __Int__. Number of elite individuals to be preserved when forming the next population in population-based search algorithms that do not use an archive, like for example Genetic Algorithms. *Constraints*: `min=0.0`. *Default value*: `1`.|
|`employSmartDbClean`| __Boolean__. Specify whether to employ smart database clean to clear data in the database if the SUT has.`null` represents to employ the setting specified on the EM driver side. *Default value*: `null`.|
|`enableBasicAssertions`| __Boolean__. Generate basic assertions. Basic assertions (comparing the returned object to itself) are added to the code. NOTE: this should not cause any tests to fail. *Default value*: `true`.|
|`enableNLPParser`| __Boolean__. Whether to employ NLP parser to process text. Note that to enable this parser, it is required to build the EvoMaster with the resource profile, i.e., mvn clean install -Presourceexp -DskipTests. *Default value*: `false`.|
|`enableOptimizedTestSize`| __Boolean__. Based on some heuristics, there are cases in which 'maxTestSize' can be overridden at runtime. *Default value*: `true`.|
|`enableProcessMonitor`| __Boolean__. Whether or not enable a search process monitor for archiving evaluated individuals and Archive regarding an evaluation of search. This is only needed when running experiments with different parameter settings. *DEBUG option*. *Default value*: `false`.|
|`enablePureRPCTestGeneration`| __Boolean__. Whether to generate RPC endpoint invocation which is independent from EM driver. *Default value*: `true`.|
|`enableRPCAssertionWithInstance`| __Boolean__. Whether to generate RPC Assertions based on response instance. *Default value*: `true`.|
|`enableRPCCustomizedResponseTargets`| __Boolean__. Whether to enable customized responses indicating business logic. *Default value*: `true`.|
|`enableRPCExtraResponseTargets`| __Boolean__. Whether to enable extra targets for responses, e.g., regarding nullable response, having extra targets for whether it is null. *Default value*: `true`.|
|`enableSchemaConstraintHandling`| __Boolean__. Whether to employ constraints specified in API schema (e.g., OpenAPI) in test generation. *Default value*: `true`.|
|`enableStructureMutation`| __Boolean__. Whether or not to enable a structure mutation for mutating individuals. This feature can only be activated for algorithms that support structural mutation, such as MIO or RW. *Default value*: `true`.|
|`enableTrackEvaluatedIndividual`| __Boolean__. Whether to enable tracking the history of modifications of the individuals with its fitness values (i.e., evaluated individual) during the search. Note that we enforced that set enableTrackIndividual false when enableTrackEvaluatedIndividual is true since information of individual is part of evaluated individual. *Default value*: `true`.|
|`enableTrackIndividual`| __Boolean__. Whether to enable tracking the history of modifications of the individuals during the search. *Default value*: `false`.|
|`enableWeightBasedMutationRateSelectionForGene`| __Boolean__. Specify whether to enable weight-based mutation selection for selecting genes to mutate for a gene. *Default value*: `true`.|
|`endNumberOfMutations`| __Int__. Number of applied mutations on sampled individuals, by the end of the search. *Constraints*: `min=0.0`. *Default value*: `10`.|
|`errorTextEpsilon`| __Double__. The Distance Metric Error Text may use several values for epsilon.During experimentation, it may be useful to adjust these values. Epsilon describes the size of the neighbourhood used for clustering, so may result in different clustering results.Epsilon should be between 0.0 and 1.0. If the value is outside of that range, epsilon will use the default of 0.8. *Constraints*: `min=0.0, max=1.0`. *Default value*: `0.8`.|
|`exceedTargetsFile`| __String__. Specify a path to save all not covered targets when the number is more than 100. *DEBUG option*. *Default value*: `exceedTargets.txt`.|
|`excludeTargetsForImpactCollection`| __String__. Specify prefixes of targets (e.g., MethodReplacement, Success_Call, Local) which will exclude in impact collection. Multiple exclusions should be separated with semicolon (i.e., ;). *Constraints*: `regex ^(\b(None\|NONE\|none)\b\|(\b(Class\|CLASS\|class\|Line\|LINE\|line\|Branch\|BRANCH\|branch\|MethodReplacement\|METHODREPLACEMENT\|method[r\|R]eplacement\|Success_Call\|SUCCESS_CALL\|success_[c\|C]all\|Local\|LOCAL\|local\|PotentialFault\|POTENTIALFAULT\|potential[f\|F]ault)\b(;\b(Class\|CLASS\|class\|Line\|LINE\|line\|Branch\|BRANCH\|branch\|MethodReplacement\|METHODREPLACEMENT\|method[r\|R]eplacement\|Success_Call\|SUCCESS_CALL\|success_[c\|C]all\|Local\|LOCAL\|local\|PotentialFault\|POTENTIALFAULT\|potential[f\|F]ault)\b)*))$`. *Default value*: `Local;MethodReplacement`.|
|`expandRestIndividuals`| __Boolean__. Enable to expand the genotype of REST individuals based on runtime information missing from Swagger. *Default value*: `true`.|
|`exportCoveredTarget`| __Boolean__. Specify whether to export covered targets info. *Default value*: `false`.|
|`exportDependencies`| __Boolean__. Specify whether to export derived dependencies among resources. *DEBUG option*. *Default value*: `false`.|
|`exportImpacts`| __Boolean__. Specify whether to export derived impacts among genes. *DEBUG option*. *Default value*: `false`.|
|`extraHeader`| __Boolean__. Add an extra HTTP header, to analyze how it is used/read by the SUT. Needed to discover new headers that were not specified in the schema. *Default value*: `true`.|
|`extraHeuristicsFile`| __String__. Where the extra heuristics file (if any) is going to be written (in CSV format). *Default value*: `extra_heuristics.csv`.|
|`extraQueryParam`| __Boolean__. Add an extra query param, to analyze how it is used/read by the SUT. Needed to discover new query params that were not specified in the schema. *Default value*: `true`.|
|`extractMongoExecutionInfo`| __Boolean__. Enable extracting Mongo execution info. *Default value*: `true`.|
|`extractSqlExecutionInfo`| __Boolean__. Enable extracting SQL execution info. *Default value*: `true`.|
|`feedbackDirectedSampling`| __Enum__. Specify whether when we sample from archive we do look at the most promising targets for which we have had a recent improvement. *Valid values*: `NONE, LAST, FOCUSED_QUICKEST`. *Default value*: `FOCUSED_QUICKEST`.|
|`fixedRateMutation`| __Double__. Define the probability of happening mutation in the genetic algorithms. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.04`.|
|`focusedSearchActivationTime`| __Double__. The percentage of passed search before starting a more focused, less exploratory one. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.8`.|
|`forceSqlAllColumnInsertion`| __Boolean__. Force filling data of all columns when inserting new row, instead of only minimal required set. *Default value*: `true`.|
|`geneMutationStrategy`| __Enum__. Strategy used to define the mutation probability. *Valid values*: `ONE_OVER_N, ONE_OVER_N_BIASED_SQL`. *Default value*: `ONE_OVER_N_BIASED_SQL`.|
|`geneWeightBasedOnImpactsBy`| __Enum__. Specify a strategy to calculate a weight of a gene based on impacts. *Valid values*: `SORT_COUNTER, SORT_RATIO, COUNTER, RATIO`. *Default value*: `RATIO`.|
|`generateMongoData`| __Boolean__. Enable EvoMaster to generate Mongo data with direct accesses to the database. *Default value*: `true`.|
|`generateSqlDataWithSearch`| __Boolean__. Enable EvoMaster to generate SQL data with direct accesses to the database. Use a search algorithm. *Default value*: `true`.|
|`heuristicsForMongo`| __Boolean__. Tracking of Mongo commands to improve test generation. *Default value*: `true`.|
|`heuristicsForSQL`| __Boolean__. Tracking of SQL commands to improve test generation. *Default value*: `true`.|
|`impactAfterMutationFile`| __String__. Specify a path to save collected impact info after each mutation during search, only useful for debugging. *DEBUG option*. *Default value*: `impactSnapshot.csv`.|
|`impactFile`| __String__. Specify a path to save derived genes. *DEBUG option*. *Default value*: `impact.csv`.|
|`instrumentMR_BASE`| __Boolean__. Execute instrumentation for method replace with category BASE. Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin on the JVM. *Default value*: `true`.|
|`instrumentMR_EXT_0`| __Boolean__. Execute instrumentation for method replace with category EXT_0. Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin on the JVM. *Default value*: `true`.|
|`instrumentMR_MONGO`| __Boolean__. Execute instrumentation for method replace with category MONGO. Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin on the JVM. *Default value*: `true`.|
|`instrumentMR_SQL`| __Boolean__. Execute instrumentation for method replace with category SQL. Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin on the JVM. *Default value*: `true`.|
|`jaCoCoAgentLocation`| __String__. Path on filesystem of where JaCoCo Agent jar file is located. Option meaningful only for External Drivers for JVM. If left empty, it is not used. Note that this only impact the generated output test cases. *Constraints*: `regex (.*jacoco.*\.jar)\|(^$)`. *Default value*: `""`.|
|`jaCoCoCliLocation`| __String__. Path on filesystem of where JaCoCo CLI jar file is located. Option meaningful only for External Drivers for JVM. If left empty, it is not used. Note that this only impact the generated output test cases. *Constraints*: `regex (.*jacoco.*\.jar)\|(^$)`. *Default value*: `""`.|
|`jaCoCoOutputFile`| __String__. Destination file for JaCoCo. Option meaningful only for External Drivers for JVM. If left empty, it is not used. Note that this only impact the generated output test cases. *Default value*: `""`.|
|`jaCoCoPort`| __Int__. Port used by JaCoCo to export coverage reports. *Constraints*: `min=0.0, max=65535.0`. *Default value*: `8899`.|
|`javaCommand`| __String__. Command for 'java' used in the External Drivers. Useful for when there are different JDK installed on same machine without the need to update JAVA_HOME. Note that this only impact the generated output test cases. *Default value*: `java`.|
|`jsControllerPath`| __String__. When generating tests in JavaScript, there is the need to know where the driver is located in respect to the generated tests. *Default value*: `./app-driver.js`.|
|`killSwitch`| __Boolean__. Try to enforce the stopping of SUT business-level code. This is needed when TCP connections timeouts, to avoid thread executions from previous HTTP calls affecting the current one. *Default value*: `true`.|
|`labelForExperimentConfigs`| __String__. Further label to represent the names of CONFIGS sets in experiment scripts, e.g., exp.py. *Default value*: `-`.|
|`labelForExperiments`| __String__. When running experiments and statistic files are generated, all configs are saved. So, this one can be used as extra label for classifying the experiment. *Default value*: `-`.|
|`lastLineEpsilon`| __Double__. The Distance Metric Last Line may use several values for epsilon.During experimentation, it may be useful to adjust these values. Epsilon describes the size of the neighbourhood used for clustering, so may result in different clustering results.Epsilon should be between 0.0 and 1.0. If the value is outside of that range, epsilon will use the default of 0.8. *Constraints*: `min=0.0, max=1.0`. *Default value*: `0.8`.|
|`maxAssertionForDataInCollection`| __Int__. Specify a maximum number of data in a collection to be asserted in the generated tests. Note that zero means that only the size of the collection will be asserted. A negative value means all data in the collection will be asserted (i.e., no limit). *Default value*: `3`.|
|`maxEvaluations`| __Int__. Maximum number of action or individual evaluations (depending on chosen stopping criterion) for the search. A fitness evaluation can be composed of 1 or more actions, like for example REST calls or SQL setups. The more actions are allowed, the better results one can expect. But then of course the test generation will take longer. Only applicable depending on the stopping criterion. *Constraints*: `min=1.0`. *Default value*: `1000`.|
|`maxLengthForCommentLine`| __Int__. Max length for test comments. Needed when enumerating some names/values, making comments too long to be on a single line. *Constraints*: `min=1.0`. *Default value*: `80`.|
|`maxLengthForStrings`| __Int__. The maximum length allowed for evolved strings. Without this limit, strings could in theory be billions of characters long. *Constraints*: `min=0.0, max=20000.0`. *Default value*: `1024`.|
|`maxLengthForStringsAtSamplingTime`| __Int__. Maximum length when sampling a new random string. Such limit can be bypassed when a string is mutated. *Constraints*: `min=0.0`. *Default value*: `16`.|
|`maxLengthOfTraces`| __Int__. Specify a maxLength of tracking when enableTrackIndividual or enableTrackEvaluatedIndividual is true. Note that the value should be specified with a non-negative number or -1 (for tracking all history). *Constraints*: `min=-1.0`. *Default value*: `10`.|
|`maxResponseByteSize`| __Int__. Maximum size (in bytes) that EM handles response payloads in the HTTP responses. If larger than that, a response will not be stored internally in EM during the test generation. This is needed to avoid running out of memory. *Default value*: `1000000`.|
|`maxSearchSuiteSize`| __Int__. Define the maximum number of tests in a suite in the search algorithms that evolve whole suites, e.g. WTS. *Constraints*: `min=1.0`. *Default value*: `50`.|
|`maxSqlInitActionsPerMissingData`| __Int__. When generating SQL data, how many new rows (max) to generate for each specific SQL Select. *Constraints*: `min=1.0`. *Default value*: `1`.|
|`maxTestCaseNameLength`| __Int__. Specify the hard limit for test case name length. *Default value*: `80`.|
|`maxTestSize`| __Int__. Max number of 'actions' (e.g., RESTful calls or SQL commands) that can be done in a single test. *Constraints*: `min=1.0`. *Default value*: `10`.|
|`maxTimeInSeconds`| __Int__. Maximum number of seconds allowed for the search. The more time is allowed, the better results one can expect. But then of course the test generation will take longer. Only applicable depending on the stopping criterion. If this value is 0, the setting 'maxTime' will be used instead. *Constraints*: `min=0.0`. *Default value*: `0`.|
|`maxlengthOfHistoryForAGM`| __Int__. Specify a maximum length of history when applying archive-based gene mutation. *Default value*: `10`.|
|`minRowOfTable`| __Int__. Specify a minimal number of rows in a table that enables selection (i.e., SELECT sql) to prepare resources for REST Action. In other words, if the number is less than the specified, insertion is always applied. *Constraints*: `min=0.0`. *Default value*: `10`.|
|`minimize`| __Boolean__. Apply a minimization phase to make the generated tests more readable. Achieved coverage would stay the same. Generating shorter test cases might come at the cost of having more test cases. *Default value*: `true`.|
|`minimizeShowLostTargets`| __Boolean__. When applying minimization phase, and some targets get lost when re-computing coverage, then printout a detailed description. *Default value*: `true`.|
|`minimizeThresholdForLoss`| __Double__. Losing targets when recomputing coverage is expected (e.g., constructors of singletons), but problematic if too much. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.2`.|
|`minimizeTimeout`| __Int__. Maximum number of minutes that will be dedicated to the minimization phase. A negative number mean no timeout is considered. A value of 0 means minimization will be skipped, even if minimize=true. *Default value*: `5`.|
|`minimumSizeControl`| __Int__. Specify minimum size when bloatControlForSecondaryObjective. *Constraints*: `min=0.0`. *Default value*: `2`.|
|`mutatedGeneFile`| __String__. Specify a path to save mutation details which is useful for debugging mutation. *DEBUG option*. *Default value*: `mutatedGeneInfo.csv`.|
|`nameWithQueryParameters`| __Boolean__. Specify if true boolean query parameters are included in the test case name. Used for test case naming disambiguation. Only valid for Action based naming strategy. *Default value*: `true`.|
|`namingStrategy`| __Enum__. Specify the naming strategy for test cases. *Valid values*: `NUMBERED, ACTION`. *Default value*: `ACTION`.|
|`outputExecutedSQL`| __Enum__. Whether to output executed sql info. *DEBUG option*. *Valid values*: `NONE, ALL_AT_END, ONCE_EXECUTED`. *Default value*: `NONE`.|
|`overrideAuthExternalEndpointURL`| __String__. Override the value of externalEndpointURL in auth configurations. This is useful when the auth server is running locally on an ephemeral port, or when several instances are run in parallel, to avoid creating/modifying auth configuration files. If what provided is a URL starting with 'http', then full replacement will occur. Otherwise, the input will be treated as a 'hostname:port', and only that info will be updated (e.g., path element of the URL will not change). *Default value*: `null`.|
|`populationSize`| __Int__. Define the population size in the search algorithms that use populations (e.g., Genetic Algorithms, but not MIO). *Constraints*: `min=1.0`. *Default value*: `30`.|
|`probOfApplySQLActionToCreateResources`| __Double__. Specify a probability to apply SQL actions for preparing resources for REST Action. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.1`.|
|`probOfArchiveMutation`| __Double__. Specify a probability to enable archive-based mutation. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`probOfEnablingResourceDependencyHeuristics`| __Double__. Specify whether to enable resource dependency heuristics, i.e, probOfEnablingResourceDependencyHeuristics > 0.0. Note that the option is available to be enabled only if resource-based smart sampling is enable. This option has an effect on sampling multiple resources and mutating a structure of an individual. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.95`.|
|`probOfEnablingSingleInsertionForTable`| __Double__. a probability of enabling single insertion strategy to insert rows into database. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`probOfRandomSampling`| __Double__. Probability of sampling a new individual at random. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.8`.|
|`probOfSelectFromDatabase`| __Double__. Specify a probability that enables selection (i.e., SELECT sql) of data from database instead of insertion (i.e., INSERT sql) for preparing resources for REST actions. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.1`.|
|`probOfSmartSampling`| __Double__. When sampling new test cases to evaluate, probability of using some smart strategy instead of plain random. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.95`.|
|`probRestDefault`| __Double__. In REST, specify probability of using 'default' values, if any is specified in the schema. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.05`.|
|`probRestExamples`| __Double__. In REST, specify probability of using 'example(s)' values, if any is specified in the schema. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.2`.|
|`probUseRestLinks`| __Double__. In REST, enable the supports of 'links' between resources defined in the OpenAPI schema, if any. When sampling a test case, if the last call has links, given this probability new calls are added for the link. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`problemType`| __Enum__. The type of SUT we want to generate tests for, e.g., a RESTful API. If left to DEFAULT, the type will be inferred from the EM Driver. However, in case of ambiguities (e.g., the driver specifies more than one type), then this field must be set with a specific type. This is also the case for Black-Box testing where there is no EM Driver. In this latter case, the system defaults to handle REST APIs. *Valid values*: `DEFAULT, REST, GRAPHQL`. *Experimental values*: `RPC, WEBFRONTEND`. *Default value*: `DEFAULT`.|
|`processFiles`| __String__. Specify a folder to save results when a search monitor is enabled. *DEBUG option*. *Default value*: `process_data`.|
|`processFormat`| __Enum__. Specify a format to save the process data. *DEBUG option*. *Valid values*: `JSON_ALL, TEST_IND, TARGET_TEST_IND, TARGET_HEURISTIC`. *Default value*: `JSON_ALL`.|
|`processInterval`| __Double__. Specify how often to save results when a search monitor is enabled, and 0.0 presents to record all evaluated individual. *DEBUG option*. *Constraints*: `min=0.0, max=50.0`. *Default value*: `0.0`.|
|`recordExceededTargets`| __Boolean__. Whether to record targets when the number is more than 100. *DEBUG option*. *Default value*: `false`.|
|`recordExecutedMainActionInfo`| __Boolean__. Whether to record info of executed actions during search. *DEBUG option*. *Default value*: `false`.|
|`resourceSampleStrategy`| __Enum__. Specify whether to enable resource-based strategy to sample an individual during search. Note that resource-based sampling is only applicable for REST problem with MIO algorithm. *Valid values*: `NONE, Customized, EqualProbability, Actions, TimeBudgets, Archive, ConArchive`. *Default value*: `ConArchive`.|
|`runningInDocker`| __Boolean__. Inform EvoMaster process that it is running inside Docker. Users should not modify this parameter, as it is set automatically in the Docker image of EvoMaster. *Default value*: `false`.|
|`saveArchiveAfterMutation`| __Boolean__. Whether to save archive info after each of mutation, which is typically useful for debugging mutation and archive. *DEBUG option*. *Default value*: `false`.|
|`saveExecutedMainActionInfo`| __String__. Specify a path to save all executed main actions to a file (default is 'executedMainActions.txt'). *DEBUG option*. *Default value*: `executedMainActions.txt`.|
|`saveExecutedSQLToFile`| __String__. Specify a path to save all executed sql commands to a file (default is 'sql.txt'). *DEBUG option*. *Default value*: `sql.txt`.|
|`saveImpactAfterMutation`| __Boolean__. Whether to save impact info after each of mutation, which is typically useful debugging impact driven solutions and mutation. *DEBUG option*. *Default value*: `false`.|
|`saveMutationInfo`| __Boolean__. Whether to save mutated gene info, which is typically used for debugging mutation. *DEBUG option*. *Default value*: `false`.|
|`schemaOracles`| __Boolean__. Validate responses against their schema, to check for inconsistencies. Those are treated as faults. *Default value*: `true`.|
|`searchPercentageExtraHandling`| __Double__. Percentage [0.0,1.0] of elapsed time in the search while trying to infer any extra query parameter and header. After this time has passed, those attempts stop. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.1`.|
|`secondaryObjectiveStrategy`| __Enum__. Strategy used to handle the extra heuristics in the secondary objectives. *Valid values*: `AVG_DISTANCE, AVG_DISTANCE_SAME_N_ACTIONS, BEST_MIN`. *Default value*: `AVG_DISTANCE_SAME_N_ACTIONS`.|
|`security`| __Boolean__. Apply a security testing phase after functional test cases have been generated. *Default value*: `true`.|
|`seed`| __Long__. The seed for the random generator used during the search. A negative value means the CPU clock time will be rather used as seed. *Default value*: `-1`.|
|`showProgress`| __Boolean__. Whether to print how much search done so far. *Default value*: `true`.|
|`skipFailureSQLInTestFile`| __Boolean__. Whether to skip failed SQL commands in the generated test files. *Default value*: `true`.|
|`snapshotInterval`| __Double__. If positive, check how often, in percentage % of the budget, to collect statistics snapshots. For example, every 5% of the time. *Constraints*: `max=50.0`. *Default value*: `-1.0`.|
|`snapshotStatisticsFile`| __String__. Where the snapshot file (if any) is going to be written (in CSV format). *Default value*: `snapshot.csv`.|
|`specializeSQLGeneSelection`| __Boolean__. Whether to specialize sql gene selection to mutation. *Default value*: `true`.|
|`startNumberOfMutations`| __Int__. Number of applied mutations on sampled individuals, at the start of the search. *Constraints*: `min=0.0`. *Default value*: `1`.|
|`startingPerOfGenesToMutate`| __Double__. Specify a starting percentage of genes of an individual to mutate. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`statisticsColumnId`| __String__. An id that will be part as a column of the statistics file (if any is generated). *Default value*: `-`.|
|`statisticsFile`| __String__. Where the statistics file (if any) is going to be written (in CSV format). *Default value*: `statistics.csv`.|
|`stoppingCriterion`| __Enum__. Stopping criterion for the search. *Valid values*: `TIME, ACTION_EVALUATIONS, INDIVIDUAL_EVALUATIONS`. *Default value*: `TIME`.|
|`structureMutationProbability`| __Double__. Probability of applying a mutation that can change the structure of a test. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`taintAnalysisForMapsAndArrays`| __Boolean__. Apply taint analysis to handle special cases of Maps and Arrays. *Default value*: `true`.|
|`taintApplySpecializationProbability`| __Double__. Probability of applying a discovered specialization for a tainted value. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`taintChangeSpecializationProbability`| __Double__. Probability of changing specialization for a resolved taint during mutation. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.1`.|
|`taintOnSampling`| __Boolean__. Whether input tracking is used on sampling time, besides mutation time. *Default value*: `true`.|
|`taintRemoveProbability`| __Double__. Probability of removing a tainted value during mutation. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|`tcpTimeoutMs`| __Int__. Number of milliseconds we are going to wait to get a response on a TCP connection, e.g., when making HTTP calls to a Web API. *Default value*: `30000`.|
|`testCaseSortingStrategy`| __Enum__. Specify the test case sorting strategy. *Valid values*: `COVERED_TARGETS, TARGET_INCREMENTAL`. *Default value*: `TARGET_INCREMENTAL`.|
|`testSuiteFileName`| __String__. DEPRECATED. Rather use _outputFilePrefix_ and _outputFileSuffix_. *Default value*: `""`.|
|`testSuiteSplitType`| __Enum__. Instead of generating a single test file, it could be split in several files, according to different strategies. *Valid values*: `NONE, FAULTS`. *Default value*: `FAULTS`.|
|`tournamentSize`| __Int__. Number of elements to consider in a Tournament Selection (if any is used in the search algorithm). *Constraints*: `min=1.0`. *Default value*: `10`.|
|`treeDepth`| __Int__. Maximum tree depth in mutations/queries to be evaluated. This is to avoid issues when dealing with huge graphs in GraphQL. *Constraints*: `min=1.0`. *Default value*: `4`.|
|`useExtraSqlDbConstraintsProbability`| __Double__. Whether to analyze how SQL databases are accessed to infer extra constraints from the business logic. An example is javax/jakarta annotation constraints defined on JPA entities. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.9`.|
|`useMethodReplacement`| __Boolean__. Apply method replacement heuristics to smooth the search landscape. Note that the method replacement instrumentations would still be applied, it is just that their testing targets will be ignored in the fitness function if this option is set to false. *Default value*: `true`.|
|`useNonIntegerReplacement`| __Boolean__. Apply non-integer numeric comparison heuristics to smooth the search landscape. *Default value*: `true`.|
|`useResponseDataPool`| __Boolean__. Enable the collection of response data, to feed new individuals based on field names matching. *Default value*: `true`.|
|`useTimeInFeedbackSampling`| __Boolean__. Whether to use timestamp info on the execution time of the tests for sampling (e.g., to reward the quickest ones). *Default value*: `true`.|
|`weightBasedMutationRate`| __Boolean__. Whether to enable a weight-based mutation rate. *Default value*: `true`.|
|`writeExtraHeuristicsFile`| __Boolean__. Whether we should collect data on the extra heuristics. Only needed for experiments. *Default value*: `false`.|
|`writeStatistics`| __Boolean__. Whether or not writing statistics of the search process. This is only needed when running experiments with different parameter settings. *Default value*: `false`.|
|`writeWFCReport`| __Boolean__. Output a JSON file representing statistics of the fuzzing session, written in the WFC Report format. This also includes a index.html web application to visualize such data. *Default value*: `true`.|
|`writeWFCReportExcludeWebApp`| __Boolean__. If creating a WFC Report as output, specify if should not generate the index.html web app, i.e., only the JSON report file will be created. *Default value*: `false`.|
|`xoverProbability`| __Double__. Probability of applying crossover operation (if any is used in the search algorithm). *Constraints*: `probability 0.0-1.0`. *Default value*: `0.7`.|

## Experimental Command-Line Options

|Options|Description|
|---|---|
|`abstractInitializationGeneToMutate`| __Boolean__. During mutation, whether to abstract genes for repeated SQL actions. *Default value*: `false`.|
|`aiClassifierRepairActivation`| __Enum__. Specify how the classification of actions's response will be used to execute a possible repair on the action. *Valid values*: `PROBABILITY, THRESHOLD`. *Default value*: `THRESHOLD`.|
|`aiEncoderType`| __Enum__. The encoding strategy applied to transform raw data to the encoded version. *Valid values*: `RAW, NORMAL, UNIT_NORMAL`. *Default value*: `RAW`.|
|`aiModelForResponseClassification`| __Enum__. Model used to learn input constraints and infer response status before making request. *Valid values*: `NONE, GAUSSIAN, KDE, KNN, NN, GLM, DETERMINISTIC`. *Default value*: `NONE`.|
|`aiResponseClassifierLearningRate`| __Double__. Learning rate controlling the step size during parameter updates in classifiers. Relevant for gradient-based models such as GLM and neural networks. A smaller value ensures stable but slower convergence, while a larger value speeds up training but may cause instability. *Default value*: `0.01`.|
|`aiResponseClassifierWarmup`| __Int__. Number of training iterations required to update classifier parameters. For example, in the Gaussian model this affects mean and variance updates. For neural network (NN) models, the warm-up should typically be larger than 1000. *Default value*: `10`.|
|`appendToTargetHeuristicsFile`| __Boolean__. Whether should add to an existing target heuristics file, instead of replacing it. It is only used when processFormat is TARGET_HEURISTIC. *Default value*: `false`.|
|`bbProbabilityUseDataPool`| __Double__. Specify the probability of using the data pool when sampling test cases. This is for black-box (bb) mode. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.8`.|
|`callbackURLHostname`| __String__. HTTP callback verifier hostname. Default is set to 'localhost'. If the SUT is running inside a container (i.e., Docker), 'localhost' will refer to the container. This can be used to change the hostname. *Default value*: `localhost`.|
|`classificationRepairThreshold`| __Double__. If using THRESHOLD for AI Classification Repair, specify its value. All classifications with probability equal or above such threshold value will be accepted. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.8`.|
|`discoveredInfoRewardedInFitness`| __Boolean__. If there is new discovered information from a test execution, reward it in the fitness function. *Default value*: `false`.|
|`dockerLocalhost`| __Boolean__. Replace references to 'localhost' to point to the actual host machine. Only needed when running EvoMaster inside Docker. *Default value*: `false`.|
|`dpcTargetTestSize`| __Int__. Specify a max size of a test to be targeted when either DPC_INCREASING or DPC_DECREASING is enabled. *Default value*: `1`.|
|`dtoForRequestPayload`| __Boolean__. In REST APIs, when request Content-Type is JSON, POJOs are used instead of raw JSON string. Only available for JVM languages. *Default value*: `false`.|
|`employResourceSizeHandlingStrategy`| __Enum__. Specify a strategy to determinate a number of resources to be manipulated throughout the search. *Valid values*: `NONE, RANDOM, DPC`. *Default value*: `NONE`.|
|`enableAdaptiveResourceStructureMutation`| __Boolean__. Specify whether to decide the resource-based structure mutator and resource to be mutated adaptively based on impacts during focused search.Note that it only works when resource-based solution is enabled for solving REST problem. *Default value*: `false`.|
|`enableCustomizedMethodForMockObjectHandling`| __Boolean__. Whether to apply customized method (i.e., implement 'customizeMockingRPCExternalService' for external services or 'customizeMockingDatabase' for database) to handle mock object. *Default value*: `false`.|
|`enableCustomizedMethodForScheduleTaskHandling`| __Boolean__. Whether to apply customized method (i.e., implement 'customizeScheduleTaskInvocation' for invoking schedule task) to invoke schedule task. *Default value*: `false`.|
|`enableRPCCustomizedTestOutput`| __Boolean__. Whether to enable customized RPC Test output if 'customizeRPCTestOutput' is implemented. *Default value*: `false`.|
|`enableWriteSnapshotTests`| __Boolean__. Enable to print snapshots of the generated tests during the search in an interval defined in snapshotsInterval. *Default value*: `false`.|
|`executiveSummary`| __Boolean__. Generate an executive summary, containing an example of each category of potential faults found.NOTE: This option is only meaningful when used in conjunction with test suite splitting. *Default value*: `false`.|
|`expectationsActive`| __Boolean__. Enable Expectation Generation. If enabled, expectations will be generated. A variable called expectationsMasterSwitch is added to the test suite, with a default value of false. If set to true, an expectation that fails will cause the test case containing it to fail. *Default value*: `false`.|
|`exportTestCasesDuringSeeding`| __Boolean__. Whether to export test cases during seeding as a separate file. *Default value*: `false`.|
|`externalRequestHarvesterNumberOfThreads`| __Int__. Number of threads for external request harvester. No more threads than numbers of processors will be used. *Constraints*: `min=1.0`. *Default value*: `2`.|
|`externalRequestResponseSelectionStrategy`| __Enum__. Harvested external request response selection strategy. *Valid values*: `EXACT, CLOSEST_SAME_DOMAIN, CLOSEST_SAME_PATH, RANDOM`. *Default value*: `EXACT`.|
|`externalServiceIP`| __String__. User provided external service IP. When EvoMaster mocks external services, mock server instances will run on local addresses starting from this provided address. Min value is 127.0.0.4. Lower values like 127.0.0.2 and 127.0.0.3 are reserved. *Constraints*: `regex (?!^0*127(\.0*0){2}\.0*[0123]$)^0*127(\.0*(25[0-5]\|2[0-4][0-9]\|1?[0-9]?[0-9])){3}$`. *Default value*: `127.0.0.4`.|
|`externalServiceIPSelectionStrategy`| __Enum__. Specify a method to select the first external service spoof IP address. *Valid values*: `NONE, DEFAULT, USER, RANDOM`. *Default value*: `NONE`.|
|`generateSqlDataWithDSE`| __Boolean__. Enable EvoMaster to generate SQL data with direct accesses to the database. Use Dynamic Symbolic Execution. *Default value*: `false`.|
|`heuristicsForSQLAdvanced`| __Boolean__. If using SQL heuristics, enable more advanced version. *Default value*: `false`.|
|`httpOracles`| __Boolean__. Extra checks on HTTP properties in returned responses, used as automated oracles to detect faults. *Default value*: `false`.|
|`initStructureMutationProbability`| __Double__. Probability of applying a mutation that can change the structure of test's initialization if it has. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`instrumentMR_NET`| __Boolean__. Execute instrumentation for method replace with category NET. Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin on the JVM. *Default value*: `false`.|
|`instrumentMR_OPENSEARCH`| __Boolean__. Execute instrumentation for method replace with category OPENSEARCH. Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin on the JVM. *Default value*: `false`.|
|`languageModelConnector`| __Boolean__. Enable language model connector. *Default value*: `false`.|
|`languageModelConnectorNumberOfThreads`| __Int__. Number of threads for language model connector. No more threads than numbers of processors will be used. *Constraints*: `min=1.0`. *Default value*: `2`.|
|`languageModelName`| __String__. Large-language model name as listed in Ollama. *Default value*: `llama3.2:latest`.|
|`languageModelServerURL`| __String__. Large-language model external service URL. Default is set to Ollama local instance URL. *Default value*: `http://localhost:11434/`.|
|`maxRepairAttemptsInResponseClassification`| __Int__. When the Response Classifier determines an action is going to fail, specify how many attempts will be tried at fixing it. *Constraints*: `min=1.0`. *Default value*: `100`.|
|`maxResourceSize`| __Int__. Specify a max size of resources in a test. 0 means the there is no specified restriction on a number of resources. *Constraints*: `min=0.0`. *Default value*: `0`.|
|`maxSizeDataPool`| __Int__. How much data elements, per key, can be stored in the Data Pool. Once limit is reached, new old will replace old data. *Constraints*: `min=1.0`. *Default value*: `100`.|
|`maxSizeOfExistingDataToSample`| __Int__. Specify a maximum number of existing data in the database to sample in a test when SQL handling is enabled. Note that a negative number means all existing data would be sampled. *Default value*: `-1`.|
|`maxSizeOfHandlingResource`| __Int__. Specify a maximum number of handling (remove/add) resource size at once, e.g., add 3 resource at most. *Constraints*: `min=0.0`. *Default value*: `0`.|
|`maxSizeOfMutatingInitAction`| __Int__. Specify a maximum number of handling (remove/add) init actions at once, e.g., add 3 init actions at most. *Constraints*: `min=0.0`. *Default value*: `0`.|
|`maxTestSizeStrategy`| __Enum__. Specify a strategy to handle a max size of a test. *Valid values*: `SPECIFIED, DPC_INCREASING, DPC_DECREASING`. *Default value*: `SPECIFIED`.|
|`maxTestsPerTestSuite`| __Int__. Specify the maximum number of tests to be generated in one test suite. Note that a negative number presents no limit per test suite. *Default value*: `-1`.|
|`mutationTargetsSelectionStrategy`| __Enum__. Specify a strategy to select targets for evaluating mutation. *Valid values*: `FIRST_NOT_COVERED_TARGET, EXPANDED_UPDATED_NOT_COVERED_TARGET, UPDATED_NOT_COVERED_TARGET`. *Default value*: `FIRST_NOT_COVERED_TARGET`.|
|`prematureStopStrategy`| __Enum__. Specify how 'improvement' is defined: either any kind of improvement even if partial (ANY), or at least one new target is fully covered (NEW). *Valid values*: `ANY, NEW`. *Default value*: `NEW`.|
|`probOfHandlingLength`| __Double__. Specify a probability of applying length handling. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`probOfHarvestingResponsesFromActualExternalServices`| __Double__. a probability of harvesting actual responses from external services as seeds. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`probOfMutatingResponsesBasedOnActualResponse`| __Double__. a probability of mutating mocked responses based on actual responses. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`probOfPrioritizingSuccessfulHarvestedActualResponses`| __Double__. a probability of prioritizing to employ successful harvested actual responses from external services as seeds (e.g., 2xx from HTTP external service). *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`probOfSamplingScheduleTask`| __Double__. Probability of sampling a new individual with schedule tasks. Note that schedule task is only enabled for RPCProblem. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`probOfSmartInitStructureMutator`| __Double__. Specify a probability of applying a smart structure mutator for initialization of the individual. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`probabilityAllOptionalsAreOnOrOff`| __Double__. When sampling a new individual, probability that ALL optional choices are ON, or ALL are OFF. The choice between ON and OFF depends on probabilityOfOnVsOffInAllOptionals. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`saveMockedResponseAsSeparatedFile`| __Boolean__. Whether to save mocked responses as separated files. *Default value*: `false`.|
|`saveScheduleTaskInvocationAsSeparatedFile`| __Boolean__. Whether to save schedule task invocation as separated files. *Default value*: `false`.|
|`saveTargetHeuristicsPrefixes`| __String__. Prefix specifying which targets to record. Each target can be separated by a comma, such as 'Branch,Line,Success, etc'. It is only used when processFormat is TARGET_HEURISTIC. *Default value*: `Branch`.|
|`seedTestCases`| __Boolean__. Whether to seed EvoMaster with some initial test cases. These test cases will be used and evolved throughout the search process. *Default value*: `false`.|
|`seedTestCasesFormat`| __Enum__. Format of the test cases seeded to EvoMaster. *Valid values*: `POSTMAN`. *Default value*: `POSTMAN`.|
|`seedTestCasesPath`| __String__. File path where the seeded test cases are located. *Default value*: `postman.postman_collection.json`.|
|`ssrf`| __Boolean__. To apply SSRF detection as part of security testing. *Default value*: `false`.|
|`structureMutationProFS`| __Double__. Specify a probability of applying structure mutator during the focused search. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`structureMutationProbStrategy`| __Enum__. Specify a strategy to handle a probability of applying structure mutator during the focused search. *Valid values*: `SPECIFIED, SPECIFIED_FS, DPC_TO_SPECIFIED_BEFORE_FS, DPC_TO_SPECIFIED_AFTER_FS, ADAPTIVE_WITH_IMPACT`. *Default value*: `SPECIFIED`.|
|`taintForceSelectionOfGenesWithSpecialization`| __Boolean__. During mutation, force the mutation of genes that have newly discovered specialization from previous fitness evaluations, based on taint analysis. *Default value*: `false`.|
|`targetHeuristicsFile`| __String__. Where the target heuristic values file (if any) is going to be written (in CSV format). It is only used when processFormat is TARGET_HEURISTIC. *Default value*: `targets.csv`.|
|`testResourcePathToSaveMockedResponse`| __String__. Specify test resource path where to save mocked responses as separated files. *Default value*: `""`.|
|`thresholdDistanceForDataPool`| __Int__. Threshold of Levenshtein Distance for key-matching in Data Pool. *Constraints*: `min=0.0`. *Default value*: `2`.|
|`useGlobalTaintInfoProbability`| __Double__. When sampling new individual, check whether to use already existing info on tainted values. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|`useInsertionForSqlHeuristics`| __Boolean__. Specify whether insertions should be used to calculate SQL heuristics instead of retrieving data from real databases. *Default value*: `false`.|
|`useTestMethodOrder`| __Boolean__. Adds TestMethodOrder annotation for JUnit 5 tests. *Default value*: `false`.|
|`useWeightedSampling`| __Boolean__. When sampling from archive based on targets, decide whether to use weights based on properties of the targets (e.g., a target likely leading to a flag will be sampled less often). *Default value*: `false`.|
|`vulnerableInputClassificationStrategy`| __Enum__. Strategy to classify inputs for potential vulnerability classes related to an REST endpoint. *Valid values*: `MANUAL, LLM`. *Default value*: `MANUAL`.|
|`wbProbabilityUseDataPool`| __Double__. Specify the probability of using the data pool when sampling test cases. This is for white-box (wb) mode. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.2`.|
|`writeSnapshotTestsIntervalInSeconds`| __Int__. The size (in seconds) of the interval that the snapshots will be printed, if enabled. *Default value*: `3600`.|
