import math

def calc(op, arg1, arg2):
    op = op.lower()
    if op == 'pi':
        return math.pi
    elif op == 'e':
        return math.e
    elif op == 'sqrt':
        return math.sqrt(arg1)
    elif op == 'log':
        return math.log(arg1)
    elif op == 'sine':
        return math.sin(arg1)
    elif op == 'cosine':
        return math.cos(arg1)
    elif op == 'tangent':
        return math.tan(arg1)
    elif op == 'plus':
        return arg1 + arg2
    elif op == 'subtract':
        return arg1 - arg2
    elif op == 'multiply':
        return arg1 * arg2
    elif op == 'divide':
        return arg1 / arg2
    return 0

def cookie(name, val, site):
    name = name.lower()
    val = val.lower()
    site = site.lower()
    res = 0
    if name == 'userid':
        if len(val) > 6:
            if val[0:4] == 'user':
                res = 1
    elif name == 'session':
        if val == 'am' and site == 'abc.com':
            res = 1
        else:
            res = 2
    return res

def costfuns(i, s):
    s1 = 'ba'
    s2 = 'ab'
    res = 0
    if i == 5:
        res = 1
    elif i < -444:
        res = 2
    elif i <= -333:
        res = 3
    elif i > 666:
        res = 4
    elif i >= 555:
        res = 5
    elif i != -4:
        res = 6
    elif s == s1 + s1:
        res = 7
    # elif s.compareTo(s2 + s2 +s1) > 0:
    #     res = 8
    # elif s.compareTo(s2 + s2 + s1) >= 0:
    #     res = 9
    elif s != s2 + s2:
        res = 10
    return res
