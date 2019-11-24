# Main Command-Line Options

The most important command-line options are:

* `--maxTime <String>`        
Maximum allowed time for the search, in the form `?h?m?s`, where it can be specified for how many hours (`h`),
minutes (`m`) and seconds (`s`) to run the search.
For example, `1h10m120s` would run the search for `72` minutes. 
Each component (i.e., `h`, `m` and `s`) is optional, but at least one must be specified.
In other words, if you need to run the search for just `30` seconds, you can write `30s` instead of `0h0m30s`.
**The more time is allowed, the better results one can expect**.
But then of course the test generation will take longer.

* `--outputFolder <String>`   
The path directory of where the generated test classes 
should be saved to.
 

* `--testSuiteFileName <String>`             
The name of generated file with the test cases, 
without file type   extension. 
In JVM languages, if the name contains `.`, 
folders will be created to represent the given
package structure.
