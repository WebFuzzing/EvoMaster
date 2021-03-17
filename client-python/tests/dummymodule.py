"""Module docstring."""


def dummy_compare(x, y):
    print('start')
    if dummy_boolean():
        print('dummy boolean')
    if x == y:
        print("eq")
    elif x != y:
        print("noteq")
    elif x < y:
        print("lt")
    elif x <= y:
        print("lte")
    elif x > y:
        print("gt")
    elif x >= y:
        print("gte")
    elif x is y:
        print("is")
    elif x is not y:
        print("isnot")
    elif x in y:
        print("in")
    elif x not in y:
        print("notin")
    print('end')

def dummy_print(x):
    print(x)

def dummy_boolean():
    return False

def dummy_empty(x):
    pass
