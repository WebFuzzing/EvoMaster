import pytest

from evomaster_client.instrumentation.heuristic import heuristics


def test_compare_equal_numbers():
    t = heuristics.compare(42, '==', 42)
    assert t.ofTrue == 1
    assert t.ofFalse < 1


def test_compare_equal_strings():
    t = heuristics.compare('foo', '==', 'foo')
    assert t.ofTrue == 1
    assert t.ofFalse < 1


def test_compare_numbers_same_distance():
    a = heuristics.compare(42, '==', 42)
    b = heuristics.compare(-4, '==', -4)
    assert a.ofTrue == b.ofTrue
    assert a.ofFalse == b.ofFalse


def test_compare_numbers_gradient():
    a = heuristics.compare(10042, "==", 3)
    b = heuristics.compare(420, "==", 42)
    c = heuristics.compare(-5, "==", 10)
    d = heuristics.compare(2, "==", 3)

    assert a.ofFalse == 1
    assert b.ofFalse == 1
    assert c.ofFalse == 1
    assert d.ofFalse == 1

    assert a.ofTrue < b.ofTrue
    assert b.ofTrue < c.ofTrue
    assert c.ofTrue < d.ofTrue


def test_compare_strings_gradient():
    a = heuristics.compare("aaaaaaaaaaaaaaaaaaaaa", "==", "foo")
    b = heuristics.compare("", "==", "foo")
    c = heuristics.compare("a", "==", "foo")
    d = heuristics.compare("f", "==", "foo")
    e = heuristics.compare("fo", "==", "foo")

    assert a.ofFalse == 1
    assert b.ofFalse == 1
    assert c.ofFalse == 1
    assert d.ofFalse == 1
    assert e.ofFalse == 1

    assert a.ofTrue < b.ofTrue
    assert b.ofTrue < c.ofTrue
    assert c.ofTrue < d.ofTrue
    assert d.ofTrue < e.ofTrue


def test_compare_equal_not_handled_types():
    a = heuristics.compare('42', '==', [])
    b = heuristics.compare(42, '==', {})

    assert a.ofFalse == 1
    assert b.ofFalse == 1


def test_not_equal():
    t = heuristics.compare(42, '!=', 66)

    assert t.ofTrue == 1
    assert t.ofFalse < 1


def test_not_equal_numbers_same_distance():
    a = heuristics.compare(44, '!=', 42)
    b = heuristics.compare(-6, '!=', -4)

    assert a.ofTrue == b.ofTrue
    assert a.ofFalse == b.ofFalse


def test_compare_lt_numbers():
    t = heuristics.compare(42, '<', 66)

    assert t.ofTrue == 1
    assert t.ofFalse < 1


def test_compare_leq_numbers():
    a = heuristics.compare(42, '<', 66)
    b = heuristics.compare(66, '>=', 42)

    assert a.ofTrue == b.ofTrue
    assert a.ofFalse == b.ofFalse


def test_compare_lt_strings():
    t = heuristics.compare('aaa', '<', 'aab')
    assert t.ofTrue == 1
    assert t.ofFalse < 1


def test_or_constants():
    assert heuristics.evaluate_or(lambda: True, lambda: False, False, '', 0, 0)
    a = heuristics.LAST_EVALUATION

    assert heuristics.evaluate_or(lambda: True, lambda: True, False, '', 0, 0)
    b = heuristics.LAST_EVALUATION

    assert heuristics.evaluate_or(lambda: False, lambda: True, False, '', 0, 0)
    c = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_or(lambda: False, lambda: False, False, '', 0, 0)
    d = heuristics.LAST_EVALUATION

    assert a.ofTrue == 1
    assert a.ofTrue == b.ofTrue
    assert a.ofTrue == c.ofTrue
    assert d.ofTrue < 1

    assert a.ofFalse < 1
    assert a.ofFalse == b.ofFalse  # using not pure, otherwise would be smaller
    assert a.ofFalse < c.ofFalse  # using not pure
    assert d.ofFalse == 1


def test_or_constants_using_pure():
    assert heuristics.evaluate_or(lambda: True, lambda: False, True, '', 0, 0)
    a = heuristics.LAST_EVALUATION

    assert heuristics.evaluate_or(lambda: True, lambda: True, True, '', 0, 0)
    b = heuristics.LAST_EVALUATION

    assert heuristics.evaluate_or(lambda: False, lambda: True, True, '', 0, 0)
    c = heuristics.LAST_EVALUATION

    assert a.ofFalse < 1
    assert a.ofFalse > b.ofFalse
    assert a.ofFalse == c.ofFalse


def test_or_pure_functions():
    assert heuristics.evaluate_or(
        lambda: heuristics.compare('foo', '==', 'bar'),
        lambda: heuristics.compare(42, '==', 42),
        True, '', 0, 0)
    assert heuristics.LAST_EVALUATION.ofTrue == 1
    assert heuristics.LAST_EVALUATION.ofFalse < 1


def test_or_pure_functions_gradient_true():
    assert not heuristics.evaluate_or(
        lambda: heuristics.evaluate(100, '==', 110, '', 0, 0),
        lambda: heuristics.evaluate(0, '==', 10, '', 0, 0),
        True, '', 0, 0)
    a = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_or(
        lambda: heuristics.evaluate(100, '==', 110, '', 0, 0),
        lambda: heuristics.evaluate(5, '==', 10, '', 0, 0),
        True, '', 0, 0)
    b = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_or(
        lambda: heuristics.evaluate(102, '==', 110, '', 0, 0),
        lambda: heuristics.evaluate(5, '==', 10, '', 0, 0),
        True, '', 0, 0)
    c = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_or(
        lambda: heuristics.evaluate(107, '==', 110, '', 0, 0),
        lambda: heuristics.evaluate(5, '==', 10, '', 0, 0),
        True, '', 0, 0)
    d = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_or(
        lambda: heuristics.evaluate(107, '==', 110, '', 0, 0),
        lambda: heuristics.evaluate(9, '==', 10, '', 0, 0),
        True, '', 0, 0)
    e = heuristics.LAST_EVALUATION

    assert a.ofTrue < 1
    assert a.ofTrue < b.ofTrue
    assert b.ofTrue == c.ofTrue
    assert c.ofTrue < d.ofTrue
    assert d.ofTrue < e.ofTrue
    assert e.ofTrue < 1


def test_and_constants():
    assert not heuristics.evaluate_and(lambda: True, lambda: False, False, '', 0, 0)
    a = heuristics.LAST_EVALUATION

    assert heuristics.evaluate_and(lambda: True, lambda: True, False, '', 0, 0)
    b = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_and(lambda: False, lambda: True, False, '', 0, 0)
    c = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_and(lambda: False, lambda: False, False, '', 0, 0)
    d = heuristics.LAST_EVALUATION

    assert a.ofTrue < 1
    assert b.ofTrue == 1
    assert c.ofTrue < a.ofTrue  # because not pure
    assert d.ofTrue < a.ofTrue
    assert c.ofTrue == d.ofTrue  # because not pure

    assert a.ofFalse == 1
    assert b.ofFalse < 1
    assert c.ofFalse == 1
    assert d.ofFalse == 1


def test_and_constants_pure():
    assert not heuristics.evaluate_and(lambda: True, lambda: False, True, '', 0, 0)
    a = heuristics.LAST_EVALUATION

    assert heuristics.evaluate_and(lambda: True, lambda: True, True, '', 0, 0)
    b = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_and(lambda: False, lambda: True, True, '', 0, 0)
    c = heuristics.LAST_EVALUATION

    assert not heuristics.evaluate_and(lambda: False, lambda: False, True, '', 0, 0)
    d = heuristics.LAST_EVALUATION

    assert a.ofTrue < 1
    assert b.ofTrue == 1
    assert c.ofTrue == a.ofTrue
    assert d.ofTrue < a.ofTrue
    assert c.ofTrue > d.ofTrue

    assert a.ofFalse == 1
    assert b.ofFalse < 1
    assert c.ofFalse == 1
    assert d.ofFalse == 1


def test_or_right_exception():
    a = heuristics.evaluate_or(
        lambda: 42,
        lambda: 1/0,
        False, '', 0, 0)
    assert a == 42


def test_or_left_exception_with_pure():
    with pytest.raises(Exception):
        heuristics.evaluate_or(
            lambda: 1/0,
            lambda: heuristics.evaluate(42, '==', 42, '', 0, 0),
            True, '', 0, 0)
    # exception was thrown, so neither true or false
    a = heuristics.LAST_EVALUATION
    assert a.ofTrue < 1
    assert a.ofFalse < 1


def test_or_right_exception_gradient():
    with pytest.raises(Exception):
        heuristics.evaluate_or(
            lambda: heuristics.evaluate(0, '==', 42, '', 0, 0),
            lambda: 1/0,
            False, '', 0, 0)
    a = heuristics.LAST_EVALUATION

    with pytest.raises(Exception):
        heuristics.evaluate_or(
            lambda: heuristics.evaluate(40, '==', 42, '', 0, 0),
            lambda: 1/0,
            False, '', 0, 0)
    b = heuristics.LAST_EVALUATION

    # exception was thrown, so neither true nor false
    assert a.ofTrue < 1
    assert a.ofFalse < 1
    assert b.ofTrue < 1
    assert b.ofFalse < 1

    # however, the second is closer to be true
    assert b.ofTrue > a.ofTrue


def test_and_left_exception_with_pure():
    with pytest.raises(Exception):
        heuristics.evaluate_and(
            lambda: 1/0,
            lambda: heuristics.evaluate(42, '==', 42, '', 0, 0),
            True, '', 0, 0)
    # exception was thrown, so neither true or false
    a = heuristics.LAST_EVALUATION
    assert a.ofTrue < 1
    assert a.ofFalse < 1


def test_and_right_exception_gradient_with_pure():
    heuristics.evaluate_and(
        lambda: heuristics.evaluate(0, '==', 42, '', 0, 0),
        lambda: 1/0,
        True, '', 0, 0)
    a = heuristics.LAST_EVALUATION
    heuristics.evaluate_and(
        lambda: heuristics.evaluate(40, '==', 42, '', 0, 0),
        lambda: 1/0,
        True, '', 0, 0)
    b = heuristics.LAST_EVALUATION

    # no exception was thrown, even when evaluating right due to pure
    assert a.ofTrue < 1
    assert a.ofFalse == 1
    assert b.ofTrue < 1
    assert b.ofFalse == 1

    # however, the second is closer to be true
    assert b.ofTrue > a.ofTrue


def test_and_left_true_right_exception():
    with pytest.raises(Exception):
        heuristics.evaluate_and(
            lambda: heuristics.evaluate(42, '==', 42, '', 0, 0),
            lambda: 1/0,
            True, '', 0, 0)
    # exception was thrown, so neither true or false
    a = heuristics.LAST_EVALUATION
    assert a.ofTrue < 1
    assert a.ofFalse < 1
