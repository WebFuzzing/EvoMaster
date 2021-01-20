
# Load the list of countries
with open('country_list.txt') as f:
    COUNTRIES = [x.strip() for x in f.readlines()]

def countries():
    return COUNTRIES

def is_valid(c):
    return c in COUNTRIES
