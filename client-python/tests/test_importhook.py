import sys
import warnings
from importlib import import_module, reload
from importlib.util import cache_from_source
from pathlib import Path

import pytest

from evomaster_client.instrumentation.import_hook import install_import_hook
from evomaster_client.instrumentation.execution_tracer import ExecutionTracer

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
        with install_import_hook(['dummymodule'], tracer=ExecutionTracer()):
            module = import_module('dummymodule')
            return module
    finally:
        sys.path.remove(str(this_dir))

def test_import_hook(dummymodule):
    dummymodule.dummy_print('test_import_hook SUCCESS')
