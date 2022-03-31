"""Dummy module used to test the import hook instrumentation."""


def dummy_compare(x, y):
    print('start')
    if dummy_boolean():
        print('dummy boolean')
    if not x:
        print("not")
    if x == y:
        print("eq")
    if x != y:
        print("noteq")
    if x < y:
        print("lt")
    if x <= y:
        print("lte")
    if x > y:
        print("gt")
    if x >= y:
        print("gte")
    if x is y:
        print("is")
    if x is not y:
        print("isnot")
    if x > 5 and y < 5:
        print("and")
    if x > 5 or y < 5:
        print("or")
    print('end')

def dummy_print(x):
    print(x)

def dummy_boolean():
    return False

def dummy_empty(x):
    pass

def dummy_truthness(x):
    if x > 10 and not(x < 12):
        return True
    return False

def dummy_call(x):
    return dummy_boolean()
