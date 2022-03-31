FILE = 'FILE'
LINE = 'LINE'
STATEMENT = 'STATEMENT'
BRANCH = 'BRANCH'
TRUE_BRANCH = '_trueBranch'
FALSE_BRANCH = '_falseBranch'


def file_objective_name(file_id: str) -> str:
    return f"{FILE}_{file_id}"


def get_file_id_from_objective_name(target: str) -> str:
    prefix = f"{FILE}_"
    return target[len(prefix):]


def line_objective_name(file_id: str, line: int) -> str:
    return f"{LINE}_{file_id}_{pad_number(line)}"


def statement_objective_name(file_id: str, line: int, index: int) -> str:
    return f"{STATEMENT}_{file_id}_{pad_number(line)}_{index}"


def branch_objective_name(file_id: str, line: int, branch_id: int, then_branch: bool) -> str:
    branch = TRUE_BRANCH if then_branch else FALSE_BRANCH
    return f"{BRANCH}_at_{file_id}_at_line_{pad_number(line)}_position_{branch_id}{branch}"


def pad_number(value: int) -> str:
    if value < 0:
        raise ValueError("Negative number to pad")
    if value < 10:
        return "0000" + str(value)
    if value < 100:
        return "000" + str(value)
    if value < 1_000:
        return "00" + str(value)
    if value < 10_000:
        return "0" + str(value)
    return value
