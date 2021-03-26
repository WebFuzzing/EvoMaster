"""Module docstring."""


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
    if x in y:
        print("in")
    if x not in y:
        print("notin")
    print('end')

def dummy_print(x):
    print(x)

def dummy_boolean():
    return False

def dummy_empty(x):
    pass
