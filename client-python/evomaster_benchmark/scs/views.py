import math
from re import match


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


def date_parse(dayname: str, monthname: str) -> str:
    result = 0
    # int month = -1
    dayname = dayname.lower()
    monthname = monthname.lower()
    if ("mon" == dayname or
            "tue" == dayname or
            "wed" == dayname or
            "thur" == dayname or
            "fri" == dayname or
            "sat" == dayname or
            "sun" == dayname):
        result = 1
    if ("jan" == monthname):
        result += 1
    if ("feb" == monthname):
        result += 2
    if ("mar" == monthname):
        result += 3
    if ("apr" == monthname):
        result += 4
    if ("may" == monthname):
        result += 5
    if ("jun" == monthname):
        result += 6
    if ("jul" == monthname):
        result += 7
    if ("aug" == monthname):
        result += 8
    if ("sep" == monthname):
        result += 9
    if ("oct" == monthname):
        result += 10
    if ("nov" == monthname):
        result += 11
    if ("dec" == monthname):
        result += 12
    return str(result)


def file_suffix(directory: str, file: str):
    result = 0
    suffix = None
    fileparts = file.split(".")
    lastpart = len(fileparts) - 1
    if (lastpart > 0):
        suffix = fileparts[lastpart]
        if ("text" == directory):
            if ("txt" == suffix):
                result = 1
        if ("acrobat" == directory):
            if ("pdf" == suffix):
                # print("acrobat")
                result = 2
        if ("word" == directory):
            if ("doc" == suffix):
                # print("word")
                result = 3
        if ("bin" == directory):
            if ("exe" == suffix):
                # print("bin")
                result = 4
        if ("lib" == directory):
            if ("dll" == suffix):
                # print("lib")
                result = 5
    return str(result)


def notypevar(i: int, s: str) -> str:
    result = 0
    x = i
    y = x
    if (x + y == 56):
        result = x
    xs = "hello"
    if ((xs + str(y)) == "hello7"):
        result = 1
    if (xs < s):
        result = 2
    x = 5
    if (y > x):
        result = 3
    return str(result)


def ordered4(w: str, x: str, y: str, z: str) -> str:
    result = "unordered"
    if (len(w) >= 5 and len(w) <= 6 and
            len(x) >= 5 and len(x) <= 6 and
            len(y) >= 5 and len(y) <= 6 and
            len(z) >= 5 and len(z) <= 6):
        if (z > y and y > x and x > w):
            result = "increasing"
        elif (w > x and x > y and y > z):
            result = "decreasing"
    return result


def pat(txt: str, pat: str):
    # SEARCH txt FOR FIRST OCCURRENCE OF pat OR REVERSE OF pat
    # IF pat (STRING OF LENGTH AT LEAST 3) OCCURS IN txt, RTN 1
    # IF REVERSE OF pat OCCURS IN txt, RTN 2
    # IF pat AND REVERSE OF pat OCCURS IN txt, RTN 3
    # IF PALINDROME CONSISTING OF pat FOLLOWED BY REVERSE pat OCCURS IN txt, RTN 4
    # IF PALINDROME CONSISTING OF REVERSE pat FOLLOWED pat OCCURS IN txt, RTN 5
    result = 0
    i = 0
    j = 0
    txtlen = len(txt)
    patlen = len(pat)
    possmatch = None

    if (patlen > 2):
        patrev = pat[::-1]
        for i in range(0, txtlen - patlen + 1):
            if (txt[i] == pat[0]):
                possmatch = txt[i:i + patlen]
                if (possmatch == pat):
                    # FOUND pat
                    result = 1
                    # CHECK IF txt CONTAINS REVERSE pat
                    for j in range(i + patlen, txtlen - patlen + 1):
                        if (txt[j] == patrev[0]):
                            possmatch = txt[j:j + patlen]
                            if (possmatch == patrev):
                                if (j == i + patlen):
                                    return str(i)  # 4
                                else:
                                    return str(i)  # 3
            elif (txt[i] == patrev[0]):
                possmatch = txt[i:i + patlen]
                if (possmatch == patrev):
                    # FOUND pat REVERSE
                    result = 2
                    # CHECK IF txt CONTAINS pat
                    for j in range(i + patlen, txtlen - patlen + 1):
                        if (txt[j] == pat[0]):
                            possmatch = txt[j:j + patlen]
                            if (possmatch == pat):
                                if (j == i + patlen):
                                    return str(i)  # 5
                                else:
                                    return str(i)  # 3
    return str(result)


def regex(txt: str) -> str:
    digit = r"((0)|(1)|(2)|(3)|(4)|(5)|(6)|(7)|(8)|(9))"
    fp = digit + digit + r"*." + digit + digit + r"*"
    fpe = fp + r"e((\+)|(-))" + digit + digit

    alpha = r"((a)|(b)|(c)|(d)|(e)|(f)|(g)|(h)|(i)|(j)|(k)|(l)|(m)|(n)|(o)|(p)|(q)|(r)|(s)|(t)|(u)|(v)|(w)|(x)|(y)|(z))"
    iden = alpha + "(" + alpha + "|" + digit + ")*"
    url = r"((http)|(ftp)|(afs)|(gopher))://" + iden + "/" + iden
    day = r"((mon)|(tue)|(wed)|(thur)|(fri)|(sat)|(sun))"
    month = r"((jan)|(feb)|(mar)|(apr)|(may)|(jun)|(jul)|(aug)|(sep)|(oct)|(nov)|(dec))"
    date = day + digit + digit + month

    if match(url, txt):
        return "url"
    if match(date, txt):
        return "date"
    if match(fpe, txt):
        return "fpe"
    return "none"


def text2txt(word1: str, word2: str, word3: str) -> str:
    # CONVERT ENGLISH TEXT txt INTO MOBILE TELEPHONE TXT
    # BY SUBSTITUTING ABBREVIATIONS FOR COMMON WORDS
    word1 = word1.lower()
    word2 = word2.lower()
    word3 = word3.lower()
    result = ""
    if (word1 == "two"):
        result = "2"
    if (word1 == "for" or word1 == "four"):
        result = "4"
    if (word1 == "you"):
        result = "u"
    if (word1 == "and"):
        result = "n"
    if (word1 == "are"):
        result = "r"
    elif (word1 == "see" and word2 == "you"):
        result = "cu"
    elif (word1 == "by" and word2 == "the" and word3 == "way"):
        result = "btw"
    return result


def title(sex: str, t: str) -> str:
    # CHECK PERSONAL TITLE CONSISTENT WITH SEX
    sex = sex.lower()
    t = t.lower()
    result = -1
    if ("male" == sex):
        if ("mr" == t or
                "dr" == t or
                "sir" == t or
                "rev" == t or
                "rthon" == t or
                "prof" == t):
            result = 1
    elif ("female" == sex):
        if ("mrs" == t or
                "miss" == t or
                "ms" == t or
                "dr" == t or
                "lady" == t or
                "rev" == t or
                "rthon" == t or
                "prof" == t):
            result = 0
    elif ("none" == sex):
        if ("dr" == t or
                "rev" == t or
                "rthon" == t or
                "prof" == t):
            result = 2
    return str(result)
