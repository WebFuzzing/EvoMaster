from numbers import Number
from typing import Any

from evomaster_client.instrumentation.execution_tracer import ExecutionTracer
from evomaster_client.instrumentation.heuristic.truthness import Truthness, eq_truthness_number, eq_truthness_str, \
                                                                 lt_truthness_number, lt_truthness_str

VALID_OPS = ['==', '!=', '<', '<=', '>', '>=']

LAST_EVALUATION: Truthness = None  # to handle negations


def clear_last_evaluation():
    LAST_EVALUATION = None


def evaluate(left, op, right, module, line, branch):
    if op == "==":
        res = left == right
    elif op == "!=":
        res = left != right
    elif op == "<":
        res = left < right
    elif op == "<=":
        res = left <= right
    elif op == ">":
        res = left > right
    elif op == ">=":
        res = left >= right
    elif op == "is":
        res = left is right
    elif op == "is not":
        res = left is not right
    elif op == "in":
        res = left in right
    elif op == "not in":
        res = left not in right
    truthness = compare(left, op, right)
    ExecutionTracer().update_branch(module, line, branch, truthness)
    LAST_EVALUATION = truthness
    return res


def compare(left, op, right):
    if op not in VALID_OPS:
        raise ValueError(f"Invalid op: {op}")

    left_is_number = isinstance(left, Number)
    left_is_str = isinstance(left, str)

    right_is_number = isinstance(right, Number)
    right_is_str = isinstance(right, str)

    both_number = left_is_number and right_is_number
    both_str = left_is_str and right_is_str

    number_and_str = (left_is_str and right_is_number) or (left_is_number and right_is_str)

    if op == '==':
        if both_number:
            h = eq_truthness_number(left, right)
        elif both_str:
            h = eq_truthness_str(left, right)
        else:
            b = left == right
            h = Truthness(int(b), int(not b))
    elif op == "!=":
        h = compare(left, "==", right).invert()
    elif op == "<":
        if both_number:
            h = lt_truthness_number(left, right)
        elif both_str or number_and_str:
            h = lt_truthness_str(str(left), str(right))
        else:
            b = left < right
            h = Truthness(int(b), int(not b))
    elif op == ">=":
        h = compare(left, "<", right).invert()
    elif op == "<=":
        # (l <= r)  same as  (r >= l)  same as  !(r < l)
        h = compare(right, "<", left).invert()
    elif op == ">":
        h = compare(left, "<=", right).invert()
    return h


def handle_not(value: Any) -> Any:
    if LAST_EVALUATION is not None:
        LAST_EVALUATION.invert()
    return not value


def evaluate_and(left, right, right_pure, module, line, branch):
    pass


def evaluate_or(left, right, right_pure, module, line, branch):
    pass
