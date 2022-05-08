from pathlib import Path
from pprint import pprint
from collections import defaultdict
import xml.etree.ElementTree as ET
import matplotlib.pyplot as plt
import numpy as np

from texttable import Texttable

import latextable

res = defaultdict(lambda: defaultdict(dict))

for cov_path in Path('generated_results_10min_5seeds').rglob('**/cov.xml'):
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

fig, (plt1, plt2) = plt.subplots(2, 1)

table = Texttable()
table.set_cols_align(["c"] * 5)
table.set_deco(Texttable.HEADER | Texttable.VLINES)
table.add_row(['APP', 'Nivel 0', 'Nivel 1', 'Nivel 2', 'Nivel 3'])

levels = [0, 1, 2, 3]
for app in ('ncs', 'scs', 'news'):
    coverage_by_level = []
    coverage_err_by_level = []
    lines_by_level = []
    for level in levels:
        coverage = list(map(lambda value: float(value['line_rate']), res[app][str(level)].values()))
        data = np.array(coverage)
        coverage_by_level.append(np.average(data))
        coverage_err_by_level.append(np.std(data))

        lines = list(map(lambda value: float(value['lines_covered']), res[app][str(level)].values()))
        data = np.array(lines)
        lines_by_level.append(np.average(data))

        coverage_txt = list(map(lambda value: value['line_rate'], res[app][str(level)].values()))

    table.add_row([app] + list(map(lambda v: str(v), coverage_by_level)))

    plt1.plot(levels, coverage_by_level, label=app)
    plt2.plot(levels, lines_by_level, label=app)

plt1.set_xticks(ticks=levels)
plt2.set_xticks(ticks=levels)

# Naming the x-axis, y-axis and the whole graph
plt1.set_xlabel("Nivel de intrumentación")
plt1.set_ylabel("Cobertura")
plt1.set_title("Ratio de cobertura por nivel de intrumentación")

plt2.set_xlabel("Nivel de intrumentación")
plt2.set_ylabel("Líneas cubiertas")
plt2.set_title("Líneas cubiertas por nivel de intrumentación")

# Adding legend, which helps us recognize the curve according to it's color
plt1.legend()
plt2.legend()

fig.suptitle('Coverage obtenido por aplicación (10 minutos, promedio de 5 semillas)')
fig.tight_layout()

plt1.grid()
plt2.grid()

# Display graph
plt.show()

print('\nTexttable Latex:')
print(latextable.draw_latex(table, caption="Coverage obtenido por aplicación (10 minutos, promedio de 5 semillas)"))
