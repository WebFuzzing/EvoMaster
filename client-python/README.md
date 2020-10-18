# EvoMaster Client for Python

Environment:
```
pyenv virtualenv venv-evomaster
pyenv activate venv-evomaster
pip install -r requirements-dev.txt
```

Running the instrumented version of EvoMaster Proxy controller:

```python
python -m evomaster_client.cli run-em -p 'evomaster_client.proxy' -m 'evomaster_client.proxy.em_app'
python -m evomaster_client.cli run-em -p 'evomaster_benchmark.ncs' -m 'evomaster_benchmark.ncs.app'
```

```python
python -m evomaster_client.cli run-instrumented -p 'evomaster_client.proxy' -m 'evomaster_client.proxy.em_app'
python -m evomaster_client.cli run-instrumented -p 'evomaster_benchmark.ncs' -m 'evomaster_benchmark.ncs.app'
```

Running tests:

```python
pytest [-s] tests
```
