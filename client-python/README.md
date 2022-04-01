# EvoMaster Client for Python

## About EvoMaster:

[EvoMaster](https://github.com/EMResearch/EvoMaster) is the first open-source AI-driven tool for automatically generating system-level test cases (also known as fuzzing) for web/enterprise applications. Currently targeting whitebox and blackbox testing of REST APIs.

## Evomaster-Client for Python Flask

This library will let you integrate your Flask restful application to the EvoMaster core in order to automatically generate test cases for your API endpoints using the [unittest](https://docs.python.org/3/library/unittest.html) testing framework for Python.

## Quick Start

Installation:
```bash
pip3 install evomaster-client
```

After the installation, a CLI is available to be run from a Python environment to use the EvoMaster driver.

You can access the CLI help using:
```bash
python -m evomaster_client.cli -h
```

For now, the CLI supports two commands:
1. run-instrumented
2. run-em-handler

### Running the instrumented version of your application:
```bash
python -m evomaster_client.cli run-instrumented [OPTIONS]
```

For example:
```bash
python -m evomaster_client.cli run-instrumented --package-prefix 'evomaster_benchmark.ncs' --flask-module 'evomaster_benchmark.ncs.app' --flask-app 'app' --instrumentation-level 2
```
where:
- package-prefix is the prefix used to define the modules that will be instrumented
- flask-module is the main module of your app
- flask-module is the name of your Flask object defined in flask-module, used to start your app
- instrumentation-level can be [0..2] where a higher value means the code instrumentation is richer

### Running the EvoMaster handler to generate white-box tests for your application:
```bash
python -m evomaster_client.cli run-em-handler [OPTIONS]
```

For example:
```bash
python -m evomaster_client.cli run-em-handler --handler-module 'evomaster_benchmark.em_handlers.ncs' --handler-class 'EMHandler' --instrumentation-level 2
```
where:
- handler-module is the module where the driver class, extending from FlaskHandler, was implemented
- handler-class is name of the driver class defined in handler-module
- flask-module is the name of your Flask object defined in flask-module, used to start your app
- instrumentation-level can be [0..2] where a higher value means the code instrumentation is richer

Once you have the handler running, white-box tests can be generated using:
```bash
java -jar ../core/target/evomaster.jar --maxTime [time]  --outputFolder [path_to_tests] --outputFormat PYTHON_UNITTEST
```

Then you can run your generated tests using:
```
pip3 install unittest requests
python -m unittest [path_to_tests]
```
