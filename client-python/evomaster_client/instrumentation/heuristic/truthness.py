from numbers import Number

MAX_CHAR_DISTANCE = 65_536


class Truthness:
    def __init__(self, ofTrue: float, ofFalse: float) -> None:
        self.ofTrue = ofTrue
        self.ofFalse = ofFalse

    def invert(self):
        return Truthness(self.ofFalse, self.ofTrue)

    def rescale_from_min(self, min_value: Number):
        return Truthness(1 if self.ofTrue == 1 else min_value + (1 - min_value) * self.ofTrue,
                         1 if self.ofFalse == 1 else min_value + (1 - min_value) * self.ofFalse)

    def __str__(self):
        return f"T(true:{self.ofTrue},false:{self.ofFalse})"


def normalize(value: Number) -> float:
    if value < 0:
        raise ValueError("Negative value: {value}")
    return value / (value + 1)


def eq_truthness_number(a: Number, b: Number) -> Truthness:
    distance = abs(a - b)
    return Truthness(1 - normalize(distance), int(a != b))


def eq_truthness_str(a: str, b: str) -> Truthness:
    distance = abs(len(a) - len(b)) * MAX_CHAR_DISTANCE
    distance += sum(abs(ord(a[i]) - ord(b[i])) for i in range(0, min(len(a), len(b))))
    return Truthness(1 - normalize(distance), int(a != b))


def lt_truthness_number(a: Number, b: Number) -> Truthness:
    distance = abs(a - b)
    return Truthness(1 if a < b else 1 / (1.1 + distance),
                     1 if a >= b else 1 / (1.1 + distance))


def lt_truthness_str(a: str, b: str) -> Truthness:
    distance = MAX_CHAR_DISTANCE
    for i in range(0, min(len(a), len(b))):
        diff = abs(ord(a[i]) - ord(b[i]))
        if diff > 0:
            distance = diff
            break
    return Truthness(1 if a < b else 1 / (1.1 + distance),
                     1 if a >= b else 1 / (1.1 + distance))
