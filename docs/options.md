# Command-Line Options

_EvoMaster_ has several options that can be configured. 
Those can be set on the command-line when _EvoMaster_ is started.

There are 3 types of options:

* __Important__: those are the main options that most users will need to set
                and know about.

* __Internal__: these are low-level tuning options, which most users do not need
                to modify. These were mainly introduced when experimenting with 
                different configuration to maximize the performance of _EvoMaster_.
                
* __Experimental__: these are work-in-progress options, for features still under development
                    and testing.        
         

 The list of available options can also be displayed by using `--help`, e.g.:

 `java -jar evomaster.jar --help`
  
  Options might also have *constraints*, e.g. a numeric value within a defined range,
  or a string being an URL.
  In some cases, strings might only be chosen within a specific set of possible values (i.e., an Enum).
  If any constraint is not satisfied, _EvoMaster_ will fail with an error message.
  
## Important Command-Line Options

|Options<img width=2000/>|Description|
|---|---|
|<nobr>`--maxTime` &lt;String&gt;</nobr>| Maximum amount of time allowed for the search.  The time is expressed with a string where hours (`h`), minutes (`m`) and seconds (`s`) can be specified, e.g., `1h10m120s` and `72m` are both valid and equivalent. Each component (i.e., `h`, `m` and `s`) is optional, but at least one must be specified.  In other words, if you need to run the search for just `30` seconds, you can write `30s`  instead of `0h0m30s`. **The more time is allowed, the better results one can expect**. But then of course the test generation will take longer. For how long should _EvoMaster_ be left run? The default 1 _minute_ is just for demonstration. __We recommend to run it between 1 and 24 hours__, depending on the size and complexity  of the tested application. *Constraints*: `regex (\s*)((?=([\S]+))(\d+h)?(\d+m)?(\d+s)?)(\s*)`. *Default value*: `60s`.|
|<nobr>`--outputFolder` &lt;String&gt;</nobr>| The path directory of where the generated test classes should be saved to. *Default value*: `src/em`.|
|<nobr>`--testSuiteFileName` &lt;String&gt;</nobr>| The name of generated file with the test cases, without file type extension. In JVM languages, if the name contains '.', folders will be created to represent the given package structure. *Default value*: `EvoMasterTest`.|
|<nobr>`--blackBox` &lt;Boolean&gt;</nobr>| Use EvoMaster in black-box mode. This does not require an EvoMaster Driver up and running. However, you will need to provide further option to specify how to connect to the SUT. *Default value*: `false`.|
|<nobr>`--bbSwaggerUrl` &lt;String&gt;</nobr>| When in black-box mode for REST APIs, specify where the Swagger schema can downloaded from. *Constraints*: `URL`. *Default value*: `""`.|
|<nobr>`--bbTargetUrl` &lt;String&gt;</nobr>| When in black-box mode, specify the URL of where the SUT can be reached. If this is missing, the URL will be inferred from Swagger. *Constraints*: `URL`. *Default value*: `""`.|

## Internal Command-Line Options

|Options<img width=2000/>|Description|
|---|---|
|<nobr>`--algorithm` &lt;Enum&gt;</nobr>| The algorithm used to generate test cases. *Valid values*: `MIO, RANDOM, WTS, MOSA`. *Default value*: `MIO`.|
|<nobr>`--appendToStatisticsFile` &lt;Boolean&gt;</nobr>| Whether should add to an existing statistics file, instead of replacing it. *Default value*: `false`.|
|<nobr>`--archiveTargetLimit` &lt;Int&gt;</nobr>| Limit of number of individuals per target to keep in the archive. *Constraints*: `min=1.0`. *Default value*: `10`.|
|<nobr>`--baseTaintAnalysisProbability` &lt;Double&gt;</nobr>| Probability to use input tracking (i.e., a simple base form of taint-analysis) to determine how inputs are used in the SUT. *Default value*: `0.9`.|
|<nobr>`--bbExperiments` &lt;Boolean&gt;</nobr>| Only used when running experiments for black-box mode, where an EvoMaster Driver would be present, and can reset state after each experiment. *Default value*: `false`.|
|<nobr>`--bloatControlForSecondaryObjective` &lt;Boolean&gt;</nobr>| Whether secondary objectives are less important than test bloat control. *Default value*: `false`.|
|<nobr>`--createTests` &lt;Boolean&gt;</nobr>| Specify if test classes should be created as output of the tool. Usually, you would put it to 'false' only when debugging EvoMaster itself. *Default value*: `true`.|
|<nobr>`--customNaming` &lt;Boolean&gt;</nobr>| Enable custom naming and sorting criteria. *Default value*: `true`.|
|<nobr>`--e_u1f984` &lt;Boolean&gt;</nobr>| QWN0aXZhdGUgdGhlIFVuaWNvcm4gTW9kZQ==. *Default value*: `false`.|
|<nobr>`--enableBasicAssertions` &lt;Boolean&gt;</nobr>| Generate basic assertions. Basic assertions (comparing the returned object to itself) are added to the code. NOTE: this should not cause any tests to fail. *Default value*: `true`.|
|<nobr>`--endNumberOfMutations` &lt;Int&gt;</nobr>| Number of applied mutations on sampled individuals, by the end of the search. *Constraints*: `min=0.0`. *Default value*: `10`.|
|<nobr>`--expandRestIndividuals` &lt;Boolean&gt;</nobr>| Enable to expand the genotype of REST individuals based on runtime information missing from Swagger. *Default value*: `true`.|
|<nobr>`--extraHeuristicsFile` &lt;String&gt;</nobr>| Where the extra heuristics file (if any) is going to be written (in CSV format). *Default value*: `extra_heuristics.csv`.|
|<nobr>`--extractSqlExecutionInfo` &lt;Boolean&gt;</nobr>| Enable extracting SQL execution info. *Default value*: `true`.|
|<nobr>`--feedbackDirectedSampling` &lt;Enum&gt;</nobr>| Specify whether when we sample from archive we do look at the most promising targets for which we have had a recent improvement. *Valid values*: `NONE, LAST, FOCUSED_QUICKEST`. *Default value*: `LAST`.|
|<nobr>`--focusedSearchActivationTime` &lt;Double&gt;</nobr>| The percentage of passed search before starting a more focused, less exploratory one. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|<nobr>`--geneMutationStrategy` &lt;Enum&gt;</nobr>| Strategy used to define the mutation probability. *Valid values*: `ONE_OVER_N, ONE_OVER_N_BIASED_SQL`. *Default value*: `ONE_OVER_N_BIASED_SQL`.|
|<nobr>`--generateSqlDataWithSearch` &lt;Boolean&gt;</nobr>| Enable EvoMaster to generate SQL data with direct accesses to the database. Use a search algorithm. *Default value*: `true`.|
|<nobr>`--heuristicsForSQL` &lt;Boolean&gt;</nobr>| Tracking of SQL commands to improve test generation. *Default value*: `true`.|
|<nobr>`--maxActionEvaluations` &lt;Int&gt;</nobr>| Maximum number of action evaluations for the search. A fitness evaluation can be composed of 1 or more actions, like for example REST calls or SQL setups. The more actions are allowed, the better results one can expect. But then of course the test generation will take longer. Only applicable depending on the stopping criterion. *Constraints*: `min=1.0`. *Default value*: `1000`.|
|<nobr>`--maxResponseByteSize` &lt;Int&gt;</nobr>| Maximum size (in bytes) that EM handles response payloads in the HTTP responses. If larger than that, a response will not be stored internally in EM during the test generation. This is needed to avoid running out of memory. *Default value*: `1000000`.|
|<nobr>`--maxSearchSuiteSize` &lt;Int&gt;</nobr>| Define the maximum number of tests in a suite in the search algorithms that evolve whole suites, e.g. WTS. *Constraints*: `min=1.0`. *Default value*: `50`.|
|<nobr>`--maxSqlInitActionsPerMissingData` &lt;Int&gt;</nobr>| When generating SQL data, how many new rows (max) to generate for each specific SQL Select. *Constraints*: `min=1.0`. *Default value*: `5`.|
|<nobr>`--maxTestSize` &lt;Int&gt;</nobr>| Max number of 'actions' (e.g., RESTful calls or SQL commands) that can be done in a single test. *Constraints*: `min=1.0`. *Default value*: `10`.|
|<nobr>`--maxTimeInSeconds` &lt;Int&gt;</nobr>| Maximum number of seconds allowed for the search. The more time is allowed, the better results one can expect. But then of course the test generation will take longer. Only applicable depending on the stopping criterion. If this value is 0, the setting 'maxTime' will be used instead. *Constraints*: `min=0.0`. *Default value*: `0`.|
|<nobr>`--outputFormat` &lt;Enum&gt;</nobr>| Specify in which format the tests should be outputted. *Valid values*: `DEFAULT, JAVA_JUNIT_5, JAVA_JUNIT_4, KOTLIN_JUNIT_4, KOTLIN_JUNIT_5`. *Default value*: `DEFAULT`.|
|<nobr>`--populationSize` &lt;Int&gt;</nobr>| Define the population size in the search algorithms that use populations (e.g., Genetic Algorithms, but not MIO). *Constraints*: `min=1.0`. *Default value*: `30`.|
|<nobr>`--probOfRandomSampling` &lt;Double&gt;</nobr>| Probability of sampling a new individual at random. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|<nobr>`--probOfSmartSampling` &lt;Double&gt;</nobr>| When sampling new test cases to evaluate, probability of using some smart strategy instead of plain random. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|<nobr>`--problemType` &lt;Enum&gt;</nobr>| The type of SUT we want to generate tests for, e.g., a RESTful API. *Valid values*: `REST, WEB`. *Default value*: `REST`.|
|<nobr>`--secondaryObjectiveStrategy` &lt;Enum&gt;</nobr>| Strategy used to handle the extra heuristics in the secondary objectives. *Valid values*: `AVG_DISTANCE, AVG_DISTANCE_SAME_N_ACTIONS, BEST_MIN`. *Default value*: `AVG_DISTANCE_SAME_N_ACTIONS`.|
|<nobr>`--seed` &lt;Long&gt;</nobr>| The seed for the random generator used during the search. A negative value means the CPU clock time will be rather used as seed. *Default value*: `-1`.|
|<nobr>`--showProgress` &lt;Boolean&gt;</nobr>| Whether to print how much search done so far. *Default value*: `true`.|
|<nobr>`--snapshotInterval` &lt;Double&gt;</nobr>| If positive, check how often, in percentage % of the budget, to collect statistics snapshots. For example, every 5% of the time. *Constraints*: `max=50.0`. *Default value*: `-1.0`.|
|<nobr>`--snapshotStatisticsFile` &lt;String&gt;</nobr>| Where the snapshot file (if any) is going to be written (in CSV format). *Default value*: `snapshot.csv`.|
|<nobr>`--startNumberOfMutations` &lt;Int&gt;</nobr>| Number of applied mutations on sampled individuals, at the start of the search. *Constraints*: `min=0.0`. *Default value*: `1`.|
|<nobr>`--statisticsColumnId` &lt;String&gt;</nobr>| An id that will be part as a column of the statistics file (if any is generated). *Default value*: `-`.|
|<nobr>`--statisticsFile` &lt;String&gt;</nobr>| Where the statistics file (if any) is going to be written (in CSV format). *Default value*: `statistics.csv`.|
|<nobr>`--stoppingCriterion` &lt;Enum&gt;</nobr>| Stopping criterion for the search. *Valid values*: `TIME, FITNESS_EVALUATIONS`. *Default value*: `TIME`.|
|<nobr>`--structureMutationProbability` &lt;Double&gt;</nobr>| Probability of applying a mutation that can change the structure of a test. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.5`.|
|<nobr>`--sutControllerHost` &lt;String&gt;</nobr>| Host name or IP address of where the SUT REST controller is listening on. *Default value*: `localhost`.|
|<nobr>`--sutControllerPort` &lt;Int&gt;</nobr>| TCP port of where the SUT REST controller is listening on. *Constraints*: `min=0.0, max=65535.0`. *Default value*: `40100`.|
|<nobr>`--tournamentSize` &lt;Int&gt;</nobr>| Number of elements to consider in a Tournament Selection (if any is used in the search algorithm). *Constraints*: `min=1.0`. *Default value*: `10`.|
|<nobr>`--useMethodReplacement` &lt;Boolean&gt;</nobr>| Apply method replacement heuristics to smooth the search landscape. *Default value*: `true`.|
|<nobr>`--writeExtraHeuristicsFile` &lt;Boolean&gt;</nobr>| Whether we should collect data on the extra heuristics. Only needed for experiments. *Default value*: `false`.|
|<nobr>`--writeStatistics` &lt;Boolean&gt;</nobr>| Whether or not writing statistics of the search process. This is only needed when running experiments with different parameter settings. *Default value*: `false`.|
|<nobr>`--xoverProbability` &lt;Double&gt;</nobr>| Probability of applying crossover operation (if any is used in the search algorithm). *Constraints*: `probability 0.0-1.0`. *Default value*: `0.7`.|

## Experimental Command-Line Options

|Options<img width=2000/>|Description|
|---|---|
|<nobr>`--S1dR` &lt;Double&gt;</nobr>| Specify a probability to apply S1dR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|<nobr>`--S1iR` &lt;Double&gt;</nobr>| Specify a probability to apply S1iR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|<nobr>`--S2dR` &lt;Double&gt;</nobr>| Specify a probability to apply S2dR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|<nobr>`--SMdR` &lt;Double&gt;</nobr>| Specify a probability to apply SMdR when resource sampling strategy is 'Customized'. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.25`.|
|<nobr>`--adaptivePerOfCandidateGenesToMutate` &lt;Boolean&gt;</nobr>| Specify whether to decide a top percent of genes to mutate adaptively. *Default value*: `false`.|
|<nobr>`--archiveGeneMutation` &lt;Enum&gt;</nobr>| Whether to enable archive-based gene mutation. *Valid values*: `NONE, SPECIFIED, ADAPTIVE`. *Default value*: `NONE`.|
|<nobr>`--coveredTargetFile` &lt;String&gt;</nobr>| Specify a file which saves covered targets info regarding generated test suite. *Default value*: `coveredTargets.txt`.|
|<nobr>`--coveredTargetSortedBy` &lt;Enum&gt;</nobr>| Specify a format to organize the covered targets by the search. *Valid values*: `NAME, TEST`. *Default value*: `NAME`.|
|<nobr>`--dependencyFile` &lt;String&gt;</nobr>| Specify a file that saves derived dependencies. *Default value*: `dependencies.csv`.|
|<nobr>`--disableStructureMutationDuringFocusSearch` &lt;Boolean&gt;</nobr>| Specify whether to disable structure mutation during focus search. *Default value*: `false`.|
|<nobr>`--doesApplyNameMatching` &lt;Boolean&gt;</nobr>| Whether to apply text/name analysis with natural language parser to derive relationships between name entities, e.g., a resource identifier with a name of table. *Default value*: `false`.|
|<nobr>`--enableCompleteObjects` &lt;Boolean&gt;</nobr>| Enable EvoMaster to generate, use, and attach complete objects to REST calls, rather than just the needed fields/values. *Default value*: `false`.|
|<nobr>`--enableProcessMonitor` &lt;Boolean&gt;</nobr>| Whether or not enable a search process monitor for archiving evaluated individuals and Archive regarding an evaluation of search. This is only needed when running experiments with different parameter settings. *Default value*: `false`.|
|<nobr>`--enableTrackEvaluatedIndividual` &lt;Boolean&gt;</nobr>| Whether to enable tracking the history of modifications of the individuals with its fitness values (i.e., evaluated individual) during the search. Note that we enforced that set enableTrackIndividual false when enableTrackEvaluatedIndividual is true since information of individual is part of evaluated individual. *Default value*: `false`.|
|<nobr>`--enableTrackIndividual` &lt;Boolean&gt;</nobr>| Whether to enable tracking the history of modifications of the individuals during the search. *Default value*: `false`.|
|<nobr>`--endPerOfCandidateGenesToMutate` &lt;Double&gt;</nobr>| Specify a percentage (after starting a focus search) which is used by archived-based gene selection method (e.g., APPROACH_IMPACT) for selecting top percent of genes as potential candidates to mutate. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.1`.|
|<nobr>`--expectationsActive` &lt;Boolean&gt;</nobr>| Enable Expectation Generation. If enabled, expectations will be generated. A variable called expectationsMasterSwitch is added to the test suite, with a default value of false. If set to true, an expectation that fails will cause the test case containing it to fail. *Default value*: `false`.|
|<nobr>`--exportCoveredTarget` &lt;Boolean&gt;</nobr>| Specify whether to export covered targets info. *Default value*: `false`.|
|<nobr>`--exportDependencies` &lt;Boolean&gt;</nobr>| Specify whether to export derived dependencies among resources. *Default value*: `false`.|
|<nobr>`--exportImpacts` &lt;Boolean&gt;</nobr>| Specify whether to export derived impacts among genes. *Default value*: `false`.|
|<nobr>`--geneSelectionMethod` &lt;Enum&gt;</nobr>| Specify whether to enable archive-based selection for selecting genes to mutate. *Valid values*: `NONE, AWAY_NOIMPACT, APPROACH_IMPACT, APPROACH_LATEST_IMPACT, APPROACH_LATEST_IMPROVEMENT, BALANCE_IMPACT_NOIMPACT, ALL_FIXED_RAND`. *Default value*: `NONE`.|
|<nobr>`--generateSqlDataWithDSE` &lt;Boolean&gt;</nobr>| Enable EvoMaster to generate SQL data with direct accesses to the database. Use Dynamic Symbolic Execution. *Default value*: `false`.|
|<nobr>`--impactFile` &lt;String&gt;</nobr>| Specify a file that saves derived genes. *Default value*: `impact.csv`.|
|<nobr>`--maxLengthOfTraces` &lt;Int&gt;</nobr>| Specify a maxLength of tracking when enableTrackIndividual or enableTrackEvaluatedIndividual is true. Note that the value should be specified with a non-negative number or -1 (for tracking all history). *Constraints*: `min=-1.0`. *Default value*: `10`.|
|<nobr>`--minRowOfTable` &lt;Int&gt;</nobr>| Specify a minimal number of rows in a table that enables selection (i.e., SELECT sql) to prepare resources for REST Action. In other word, if the number is less than the specified, insertion is always applied. *Constraints*: `min=0.0`. *Default value*: `10`.|
|<nobr>`--probOfApplySQLActionToCreateResources` &lt;Double&gt;</nobr>| Specify a probability to apply SQL actions for preparing resources for REST Action. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|<nobr>`--probOfArchiveMutation` &lt;Double&gt;</nobr>| Specify a probability to enable archive-based mutation. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|<nobr>`--probOfEnablingResourceDependencyHeuristics` &lt;Double&gt;</nobr>| Specify whether to enable resource dependency heuristics, i.e, probOfEnablingResourceDependencyHeuristics > 0.0. Note that the option is available to be enabled only if resource-based smart sampling is enable. This option has an effect on sampling multiple resources and mutating a structure of an individual. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.0`.|
|<nobr>`--probOfSelectFromDatabase` &lt;Double&gt;</nobr>| Specify a probability that enables selection (i.e., SELECT sql) of data from database instead of insertion (i.e., INSERT sql) for preparing resources for REST actions. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.1`.|
|<nobr>`--processFiles` &lt;String&gt;</nobr>| Specify a folder to save results when a search monitor is enabled. *Default value*: `process_data`.|
|<nobr>`--processInterval` &lt;Int&gt;</nobr>| Specify how often to save results when a search monitor is enabled. *Default value*: `100`.|
|<nobr>`--resourceSampleStrategy` &lt;Enum&gt;</nobr>| Specify whether to enable resource-based strategy to sample an individual during search. Note that resource-based sampling is only applicable for REST problem with MIO algorithm. *Valid values*: `NONE, Customized, EqualProbability, Actions, TimeBudgets, Archive, ConArchive`. *Default value*: `NONE`.|
|<nobr>`--startPerOfCandidateGenesToMutate` &lt;Double&gt;</nobr>| Specify a percentage (before starting a focus search) which is used by archived-based gene selection method (e.g., APPROACH_IMPACT) for selecting top percent of genes as potential candidates to mutate. *Constraints*: `probability 0.0-1.0`. *Default value*: `0.9`.|
|<nobr>`--testSuiteSplitType` &lt;Enum&gt;</nobr>| Instead of generating a single test file, it could be split in several files, according to different strategies. *Valid values*: `NONE`. *Default value*: `NONE`.|
