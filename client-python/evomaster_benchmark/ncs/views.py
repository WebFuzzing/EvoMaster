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


def expint(n: int, x: float) -> float:
    MAXIT = 100
    EULER = 0.5772156649
    FPMIN = 1.0e-30
    EPS = 1.0e-7
    nm1 = n - 1
    if n < 0 or x < 0 or (x == 0 and (n == 0 or n == 1)):
        raise ValueError("error: n < 0 or x < 0")
    else:
        if n == 0:
            ans = math.exp(-x) / x
        else:
            if x == 0:
                ans = 1.0 / nm1
            else:
                if x > 1:
                    b = x + n
                    c = 1.0 / FPMIN
                    d = 1.0 / b
                    h = d
                    for i in range(1, MAXIT+1):
                        a = -i * (nm1 + 1)
                        b += 2.0
                        d = 1.0 / (a * d + b)
                        c = b + a / c
                        _del = c * d
                        h *= _del
                        if abs(_del - 1) < EPS:
                            return h * math.exp(-x)
                    raise ValueError("continued fraction failed in expint")
                else:
                    ans = (1.0 / nm1 if nm1 != 0 else -math.log(x) - EULER)
                    fact = 1.0
                    for i in range(1, MAXIT+1):
                        fact *= -x / i
                        if i != nm1:
                            _del = -fact / (i - nm1)
                        else:
                            psi = -EULER
                            for j in range(1, nm1+1):
                                psi += 1.0 / j
                            _del = fact * (-math.log(x) + psi)
                        ans += _del
                        if abs(_del) < abs(ans) * EPS:
                            return ans
                    raise ValueError("series failed in expint")
    return ans


def fisher(m: int, n: int, x: float) -> float:
    a = 2 * (m / 2) - m + 2
    b = 2 * (n / 2) - n + 2
    w = (x * m) / n
    z = 1.0 / (1.0 + w)

    if a == 1:
        if b == 1:
            p = math.sqrt(w)
            y = 0.3183098862
            d = y * z / p
            p = 2.0 * y * math.atan(p)
        else:
            p = math.sqrt(w * z)
            d = 0.5 * p * z / w
    elif b == 1:
        p = math.sqrt(z)
        d = 0.5 * z * p
        p = 1.0 - p
    else:
        d = z * z
        p = w * z

    y = 2.0 * w / z

    if a == 1:
        for j in range(b+2, n+1, 2):
            d *= (1.0 + 1.0 / (j - 2)) * z
            p += d * y / (j - 1)
    else:
        zk = math.pow(z, ((n - 1) / 2))
        d *= (zk * n) / b
        p = p * zk + w * z * (zk - 1.0) / (z - 1.0)

    y = w * z
    z = 2.0 / z
    b = n - 2
    for i in range(int(a+2), int(m+1), 2):
        j = i + b
        d *= (y * j) / (i - 2)
        p -= z * d / j

    if p < 0.0:
        return 0.0
    elif p > 1.0:
        return 1.0
    else:
        return p


def remainder(a: int, b: int):
    r = 0 - 1
    cy = 0
    ny = 0
    if (a == 0):
        pass
    elif (b == 0):
        pass
    elif (a > 0):
        if (b > 0):
            while ((a - ny) >= b):
                ny = ny + b
                r = a - ny
                cy = cy + 1
        else:
            # b<0
            # while((a+ny)>=Math.abs(b))
            while ((a + ny) >= abs(b)):
                ny = ny + b
                r = a + ny
                cy = cy - 1
    else:
        # a<0
        if (b > 0):
            # while(Math.abs(a+ny)>=b)
            while (abs(a + ny) >= b):
                ny = ny + b
                r = a + ny
                cy = cy - 1
        else:
            while (b >= (a - ny)):
                ny = ny + b
                # r=Math.abs(a-ny)
                r = abs(a - ny)
                cy = cy + 1
    return r


class GammqImpl:
    ITMAX = 100
    EPS = 3.0e-7
    FPMIN = 1.0e-30

    def __init__(self) -> None:
        self.gamser = None
        self.gammcf = None
        self.gln = None
        pass

    @staticmethod
    def gammln(xx: float) -> float:
        cof = [76.18009172947146, -86.50532032941677, 24.01409824083091,
               -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5]
        y = x = xx
        tmp = x + 5.5
        tmp -= (x + 0.5) * math.log(tmp)
        ser = 1.000000000190015
        for j in range(0, 6):
            y += 1
            ser += cof[j] / y
        return -tmp + math.log(2.5066282746310005 * ser / x)

    def gcf(self, a: float, x: float) -> float:
        self.gln = self.gammln(a)
        b = x + 1.0 - a
        c = 1.0 / self.FPMIN
        d = 1.0 / b
        h = d
        for i in range(1, self.ITMAX+1):
            an = -i * (i - a)
            b += 2.0
            d = an * d + b
            if (math.abs(d) < self.FPMIN):
                d = self.FPMIN
            c = b + an / c
            if (math.abs(c) < self.FPMIN):
                c = self.FPMIN
            d = 1.0 / d
            _del = d * c
            h *= _del
            if (math.abs(_del - 1.0) < self.EPS):
                break
        if (i > self.ITMAX):
            raise ValueError("a too large, ITMAX too small in gcf")
        self.gammcf = math.exp(-x + a * math.log(x) - self.gln) * h

    def gser(self, a: float, x: float) -> float:
        self.gln = self.gammln(a)
        if (x <= 0.0):
            if (x < 0.0):
                raise ValueError("x less than 0 in routine gser")
            self.gamser = 0.0
            return
        else:
            ap = a
            _del = sum = 1.0 / a
            for _ in range(1, self.ITMAX+1):
                ap += 1
                _del *= x / ap
                sum += _del
                if (abs(_del) < abs(sum) * self.EPS):
                    self.gamser = sum * math.exp(-x + a * math.log(x) - self.gln)
                    return
            raise ValueError("a too large, ITMAX too small in routine gser")

    def exe(self, a: float, x: float):
        if x < 0 or a < 0:
            raise ValueError("Invalid arguments in routine gammq")
        if x < (a + 1.0):
            self.gser(a, x)
            return 1 - self.gamser
        else:
            self.gcf(a, x)
            return self.gammcf
