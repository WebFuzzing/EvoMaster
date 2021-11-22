from pathlib import Path
from pprint import pprint
from collections import defaultdict
import xml.etree.ElementTree as ET

res = defaultdict(lambda: defaultdict(dict))

for cov_path in Path('generated').rglob('**/cov.xml'):
    path = str(cov_path).split('/')
    app = path[1]
    level = path[2]
    seed = path[3]
    # print(f"Found coverage file for app: {app}, level: {level}, seed: {seed}")

    tree = ET.parse(cov_path)
    data = tree.getroot().attrib
    lines_valid = data['lines-valid']
    lines_covered = data['lines-covered']
    line_rate = data['line-rate']
    # print(f"Coverage: lines-valid={lines_valid}, lines_covered={lines_covered}, line-rate={line_rate}")

    res[app][level][seed] = {'lines_valid': lines_valid, 'lines_covered': lines_covered, 'line_rate': line_rate}

pprint(res)
