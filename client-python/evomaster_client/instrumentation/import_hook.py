import ast
import sys
from importlib.machinery import SourceFileLoader
from importlib.abc import MetaPathFinder
from importlib.util import decode_source
from inspect import isclass
from pathlib import Path
from typing import List

import astor

from evomaster_client.instrumentation.ast_transformer import AstTransformer
from evomaster_client.instrumentation.execution_tracer import ExecutionTracer


class InstrumentationFinder(MetaPathFinder):
    """
    A meta path finder which wraps another pathfinder.
    It receives all import requests and intercepts the ones for the modules that
    should be instrumented.
    """

    def __init__(self, original_pathfinder, package_prefixes, tracer):
        """Wraps the given path finder.
        Args:
            original_pathfinder: the original pathfinder that is wrapped.
            module_to_instrument: the name of the module, that should be instrumented.
            tracer: the execution tracer
        """
        self.package_prefixes = package_prefixes
        self._original_pathfinder = original_pathfinder
        self.tracer = tracer

    def find_spec(self, fullname, path=None, target=None):
        """Try to find a spec for the given module.

        If the original path finder accepts the request, we take the spec and replace
        the loader.

        Args:
            fullname: The full name of the module
            path: The path
            target: The target
        Returns:
            An optional ModuleSpec
        """
        if self.should_instrument(fullname):
            spec = self._original_pathfinder.find_spec(fullname, path, target)
            if spec is not None and isinstance(spec.loader, SourceFileLoader):
                spec.loader = InstrumentationLoader(spec.loader.name, spec.loader.path, self.tracer)
                cached = Path(spec.cached)
                if cached.exists():
                    cached.unlink()
                return spec
        return None

    def should_instrument(self, module_name: str) -> bool:
        """
        Determine whether the module with the given name should be instrumented.
        :param module_name: full name of the module that is about to be imported (e.g. ``xyz.abc``)
        """
        return any(module_name.startswith(prefix) for prefix in self.package_prefixes)


class InstrumentationLoader(SourceFileLoader):
    """A loader that instruments the module after execution."""

    def __init__(self, fullname, path, tracer: ExecutionTracer):
        super().__init__(fullname, path)
        self._tracer = tracer

    def exec_module(self, module):
        # self._tracer.reset()
        super().exec_module(module)
        # self._tracer.store_import_trace()

    def source_to_code(self, data, path, *, _optimize=-1):
        source = decode_source(data)
        tree = ast.parse(source, filename=path)
        # tree = _call_with_frames_removed(compile, source, path, 'exec', ast.PyCF_ONLY_AST,
        #                                  dont_inherit=True, optimize=_optimize)
        tree = AstTransformer(module=path).visit(tree)
        ast.fix_missing_locations(tree)
        # return _call_with_frames_removed(compile, tree, path, 'exec',
        #                                  dont_inherit=True, optimize=_optimize)
        print (path)
        print(astor.to_source(tree))
        return compile(tree, path, 'exec')

class ImportHookContextManager:
    """A simple context manager for using the import hook."""

    def __init__(self, hook: MetaPathFinder):
        self.hook = hook

    def __enter__(self):
        pass

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.uninstall()

    def uninstall(self):
        """Remove the installed hook."""
        try:
            sys.meta_path.remove(self.hook)
        except ValueError:
            pass  # already removed


def install_import_hook(package_prefixes: List[str], tracer: ExecutionTracer) -> ImportHookContextManager:
    """Install the InstrumentationFinder in the meta path.
    Args:
        module_to_instrument: The module that shall be instrumented.
        tracer: The tracer where the instrumentation should report its data.
    Returns:
        a context manager which can be used to uninstall the hook.
    Raises:
        RuntimeError: In case a PathFinder could not be found
    """
    to_wrap = None
    for finder in sys.meta_path:
        if (
            isclass(finder)
            and finder.__name__ == "PathFinder"  # type: ignore
            and hasattr(finder, "find_spec")
        ):
            to_wrap = finder
            break

    if not to_wrap:
        raise RuntimeError("Cannot find a PathFinder in sys.meta_path")

    hook = InstrumentationFinder(to_wrap, package_prefixes, tracer)
    sys.meta_path.insert(0, hook)
    return ImportHookContextManager(hook)
