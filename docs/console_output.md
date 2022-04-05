# Console Output


While running, _EvoMaster_ will output different info on the console logs, like how much time is remaining before completing generating the tests. 
It will also output other info, like in the following example.

```
* Going to save 1 test to src/em
* Evaluated tests: 11
* Evaluated actions: 83
* Needed budget: 53%
* Passed time (seconds): 33
* Execution time per test (ms): Avg=3013,27 , min=723,00 , max=4992,00
* Computation overhead between tests (ms): Avg=1,70 , min=1,00 , max=5,00
```

Here, at the end of the search, it will tell where the test cases are generated (and how many).
During the search, several tests could be evaluated (`Evaluated tests`), and only the ones that contribute to coverage are going to be part of the final test suite.
Furthermore, a `test` could be composed of 1 or more `actions` (e.g., HTTP calls when testing REST APIs).
This implies `Evaluated actions >= Evaluated tests`.

For how long should the search be left running? 
The info `Needed budget` helps with this question, as it keeps track of the last time there was an improvement in the search (e.g., a new line is covered, or a new fault is discovered).
If the `Search budget` (i.e., for how long the search is left running) is 100 minutes,
and the `Needed budget` is 53%, then it means that the last improvement was achieved after 53 minutes, with the last 47 minutes
of the search not finding any useful new test data.
High values (e.g., `>= 90%`) likely mean that, if you leave _EvoMaster_ running for longer, likely you are going to get better results.
But what values should you aim for `Needed budget`?
Hard to say for sure, as it depends on the tested API and how/where/when you run _EvoMaster_ (e.g., if using a dedicated CI machine 
running long fuzzing jobs during the night, then you might not care much about `Needed budget`).
But, if you run _EvoMaster_ for at least 1 hour, and `Needed budget` is _lower_ than 50% (e.g., 42%), then it is unlikely that you will get significantly
better results by increasing the search budget. 

Finally, for statistics, we also report how long it takes to evaluate each test case (`Execution time per test`), and what is the computation overhead between 2 test evaluations (`Computation overhead between tests`).
This latter is keeping track of how much _EvoMaster_ needs to run its algorithms and heuristics before deciding of evaluating a new test. 
