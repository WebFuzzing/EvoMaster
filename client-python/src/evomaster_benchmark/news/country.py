import os

script_dir = os.path.dirname(__file__)
rel_path = "country_list.txt"
abs_file_path = os.path.join(script_dir, rel_path)


# Load the list of countries
with open(abs_file_path) as f:
    COUNTRIES = [x.strip() for x in f.readlines()]


def countries():
    return COUNTRIES


def is_valid(c):
    return c in COUNTRIES
