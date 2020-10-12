class Singleton(object):
    _instances = {}

    def __new__(cls, *args, **kwargs):
        if cls not in cls._instances:
            s = super(Singleton, cls).__new__(cls, *args, **kwargs)
            s.initialize()
            cls._instances[cls] = s
        return cls._instances[cls]

    def initialize(self):
        pass
