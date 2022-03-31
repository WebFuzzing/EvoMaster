import ast
from textwrap import dedent

import astor

from evomaster_client.instrumentation.ast_transformer import AstTransformer


def run_plugin(code: str) -> str:
    ast_tree = ast.parse(code)

    t = AstTransformer('test.py')
    new_tree = t.visit(ast_tree)

    # print("### AST Tree ###")
    # print(astor.dump_tree(new_tree))
    # print("######")

    source = astor.to_source(new_tree)

    # print("### Code parsed from AST ###")
    # print(source)
    # print("######")

    return source


def test_simple():
    code = dedent("""\
    x = 0
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = 0
    completed_statement('test.py', 1, 1)
    """)


def test_expressions():
    code = dedent("""\
    x = 0
    x
    "foo"
    5
    bar.foo()
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = 0
    completed_statement('test.py', 1, 1)
    entering_statement('test.py', 2, 2)
    x
    completed_statement('test.py', 2, 2)
    entering_statement('test.py', 3, 3)
    \"""foo\"""
    completed_statement('test.py', 3, 3)
    entering_statement('test.py', 4, 4)
    5
    completed_statement('test.py', 4, 4)
    entering_statement('test.py', 5, 5)
    bar.foo()
    completed_statement('test.py', 5, 5)
    """)


def test_multi_assignment_same_line():
    code = dedent("""\
    x, y = 0, 1
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x, y = 0, 1
    completed_statement('test.py', 1, 1)
    """)


def test_return_void():
    code = dedent("""\
    def f():
        return
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *


    def f():
        completion_statement('test.py', 2, 1)
        return
    """)


def test_simple_multi_lines():
    code = dedent("""\
    x = 0

    y = 1

    def f(a, b):
        return a + b

    sum = f
    k = sum(x,y)
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = 0
    completed_statement('test.py', 1, 1)
    entering_statement('test.py', 3, 2)
    y = 1
    completed_statement('test.py', 3, 2)


    def f(a, b):
        entering_statement('test.py', 6, 3)
        return completing_statement(a + b, 'test.py', 6, 3)


    entering_statement('test.py', 8, 4)
    sum = f
    completed_statement('test.py', 8, 4)
    entering_statement('test.py', 9, 5)
    k = sum(x, y)
    completed_statement('test.py', 9, 5)
    """)


def test_lt_branch_distance():
    code = dedent("""\
    x = 2 < 5
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = compare_statement(2, '<', 5, 'test.py', 1, 1)
    completed_statement('test.py', 1, 1)
    """)


def test_not_branch_distance():
    code = dedent("""\
    x = not not True
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = not_statement(not_statement(True))
    completed_statement('test.py', 1, 1)
    """)


def test_or_branch_distance():
    code = dedent("""\
    x = True or False
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = or_statement(lambda : True, lambda : False, False, 'test.py', 1, 1)
    completed_statement('test.py', 1, 1)
    """)


def test_and_branch_distance():
    code = dedent("""\
    x = 4 and foo.bar()
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = and_statement(lambda : 4, lambda : foo.bar(), False, 'test.py', 1, 1)
    completed_statement('test.py', 1, 1)
    """)


def test_for_loop():
    code = dedent("""\
    for i in range(1,10): x = i; print(x)
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    for i in range(1, 10):
        entering_statement('test.py', 1, 1)
        x = i
        completed_statement('test.py', 1, 1)
        entering_statement('test.py', 1, 2)
        print(x)
        completed_statement('test.py', 1, 2)
    """)


def test_function_call():
    code = dedent("""\
    foo()
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    foo()
    completed_statement('test.py', 1, 1)
    """)


def test_function_call_chain():
    code = dedent("""\
    a.b.c.foo().bar(x)(y,z)
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    a.b.c.foo().bar(x)(y, z)
    completed_statement('test.py', 1, 1)
    """)


def test_if_else():
    code = dedent("""\
    x = 1
    if x > 0:
        x += 1
        return x
    elif not x:
        return False
    else:
        x -=1
        return
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    x = 1
    completed_statement('test.py', 1, 1)
    if compare_statement(x, '>', 0, 'test.py', 2, 1):
        entering_statement('test.py', 3, 2)
        x += 1
        completed_statement('test.py', 3, 2)
        entering_statement('test.py', 4, 3)
        return completing_statement(x, 'test.py', 4, 3)
    elif not_statement(x):
        entering_statement('test.py', 6, 4)
        return completing_statement(False, 'test.py', 6, 4)
    else:
        entering_statement('test.py', 8, 5)
        x -= 1
        completed_statement('test.py', 8, 5)
        completion_statement('test.py', 9, 6)
        return
    """)


def test_inline_if():
    code = dedent("""\
    print("foo") if 5 < 10 else "foo"
    """)
    res = run_plugin(code)
    assert res == dedent("""\
    from evomaster_client.instrumentation.injected_functions import *
    entering_statement('test.py', 1, 1)
    print('foo') if compare_statement(5, '<', 10, 'test.py', 1, 1) else 'foo'
    completed_statement('test.py', 1, 1)
    """)
