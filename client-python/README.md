# EvoMaster Client for Python

Environment:
```
pyenv virtualenv evomaster
pyenv activate evomaster
pip install -r requirements-dev.txt
```

Running the instrumented version of EvoMaster Proxy controller:

```python
python -m evomaster_client.cli run-instrumented -p 'evomaster_client.proxy' -m 'evomaster_client.proxy.em_app'
python -m evomaster_client.cli run-instrumented -p 'evomaster_benchmark.ncs' -m 'evomaster_benchmark.ncs.app'
python -m evomaster_client.cli run-instrumented -p 'evomaster_benchmark.scs' -m 'evomaster_benchmark.scs.app'
```

Running EvoMaster benchmark applications
```python
python -m 'evomaster_benchmark.ncs.app'
python -m 'evomaster_benchmark.scs.app'
python -m 'evomaster_benchmark.news.app'
```

Generating black-box tests
```
java -jar ../core/target/evomaster.jar --maxTime 60s  --outputFolder tests/generated/APP --outputFormat PYTHON_UNITTEST --blackBox true --bbTargetUrl http://localhost:8080/ --bbSwaggerUrl http://localhost:8080/swagger.json
```

Running EvoMaster benchmark handlers
```python
python -m evomaster_client.cli run-em-handler -m 'evomaster_benchmark.em_handlers.ncs' -c 'EMHandler'
python -m evomaster_client.cli run-em-handler -m 'evomaster_benchmark.em_handlers.scs' -c 'EMHandler'
python -m evomaster_client.cli run-em-handler -m 'evomaster_benchmark.em_handlers.news' -c 'EMHandler'
```

Generating white-box tests
```
java -jar ../core/target/evomaster.jar --maxTime 60s  --outputFolder tests/generated/APP --outputFormat PYTHON_UNITTEST
```

Running generated tests
```
python -m pytest tests/generated/path/to/test
```
