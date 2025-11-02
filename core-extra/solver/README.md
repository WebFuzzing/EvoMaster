# Solver Module

The Solver module is dedicated to handling constraint solving with a focus on utilizing the [SMT-LIB](https://smtlib.cs.uiowa.edu/) syntax.

## Usage 

This module plays a crucial role in ensuring accurate and efficient constraint solving during the test setup phase. In particular: generating meaningful values to insert in the database setup during testing, ensuring that the queries return meaningful values.

## Overview

The Solver module's primary function is to solve constraints expressed in SMT-LIB format. Currently, it uses the [Z3](https://github.com/Z3Prover/z3), a powerful SMT solver developed by Microsoft Research. The design of the module allows for flexibility, enabling the integration of other solvers if needed.

## Key Features

- **SMT-LIB Syntax Modeling**: The module provides an abstraction that models constraints using the `SMT-LIB` format, leveraging the standardized language for specifying constraints.
- **Z3 Solver Integration**: The module utilizes the `Z3 solver` to handle the constraint solving process. The integration ensures efficient and accurate solving of constraints.
- **Extensible Solver Integration**: While Z3 is the current solver, the module's architecture supports the potential replacement or addition of other solvers, whether internal or external.

## Packages

### `org.evomaster.solver.smtlib`

This package contains the entire SMT-LIB abstraction to create SMT-LIB entities. It has a recursive mechanism for creating SMT-LIB structures, which allows for flexible and powerful constraint modeling. Usage examples in [tests](https://github.com/WebFuzzing/EvoMaster/blob/master/solver/src/test/java/org/evomaster/solver/SMTLibTest.java#L21).

#### SMTLib

The `SMTLib` module provides an abstraction for constructing and managing SMT-LIB files. It is designed to work with the Z3 solver by representing various SMT-LIB constructs in a structured manner. The key components are:

- **`SMTLib`**: This class manages a collection of `SMTNode` objects. It provides methods to add and retrieve nodes, and it produces the final SMT-LIB file as a string.

- **`SMTNode`**: This is an abstract base class for different types of SMT nodes. Each specific node type represents a different kind of SMT-LIB construct.

    - **`CheckSatSMTNode`**: Represents the `check-sat` command in SMT-LIB, used to query the satisfiability of the constraints.
    - **`DeclareConstSMTNode`**: Represents the `declare-const` command, used to declare constants in the SMT-LIB file.
    - **`DeclareDatatypeSMTNode`**: Represents the `declare-datatype` command, used to define new data types.
    - **`GetValueSMTNode`**: Represents the `get-value` command, used to retrieve the values of expressions.
    - **`AssertSMTNode`**: Represents an assertion in the SMT-LIB file. It contains a single `Assertion`.

- **`Assertion`**: An abstract concept representing various types of assertions in SMT-LIB:
    - **`AndAssertion`**: Represents a logical AND of multiple assertions.
    - **`DistinctAssertion`**: Represents an assertion that ensures values are distinct.
    - **`EqualsAssertion`**: Represents an equality assertion between two expressions.
    - **`GreaterThanAssertion`**: Represents a "greater than" assertion.
    - **`GreaterThanOrEqualsAssertion`**: Represents a "greater than or equal to" assertion.
    - **`LessThanAssertion`**: Represents a "less than" assertion.
    - **`LessThanOrEqualsAssertion`**: Represents a "less than or equal to" assertion.
    - **`OrAssertion`**: Represents a logical OR of multiple assertions.

#### SMTResultParser

The `SMTResultParser` class is responsible for interpreting the output from the Z3 solver. It takes a string response from Z3 and converts it into a `Map<String, SMTLibValue>`.

- **`SMTLibValue`**: An abstract base class for values.
    - **`IntValue`**: Represents integer values.
    - **`RealValue`**: Represents real (floating-point) numbers.
    - **`StringValue`**: Represents string literals.
    - **`StructValue`**: Represents complex structures with multiple fields.

The `parseZ3Response` method parses the raw solver response to produce a structured map where each key is a variable name and the value is an instance of `SMTLibValue`.

### `org.evomaster.solver.Z3DockerExecutor`

This class is responsible for executing the Z3 solver within a Docker container. It manages the lifecycle of the Docker container, ensures the solver is executed correctly, and parses the results.

1. Initializes a Docker container with the Z3 solver.
2. Binds a host directory to a directory in the container to facilitate the transfer of SMT-LIB files.
3. Executes the Z3 solver on SMT-LIB files located in the bound directory.
4. Parses the output from the Z3 solver and returns the results.

The class implements the `AutoCloseable` interface to ensure that the Docker container is properly stopped and resources are released when no longer needed.

## Future Enhancements

The Solver module is designed with extensibility in mind, enabling the addition of support for other constraint languages and solvers to broaden its applicability.

## Getting Started

To get started with the Solver module, ensure that you have Docker installed. The module automatically downloads the necessary Z3 image and runs it in a Docker container.
