# Replicating Studies

To foster the progress of scientific research, _replicability_ of studies is very important.
At least in the software testing community, now quite  a few venues __require__ the presence of _replication packages_ to get papers accepted.

To help replicating previous studies for _EvoMaster_, we provide most of the [scripts](exp) used throughout the years in our studies.
Here, we give a high overview of how they can be used. 
To see which script was used in each study,
you can look at the [list of publications](publications.md), where for each paper we provide links to the used scripts. 


First and foremost, we need to make an important disclaimer here.
None of our previous experiments can be replicated with 100% accuracy.
The reason is that _EvoMaster_ deals with networking, which is an unfortunate source of non-determinism (e.g., when idempotent HTTP calls get repeated sporadically).
Furthermore, some of the internal heuristics of our algorithms are time-based: unless repeating the experiments on exactly the same hardware in the same exact conditions, 100% accurate replication of the results cannot be achieved.
However, lot of care has been taken in trying to keep under-control all source of non-determinism in _EvoMaster_.
This means that, given enough repeated experiments with different initializing seeds, average results (e.g., code coverage) should be very similar (although unlikely being 100% the same). 

Another important fact to keep in mind is that _EvoMaster_ is under continuous development, with new releases coming out each year.
However, the release process is unrelated to the published scientific papers, and we have _NOT_ been tagging versions on Git to trace the exact version of _EvoMaster_ used in any specific paper (as that would be a non-negligible time overhead).
As anyway having a 100% accurate replication of the experiments is not possible, we simply suggest  using a recent version of _EvoMaster_ when one wants to re-investigate some of our previous research questions (as most experiment settings will be backward compatible).


## Experiment Scripts

When running experiments for _EvoMaster_, we usually employ the case studies in 
[EMB](https://github.com/EMResearch/EMB), plus some other systems provided by our industrial partners (which of course we cannot provide online).
In each study, we might investigate different settings of _EvoMaster_, and each experiment has to be repeated several times (typically 30) with different random seeds.
Each experiment requires not only to start the process of _EvoMaster's core_, but also the _driver_ process for the tested applications. 
This means that, often, the experiment settings for a scientific study is quite complex.
To help running these experiments, we usually employ a script (written in Python) to configure all these settings.

This script has been evolving throughout the years, to simplify the running of the experiments, from paper to paper, with as little effort as possible. 
A template for one of its most recent versions can be found [in the _scripts_ folder (i.e., _scritps/exp.py_)](../scripts/exp.py).
Here, we briefly discuss how it works. 

Once defined a set of configurations of _EvoMaster_ to experiment with, the _exp.py_ script will generate a set of Bash scripts, in which the experiments will be distributed.
The Python script will take few parameters, like for example:

```
exp.py <cluster> <baseSeed> <dir> <minSeed> <maxSeed> <maxActions> <minutesPerRun> <nJobs>
```

The version of the scripts used in our recent papers do have a description (as comments) for each of these parameters.
But let us briefly summarize them here:

* _cluster_: whether experiments are meant to run on a local machine, or a scientific cluster of computers (the specific settings are for one cluster which is available for researchers in Norway). Unless you have access to such cluster, or want to adapt the script for clusters you have access to, you will likely use __false__ for this parameter.

* _baseSeed_: the starting seed used for initialization of TCP ports (e.g., for the tested applications). Each experiment will use different ports, where _baseSeed_ is the minimum value. As this value is used to setup TCP ports, we recommend a value of at least 1024, and no more than 60 000. The idea is that all (most?) experiments could be run on a single machine, without TCP port clashes. 

* _dir_: where all Bash scripts will be created. _EvoMaster_ is then configured to output all of its data (e.g., the generated test files) into such folder. 

* _minSeed_: used to setup the `--seed` configuration in _EvoMaster_. Although we cannot control all source of non-determinism, we can at least control the seed for the random generator.

* _maxSeed_: the maximum seed used. Experiments will be run for all seeds between _minSeed_ and _maxSeed_ (inclusive). For example, to repeat the experiments 5 times with different seed, could use the configuration _minSeed=0_ and _maxSeed=4_.

* _maxActions_: the search budget, measured in number of actions. For REST APIs, this will be the max number of HTTP calls to execute. Note: to help replicability of the studies, in the experiments we usually do not use time as stopping criterion.

* _minutesPerRun_: this is only needed to setup when running experiments on cluster, to enforce timeouts on hanging processes.

* _nJobs_: the number of Bash scripts to generate. All experiments will be distributed among those scripts. In other words, each script will have one or more experiments, run in sequence. How many scripts to run in parallel depends on your machine specs (e.g., number of CPUs and RAM size).

Once the _exp.py_ script is run, a _runAll.sh_ script will be automatically generated, which helps with the starting of all the Bash scripts in parallel.  

However, _exp.py_ relies on several environment variables, e.g., to specify where different versions of the JDK, EMB and _EvoMaster_ are installed. 
The script will provide descriptive error messages when those variables are not set.


An usage of the script could be:

```
exp.py false 12345 foo 0 4 100000 1 10
```

This will generate 10 Bash scripts in the `foo` folder, with experiments repeated 5 times, having a search budget of 100k HTTP calls per experiment.
These 10 scripts can then be started in parallel with `foo/runAll.sh`.
Once all experiments are finished, the generated `statistics.csv` files can be collected to analyze the results. 


Generating _N_ bash scripts does not mean that all of them will take the exact amount of time.
Running all of them in parallel would hence be inefficient.
Assume you can run _K_ jobs in parallel, let's say 5, based on CPU cores and memory available.
Then, it makes sense to generate _N>K_ bash scripts, and then use the script `schedule.py` to run them, e.g., using:

```
schedule.py 5 foo
```

This will run all the experiments in `foo`, with _K=5_ jobs in parallel (out of the total _N=10_).
Each time a job ends, a new one is started.
Where the value of _K_ depends on available resources, _N_ can be arbitrary, as long as _N>=K_.
Can make sense to have something like _N=100_, or at least _N>3K_. 


















