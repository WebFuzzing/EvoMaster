# Solver Module

This module handles the Constraint Solving. For the moment the support is for [SMT-LIB](https://smtlib.cs.uiowa.edu/) syntax.

And the constraint solving is being used for creating queries in the Database in the setup of the tests to insert data. In the future, this module could be expanded to more functionalities.

It currently uses [Z3](https://github.com/Z3Prover/z3) as solver, but it could be replaced for any other solver (internal or external). 