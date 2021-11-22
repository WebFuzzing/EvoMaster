from typing import Any

from evomaster_client.instrumentation.execution_tracer import ExecutionTracer
from evomaster_client.instrumentation.heuristic import heuristics


def entering_statement(module: str, line: int, statement: int):
    ExecutionTracer().entering_statement(module, line, statement)
    print(f"entering statement: {module}-{line}-{statement}")
    return


def completed_statement(module: str, line: int, statement: int):
    ExecutionTracer().completed_statement(module, line, statement)
    print(f"completed statement: {module}-{line}-{statement}")
    return


def completion_statement(module: str, line: int, statement: int):
    entering_statement(module, line, statement)
    completed_statement(module, line, statement)


def completing_statement(value: Any, module: str, line: int, statement: int):
    completed_statement(module, line, statement)
    return value


def compare_statement(left, op, right, module: str, line: int, branch: int):
    return heuristics.evaluate(left, op, right, module, line, branch)


def not_statement(value: Any) -> Any:
    return heuristics.handle_not(value)


def and_statement(left: Any, right: Any, right_pure: bool, module: str, line: int, branch: int) -> Any:
    return heuristics.evaluate_and(left, right, right_pure, module, line, branch)


def or_statement(left: Any, right: Any, right_pure: bool, module: str, line: int, branch: int) -> Any:
    return heuristics.evaluate_or(left, right, right_pure, module, line, branch)
