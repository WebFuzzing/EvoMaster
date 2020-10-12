from typing import Any

from evomaster_client.instrumentation.execution_tracer import ExecutionTracer


def entering_statement(module: str, line: int, statement: int):
    ExecutionTracer().entering_statement(module, line, statement)
    print(f"entering statement: {module}-{line}-{statement}")
    return


def completed_statement(module: str, line: int, statement: int):
    ExecutionTracer().completed_statement(module, line, statement)
    print(f"completed statement: {module}-{line}-{statement}")
    return


def completing_statement(value: Any, module: str, line: int, statement: int):
    completed_statement(module, line, statement)
    return value
