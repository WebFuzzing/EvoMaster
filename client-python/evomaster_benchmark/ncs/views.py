import math


def triangle_classify(a: int, b: int, c: int) -> int:
    if a <= 0 or b <= 0 or c <= 0:
        return 0
    if a == b and b == c:
        return 3
    m = max(a, b, c)
    if (m == a and m - b - c >= 0 or
            m == b and m - a - c >= 0 or
            m == c and m - a - b >= 0):
        return 0
    if a == b or b == c or a == c:
        return 2
    else:
        return 1


def bessj(n: int, x: float) -> float:
    """
    Bessel function J-sub-n(x).
    @see: NRP 6.5

    @param x: float number
    @return: float number

    @status: Tested function
    @since: version 0.1
    https://github.com/mauriceling/dose/blob/master/dose/copads/nrpy.py
    """
    iacc = 40.0
    bigno = 1.0e10
    bigni = 1.0e-10
    if n < 2:
        raise ValueError('n must be more than 1 - use \
            bessj0 or bessj1 for n = 0 or 1 respectively')
    if x == 0.0:
        ans = 0.0
    else:
        if abs(x) > 1.0 * n:
            tox = 2.0/abs(x)
            bjm = bessj0(abs(x))
            bj = bessj1(abs(x))
            for j in range(1, n):
                bjp = j*tox*bj-bjm
                bjm = bj
                bj = bjp
            ans = bj
        else:
            tox = 2.0/abs(x)
            m = int(2*((n+math.floor(math.sqrt(1.0*(iacc*n)))) % 2))
            ans = 0.0
            jsum = 0.0
            sum = 0.0
            bjp = 0.0
            bj = 1.0
            for j in range(m, 1, -1):
                bjm = j*tox*bj-bjp
                bjp = bj
                bj = bjm
                if abs(bj) > bigno:
                    bj = bj*bigni
                    bjp = bjp*bigni
                    ans = ans*bigni
                    sum = sum*bigni
                if jsum != 0:
                    sum = sum + bj
                jsum = 1-jsum
                if j == n:
                    ans = bjp
            sum = 2.0*sum-bj
            print(sum, ans)
            ans = ans/sum
        if x < 0.0 and (n % 2) == 1:
            ans = -ans
        return ans


def bessj0(x):
    """
    Bessel function J-sub-0(x).
    @see: NRP 6.4

    @param x: float number
    @return: float number

    @status: Tested function
    @since: version 0.1
    """
    if abs(x) < 8.0:
        y = x*x
        return (57568490574.0 + y * (-13362590354.0 + y * (651619640.7 +
                                                           y * (-11214424.18 + y * (77392.33017 + y *
                                                                                    (-184.9052456)))))) / \
               (57568490411.0 + y * (1029532985.0 + y * (9494680.718 + y *
                                                         (59272.64853 + y * (267.8532712 + y * 1.0)))))
    else:
        ax = abs(x)
        z = 8.0/ax
        y = z*z
        xx = ax - 0.785398164
        ans1 = 1.0 + y * (-0.1098628627e-2 + y * (0.2734510407e-4 + y *
                                                  (-0.2073370639e-5 + y * 0.2093887211e-6)))
        ans2 = -0.156249995e-1 + y * (0.1430488765e-3 + y *
                                      (-0.6911147651e-5 +
                                       y * (0.7621095161e-6 - y * 0.934945152e-7)))
        return math.sqrt(0.636619772 / ax) * (math.cos(xx) * ans1 - z *
                                              math.sin(xx) * ans2)


def bessj1(x):
    """
    Bessel function J-sub-1(x).
    @see: NRP 6.4

    @param x: float number
    @return: float number

    @status: Tested function
    @since: version 0.1
    """
    if abs(x) < 8.0:
        y = x*x
        ans1 = x * (72362614232.0 + y * (-7895059235.0 + y *
                                         (242396853.1 + y *
                                          (-2972611.439 + y * (15704.4826 + y * (-30.16036606))))))
        ans2 = 144725228442.0 + y * (2300535178.0 + y * (18583304.74 + y *
                                                         (99447.43394 + y * (376.9991397 + y))))
        return ans1 / ans2
    else:
        ax = abs(x)
        z = 8.0 / ax
        y = z * z
        xx = ax - 2.356194491
        ans1 = 1.0 + y * (0.183105e-2 + y * (-0.3516396496e-4 + y *
                                             (0.2457520174e-5 + y * (-0.240337019e-6))))
        ans2 = 0.04687499995 + y * (-0.2002690873e-3 + y *
                                    (0.8449199096e-5 + y *
                                     (-0.88228987e-6 + y * 0.105787412e-6)))
        if x < 0.0:
            return math.sqrt(0.636619772 / ax) * \
                   (math.cos(xx) * ans1 - z *
                    math.sin(xx) * ans2)
        else:
            return -1 * math.sqrt(0.636619772 / ax) * \
                   (math.cos(xx) * ans1 -
                    z * math.sin(xx) * ans2)
