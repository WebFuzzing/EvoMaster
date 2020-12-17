# Resource-based MIO

We report experiment configurations used in the paper *Resource and Dependency based Test Case Generation for RESTful Web Services*.

## Steps
1. [build the evomaster](#Build)
2. [start a SUT with a driver](#Case-Studies)
3. [run evomaster](#Execute) with [specified options](#Techniques)


## Build
Build the evomaster from the source code with `mvn  clean install -DskipTests`.

*see more details [here](../build.md).*

## Case Studies

The employed case studies include [7 open source](https://github.com/EMResearch/EMB) and [12 synthetic](https://github.com/EMResearch/artificial-rest-api) REST APIs.

For each of the case study, you can find a driver named `EmbeddedEvoMasterController.java`.
You can start the driver with a specified port, e.g., 40100.

*see more details to write a drive for enabling white-box testing [here](../write_driver.md).*

## Techniques

In the paper, we conducted the experiment with the following techniques:
* __Base1__: `--probOfSmartSampling 0.0 --resourceSampleStrategy NONE --probOfEnablingResourceDependencyHeuristics 0.0 --doesApplyNameMatching false --probOfArchiveMutation 0.0 --baseTaintAnalysisProbability 0.0 --enableTrackEvaluatedIndividual false --weightBasedMutationRate false`
* __Base2__: `--probOfSmartSampling 0.5 --resourceSampleStrategy NONE --probOfEnablingResourceDependencyHeuristics 0.0 --doesApplyNameMatching false --probOfArchiveMutation 0.0 --baseTaintAnalysisProbability 0.0 --enableTrackEvaluatedIndividual false --weightBasedMutationRate false`
* __R-MIO__: `--probOfSmartSampling 0.5 --resourceSampleStrategy ConArchive --probOfEnablingResourceDependencyHeuristics 0.0 --doesApplyNameMatching false --probOfArchiveMutation 0.0 --baseTaintAnalysisProbability 0.0 --enableTrackEvaluatedIndividual false --weightBasedMutationRate false`
* __Rd-MIO__: `--probOfSmartSampling 1.0 --resourceSampleStrategy ConArchive --probOfEnablingResourceDependencyHeuristics 1.0 --doesApplyNameMatching true --probOfArchiveMutation 0.0 --baseTaintAnalysisProbability 0.0 --enableTrackEvaluatedIndividual false --weightBasedMutationRate false`

For all of the techniques, we employ a budget 100k 
`--maxActionEvaluations 100000 --stoppingCriterion FITNESS_EVALUATIONS`

To interact with a driver, you need to specify a port used when starting the driver, e.g.,
`--sutControllerPort 40100`

*see more options [here](../options.md).*

## Execute

To apply a technique to generate tests, you can run the evomaster with the corresponding configuration,
e.g., apply __Rd-MIO__

`java -jar evomaster.jar --maxActionEvaluations 100000 --stoppingCriterion FITNESS_EVALUATIONS --sutControllerPort 40100 --probOfSmartSampling 1.0 --resourceSampleStrategy ConArchive --probOfEnablingResourceDependencyHeuristics 1.0 --doesApplyNameMatching true --probOfArchiveMutation 0.0 --baseTaintAnalysisProbability 0.0 --enableTrackEvaluatedIndividual false --weightBasedMutationRate false`

