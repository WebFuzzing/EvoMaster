from numbers import Number
from typing import Any, Callable

from evomaster_client.instrumentation.execution_tracer import ExecutionTracer
from evomaster_client.instrumentation.heuristic.truthness import Truthness, eq_truthness_number, eq_truthness_str, \
                                                                 lt_truthness_number, lt_truthness_str

VALID_OPS = ['==', '!=', '<', '<=', '>', '>=']

LAST_EVALUATION: Truthness = None  # to handle negations

FLAG_NO_EXCEPTION = 0.01
FLAG_EXCEPTION = FLAG_NO_EXCEPTION / 2


def clear_last_evaluation():
    global LAST_EVALUATION
    LAST_EVALUATION = None


def evaluate(left, op, right, module, line, branch):
    global LAST_EVALUATION
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
    global LAST_EVALUATION
    if LAST_EVALUATION is not None:
        LAST_EVALUATION.invert()
    return not value


def evaluate_and(left: Callable, right: Callable, right_pure: bool, module: str, line: int, branch: int):
    global LAST_EVALUATION
    clear_last_evaluation()
    try:
        exception_x = None
        x = left()
        truthness_x = LAST_EVALUATION
        if not truthness_x:
            truthness_x = Truthness(1 if x else FLAG_NO_EXCEPTION, FLAG_NO_EXCEPTION if x else 1)
        else:
            truthness_x = truthness_x.rescale_from_min(FLAG_NO_EXCEPTION)
    except Exception as e:
        truthness_x = Truthness(FLAG_EXCEPTION, FLAG_EXCEPTION)
        exception_x = e

    left_is_true = x and exception_x is None

    if left_is_true or right_pure:
        clear_last_evaluation()
        try:
            exception_y = None
            y = right()
            truthness_y = LAST_EVALUATION
            if not truthness_y:
                truthness_y = Truthness(1 if y else FLAG_NO_EXCEPTION, FLAG_NO_EXCEPTION if y else 1)
            else:
                truthness_y = truthness_y.rescale_from_min(FLAG_NO_EXCEPTION)
        except Exception as e:
            truthness_y = Truthness(FLAG_EXCEPTION, FLAG_EXCEPTION)
            exception_y = e
        truthness = Truthness(truthness_x.ofTrue / 2 + truthness_y.ofTrue / 2,
                              max(truthness_x.ofFalse, (truthness_y.ofFalse / 2 if exception_x is not None else truthness_y.ofFalse)))
    else:
        truthness = Truthness(truthness_x.ofTrue / 2, truthness_x.ofFalse)

    ExecutionTracer().update_branch(module, line, branch, truthness)
    LAST_EVALUATION = truthness

    if exception_x:
        raise exception_x
    elif left_is_true and exception_y:
        raise exception_y
    else:
        return x and y


def evaluate_or(left: Callable, right: Callable, right_pure: bool, module: str, line: int, branch: int):
    global LAST_EVALUATION
    clear_last_evaluation()
    try:
        exception_x = None
        x = left()
        truthness_x = LAST_EVALUATION
        if not truthness_x:
            truthness_x = Truthness(1 if x else FLAG_NO_EXCEPTION, FLAG_NO_EXCEPTION if x else 1)
        else:
            truthness_x = truthness_x.rescale_from_min(FLAG_NO_EXCEPTION)
    except Exception as e:
        truthness_x = Truthness(FLAG_EXCEPTION, FLAG_EXCEPTION)
        exception_x = e

    left_is_false = (not x) and exception_x is None

    if left_is_false or right_pure:
        clear_last_evaluation()
        try:
            exception_y = None
            y = right()
            truthness_y = LAST_EVALUATION
            if not truthness_y:
                truthness_y = Truthness(1 if y else FLAG_NO_EXCEPTION, FLAG_NO_EXCEPTION if y else 1)
            else:
                truthness_y = truthness_y.rescale_from_min(FLAG_NO_EXCEPTION)
        except Exception as e:
            truthness_y = Truthness(FLAG_EXCEPTION, FLAG_EXCEPTION)
            exception_y = e
        truthness = Truthness(max(truthness_x.ofTrue, (truthness_y.ofTrue / 2 if exception_x is not None else truthness_y.ofTrue)),
                              truthness_x.ofFalse / 2 + truthness_y.ofFalse / 2)
    else:
        truthness = Truthness(truthness_x.ofTrue, truthness_x.ofFalse / 2)

    ExecutionTracer().update_branch(module, line, branch, truthness)
    LAST_EVALUATION = truthness

    if exception_x:
        raise exception_x
    elif left_is_false and exception_y:
        raise exception_y
    else:
        return x or y
