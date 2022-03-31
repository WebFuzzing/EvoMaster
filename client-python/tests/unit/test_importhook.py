import sys
from importlib import import_module
from importlib.util import cache_from_source
from pathlib import Path

import pytest

from evomaster_client.instrumentation.import_hook import install_import_hook
from evomaster_client.instrumentation.staticstate.execution_tracer import ExecutionTracer

this_dir = Path(__file__).parent
dummy_module_path = this_dir / 'dummymodule.py'
cached_module_path = Path(cache_from_source(str(dummy_module_path)))


@pytest.fixture(scope='module')
def dummymodule():
    # Removes cached module
    if cached_module_path.exists():
        cached_module_path.unlink()

    sys.path.insert(0, str(this_dir))
    try:
        with install_import_hook(['dummymodule']):
            module = import_module('dummymodule')
            return module
    finally:
        sys.path.remove(str(this_dir))


def test_import_hook(dummymodule):
    objectives = ExecutionTracer().get_number_of_objectives()
    count = objectives
    assert count > 0

    print(dummymodule.dummy_print('test_import_hook SUCCESS'))
    objectives = ExecutionTracer().get_number_of_objectives()
    assert objectives > count
    count = objectives

    print(dummymodule.dummy_compare(1, 2))
    objectives = ExecutionTracer().get_number_of_objectives()
    assert objectives > count
    count = objectives

    print(f"dummy_truthness: {dummymodule.dummy_truthness(12)}")
    objectives = ExecutionTracer().get_number_of_objectives()
    assert objectives > count
    count = objectives

    print(dummymodule.dummy_call(1))
    objectives = ExecutionTracer().get_number_of_objectives()
    assert objectives > count
    count = objectives
